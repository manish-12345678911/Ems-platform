package com.h8.ems.gateway.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AdminAuthService adminAuthService;

    public AuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        AdminAuthService.AuthResult result = adminAuthService.authenticate(request);
        return Mono.just(ResponseEntity.status(result.statusCode()).body(result.response()));
    }

    @GetMapping("/verify")
    public Mono<ResponseEntity<AuthResponse>> verify(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        AdminAuthService.AuthResult result = adminAuthService.verifyToken(authHeader);
        return Mono.just(ResponseEntity.status(result.statusCode()).body(result.response()));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, Object>>> logout() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Admin signed out successfully."
        )));
    }
}
