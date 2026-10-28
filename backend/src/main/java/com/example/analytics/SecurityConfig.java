package com.example.analytics;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import static org.springframework.security.config.Customizer.withDefaults;
@Configuration
class SecurityConfig {
 @Bean UserDetailsService users(@Value("${app.username}") String name,@Value("${app.password}") String password) {
  return new InMemoryUserDetailsManager(User.withUsername(name).password("{noop}"+password).roles("ANALYST").build());
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a->a.anyRequest().authenticated()).httpBasic(withDefaults())
   .csrf(c->c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())).build();
 }
}
