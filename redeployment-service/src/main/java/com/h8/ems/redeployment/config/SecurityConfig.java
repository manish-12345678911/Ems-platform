package com.h8.ems.redeployment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/redeployment", "/redeployment/**", "/redeploy", "/redeploy/**", "/actuator/**", "/error").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
