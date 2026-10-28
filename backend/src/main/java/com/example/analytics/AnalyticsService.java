package com.example.analytics;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.Duration;
@Service
class AnalyticsService {
 private final QueryEngine engine;private final StringRedisTemplate redis;private final ObjectMapper json;
 AnalyticsService(QueryEngine engine,StringRedisTemplate redis,ObjectMapper json){this.engine=engine;this.redis=redis;this.json=json;}
 private String owner(String user){return Base64.getUrlEncoder().withoutPadding().encodeToString(user.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 private String jobKey(String user,String id){return "job:"+owner(user)+":"+id;}
 QueryResponse start(String user,QueryRequest request) throws Exception {
  request.validate();
  String cache="summary:v1:"+owner(user)+":"+request.start()+":"+request.end();
  String hit=redis.opsForValue().get(cache);
  if(hit!=null) return new QueryResponse(null,"SUCCEEDED",true,json.readValue(hit,QueryResult.class).rows());
  String id=engine.start(request);
  try {redis.opsForValue().set(jobKey(user,id),cache,Duration.ofHours(24));}
  catch(RuntimeException e){engine.cancel(id);throw e;}
  return new QueryResponse(id,"RUNNING",false,List.of());
 }
 private String owned(String user,String id){
  String key=redis.opsForValue().get(jobKey(user,id));
  if(key==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Query not found or expired");return key;
 }
 QueryResponse poll(String user,String id) throws Exception {
  String key=owned(user,id);
  String done=redis.opsForValue().get(jobKey(user,id)+":result");
  QueryResult result=done==null?engine.poll(id):json.readValue(done,QueryResult.class);
  if(!result.status().equals("RUNNING") && done==null){
   String encoded=json.writeValueAsString(result);
   redis.opsForValue().set(jobKey(user,id)+":result",encoded,Duration.ofHours(24));
   if(result.status().equals("SUCCEEDED")) redis.opsForValue().set(key,encoded,Duration.ofMinutes(5));
  }
  return new QueryResponse(id,result.status(),false,result.rows());
 }
 void cancel(String user,String id) throws Exception {
  owned(user,id);engine.cancel(id);
  redis.opsForValue().set(jobKey(user,id)+":result",json.writeValueAsString(new QueryResult("CANCELLED",List.of())),Duration.ofHours(24));
 }
}
