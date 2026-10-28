package com.example.analytics;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;
@Component @Profile("demo")
class DemoEngine implements QueryEngine {
 record Task(QueryRequest request,long readyAt) {}
 private final Map<String,Task> tasks=new ConcurrentHashMap<>();
 public String start(QueryRequest r) { String id=UUID.randomUUID().toString(); tasks.put(id,new Task(r,System.currentTimeMillis()+1500)); return id; }
 public QueryResult poll(String id) {
  Task t=tasks.get(id); if(t==null) return new QueryResult("CANCELLED",List.of());
  if(System.currentTimeMillis()<t.readyAt) return new QueryResult("RUNNING",List.of());
  var rows=t.request.start().datesUntil(t.request.end().plusDays(1)).map(d->{
   long count=10000+Math.floorMod(d.toEpochDay()*7919,40000);
   return new DailyTotal(d.toString(),count,BigDecimal.valueOf(count*1245,2));
  }).toList(); tasks.remove(id); return new QueryResult("SUCCEEDED",rows);
 }
 public void cancel(String id) {tasks.remove(id);}
}
