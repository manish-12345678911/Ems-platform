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

    @PostMapping("/crew/login")
    public Mono<ResponseEntity<AuthResponse>> crewLogin(@RequestBody AuthRequest request) {
        AdminAuthService.AuthResult result = adminAuthService.authenticateCrew(request);
        return Mono.just(ResponseEntity.status(result.statusCode()).body(result.response()));
    }

    @PostMapping("/crew/register")
    public Mono<ResponseEntity<AuthResponse>> crewRegister(@RequestBody Map<String, String> body) {
        String callSign = body != null ? body.getOrDefault("callSign", "") : "";
        String type = body != null ? body.getOrDefault("type", "ALS") : "ALS";
        String password = body != null ? body.getOrDefault("password", "") : "";
        AdminAuthService.AuthResult result = adminAuthService.registerCrew(callSign, type, password);
        return Mono.just(ResponseEntity.status(result.statusCode()).body(result.response()));
    }

    @PostMapping("/crew/logout")
    public Mono<ResponseEntity<Map<String, Object>>> crewLogout() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Crew taken off-duty successfully."
        )));
    }
}
