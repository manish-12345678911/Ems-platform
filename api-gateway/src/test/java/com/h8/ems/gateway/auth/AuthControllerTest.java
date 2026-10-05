package com.h8.ems.gateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {

    private AuthController authController;
    private AdminAuthService adminAuthService;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService("test-secret-key-must-be-at-least-256-bits-long-1234567890-abcdef", 3600);
        adminAuthService = new AdminAuthService(tokenService, "admin", "admin123");
        authController = new AuthController(adminAuthService);
    }

    @Test
    @DisplayName("POST /auth/login returns 200 OK with JWT for admin")
    void testLoginAdmin() {
        ResponseEntity<AuthResponse> response = authController.login(new AuthRequest("admin", "admin123")).block();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("POST /auth/login returns 401 for wrong credentials")
    void testLoginBadPassword() {
        ResponseEntity<AuthResponse> response = authController.login(new AuthRequest("admin", "wrong")).block();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
    }

    @Test
    @DisplayName("GET /auth/verify returns 200 OK for valid bearer token")
    void testVerifyValidToken() {
        AdminAuthService.AuthResult loginResult = adminAuthService.authenticate(new AuthRequest("admin", "admin123"));
        String token = loginResult.response().token();

        ResponseEntity<AuthResponse> response = authController.verify("Bearer " + token).block();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("POST /auth/logout returns 200 OK")
    void testLogout() {
        ResponseEntity<Map<String, Object>> response = authController.logout().block();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("POST /auth/crew/login returns 200 OK for default registered ambulance AMB-01")
    void testCrewLoginDefaultUnit() {
        ResponseEntity<AuthResponse> response = authController.crewLogin(new AuthRequest("AMB-01", "crew123")).block();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().username()).isEqualTo("AMB-01");
    }

    @Test
    @DisplayName("POST /auth/crew/login returns 401 for wrong crew passcode")
    void testCrewLoginBadPassword() {
        ResponseEntity<AuthResponse> response = authController.crewLogin(new AuthRequest("AMB-01", "wrong")).block();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
    }

    @Test
    @DisplayName("POST /auth/crew/register successfully registers new ambulance unit")
    void testCrewRegister() {
        Map<String, String> payload = Map.of(
                "callSign", "AMB-15",
                "type", "ALS",
                "password", "secret123"
        );
        ResponseEntity<AuthResponse> response = authController.crewRegister(payload).block();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().username()).isEqualTo("AMB-15");
    }

    @Test
    @DisplayName("POST /auth/crew/logout returns 200 OK")
    void testCrewLogout() {
        ResponseEntity<Map<String, Object>> response = authController.crewLogout().block();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }
}
