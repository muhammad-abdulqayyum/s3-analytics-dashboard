package com.example.analytics;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AnalyticsServiceTest {
 QueryEngine engine;StringRedisTemplate redis;ValueOperations<String,String> values;AnalyticsService service;
 @BeforeEach @SuppressWarnings("unchecked") void setup(){engine=mock(QueryEngine.class);redis=mock(StringRedisTemplate.class);values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);service=new AnalyticsService(engine,redis,new ObjectMapper());}
 QueryRequest request(){return new QueryRequest(LocalDate.parse("2026-10-01"),LocalDate.parse("2026-10-07"));}
 @Test void rejectsInvalidRangeBeforeCallingDependencies(){assertThrows(IllegalArgumentException.class,()->service.start("alice",new QueryRequest(LocalDate.parse("2026-10-07"),LocalDate.parse("2026-10-01"))));verifyNoInteractions(engine,redis);}
 @Test void cacheHitSkipsAthena() throws Exception {when(values.get(startsWith("summary:"))).thenReturn("{\"status\":\"SUCCEEDED\",\"rows\":[]}");assertTrue(service.start("alice",request()).cached());verifyNoInteractions(engine);}
 @Test void cannotPollAnotherUsersJob(){assertThrows(ResponseStatusException.class,()->service.poll("bob","secret-query"));verifyNoInteractions(engine);}
 @Test void completedQueryWritesSummaryWithFiveMinuteTtl() throws Exception {
  when(values.get("job:YWxpY2U:q1")).thenReturn("summary-key");
  when(engine.poll("q1")).thenReturn(new QueryResult("SUCCEEDED",List.of(new DailyTotal("2026-10-01",10,new BigDecimal("20.00")))));
  assertEquals("SUCCEEDED",service.poll("alice","q1").status());
  verify(values).set(eq("summary-key"),anyString(),eq(Duration.ofMinutes(5)));
 }
 @Test void failedOwnershipWriteCancelsStartedQuery(){when(engine.start(any())).thenReturn("q1");doThrow(new RuntimeException("Redis down")).when(values).set(startsWith("job:"),anyString(),any(Duration.class));assertThrows(RuntimeException.class,()->service.start("alice",request()));verify(engine).cancel("q1");}
}
