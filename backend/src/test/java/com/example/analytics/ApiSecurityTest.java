package com.example.analytics;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc
class ApiSecurityTest {
 @Autowired MockMvc mvc;
 @MockitoBean StringRedisTemplate redis;
 @BeforeEach @SuppressWarnings("unchecked") void setup(){when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));}
 @Test void unauthenticatedRequestIsRejected() throws Exception {mvc.perform(get("/api/queries/example")).andExpect(status().isUnauthorized());}
 @Test void postWithoutCsrfIsRejected() throws Exception {
  mvc.perform(post("/api/queries").with(httpBasic("demo","demo-change-me")).contentType("application/json").content("{\"start\":\"2026-10-01\",\"end\":\"2026-10-07\"}")).andExpect(status().isForbidden());
 }
 @Test void authenticatedPostStartsDemoJob() throws Exception {
  mvc.perform(post("/api/queries").with(httpBasic("demo","demo-change-me")).with(csrf()).contentType("application/json").content("{\"start\":\"2026-10-01\",\"end\":\"2026-10-07\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RUNNING")).andExpect(jsonPath("$.jobId").isNotEmpty());
 }
 @Test void csrfEndpointReturnsToken() throws Exception {mvc.perform(get("/api/csrf").with(httpBasic("demo","demo-change-me"))).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());}
}
