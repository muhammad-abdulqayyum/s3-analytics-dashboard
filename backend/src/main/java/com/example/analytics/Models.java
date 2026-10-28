package com.example.analytics;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
record QueryRequest(LocalDate start, LocalDate end) {
 void validate() {
  if(start==null || end==null || end.isBefore(start) || java.time.temporal.ChronoUnit.DAYS.between(start,end)>30)
   throw new IllegalArgumentException("Choose an inclusive date range of 1–31 days");
 }
}
record DailyTotal(String date, long transactions, BigDecimal amount) {}
record QueryResult(String status, List<DailyTotal> rows) {}
record QueryResponse(String jobId,String status,boolean cached,List<DailyTotal> rows) {}
interface QueryEngine {
 String start(QueryRequest request);
 QueryResult poll(String id);
 void cancel(String id);
}
