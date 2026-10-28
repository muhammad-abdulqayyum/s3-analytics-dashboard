package com.example.analytics;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;
import software.amazon.awssdk.regions.Region;
import java.util.*;
import java.math.BigDecimal;
@Component @Profile("aws")
class AthenaEngine implements QueryEngine,AutoCloseable {
 private final AthenaClient client;
 private final String database,workgroup,output;
 AthenaEngine(@Value("${app.region}") String region,@Value("${app.database}") String database,
  @Value("${app.workgroup}") String workgroup,@Value("${app.output}") String output) {
  client=AthenaClient.builder().region(Region.of(region)).build();this.database=database;this.workgroup=workgroup;this.output=output;
 }
 public String start(QueryRequest r) {
  r.validate();
  String sql="""
   SELECT event_date, COUNT(*) AS transaction_count,
          COALESCE(SUM(amount),0) AS total_amount
   FROM transactions
   WHERE event_date BETWEEN '%s' AND '%s'
   GROUP BY event_date ORDER BY event_date
   """.formatted(r.start(),r.end());
  return client.startQueryExecution(StartQueryExecutionRequest.builder().queryString(sql)
   .queryExecutionContext(QueryExecutionContext.builder().database(database).build()).workGroup(workgroup)
   .resultConfiguration(ResultConfiguration.builder().outputLocation(output).build()).build()).queryExecutionId();
 }
 public QueryResult poll(String id) {
  var state=client.getQueryExecution(GetQueryExecutionRequest.builder().queryExecutionId(id).build()).queryExecution().status().state();
  if(state==QueryExecutionState.FAILED || state==QueryExecutionState.CANCELLED) return new QueryResult(state.toString(),List.of());
  if(state!=QueryExecutionState.SUCCEEDED) return new QueryResult("RUNNING",List.of());
  var response=client.getQueryResults(GetQueryResultsRequest.builder().queryExecutionId(id).maxResults(1000).build());
  // At most 31 daily groups plus a header: one result page is sufficient.
  var rows=response.resultSet().rows().stream().skip(1).map(row->new DailyTotal(
   row.data().get(0).varCharValue(),Long.parseLong(row.data().get(1).varCharValue()),
   new BigDecimal(row.data().get(2).varCharValue()))).toList();
  return new QueryResult("SUCCEEDED",rows);
 }
 public void cancel(String id) {client.stopQueryExecution(StopQueryExecutionRequest.builder().queryExecutionId(id).build());}
 public void close(){client.close();}
}
