package com.example.analytics;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.*;
import java.security.Principal;
import java.util.Map;
@RestController @RequestMapping("/api")
class AnalyticsController {
 private final AnalyticsService service;
 AnalyticsController(AnalyticsService service){this.service=service;}
 @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @PostMapping("/queries") QueryResponse start(@RequestBody QueryRequest request,Principal user) throws Exception {return service.start(user.getName(),request);}
 @GetMapping("/queries/{id}") QueryResponse poll(@PathVariable String id,Principal user) throws Exception {return service.poll(user.getName(),id);}
 @DeleteMapping("/queries/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void cancel(@PathVariable String id,Principal user) throws Exception {service.cancel(user.getName(),id);}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
}
