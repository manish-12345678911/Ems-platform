package com.h8.ems.gateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthServiceTest {

    private AdminAuthService adminAuthService;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService("test-secret-key-must-be-at-least-256-bits-long-1234567890-abcdef", 3600);
        adminAuthService = new AdminAuthService(tokenService, "admin", "admin123");
    }

    @Test
    @DisplayName("Admin login succeeds with valid credentials")
    void testAdminLoginSuccess() {
        AdminAuthService.AuthResult result = adminAuthService.authenticate(new AuthRequest("admin", "admin123"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.response().token()).isNotBlank();
        assertThat(result.response().username()).isEqualTo("admin");
        assertThat(result.response().roles()).contains("ADMIN");
    }

    @Test
    @DisplayName("Dispatcher1 login succeeds as authorized tactical dispatcher")
    void testDispatcherLoginSuccess() {
        AdminAuthService.AuthResult result = adminAuthService.authenticate(new AuthRequest("dispatcher1", "test123"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.response().roles()).contains("DISPATCHER");
    }

    @Test
    @DisplayName("Wrong password returns 401 Unauthorized")
    void testWrongPassword() {
        AdminAuthService.AuthResult result = adminAuthService.authenticate(new AuthRequest("admin", "wrongpass"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.statusCode()).isEqualTo(401);
        assertThat(result.response().token()).isNull();
    }

    @Test
    @DisplayName("Crew1 without admin clearance is rejected with 403 Forbidden")
    void testNonAdminRoleForbidden() {
        AdminAuthService.AuthResult result = adminAuthService.authenticate(new AuthRequest("crew1", "test123"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.statusCode()).isEqualTo(403);
        assertThat(result.response().message()).contains("Access Denied");
    }

    @Test
    @DisplayName("Unknown user returns 401 Unauthorized")
    void testUnknownUser() {
        AdminAuthService.AuthResult result = adminAuthService.authenticate(new AuthRequest("unknown_user", "test123"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Verifying valid admin token succeeds")
    void testVerifyAdminToken() {
        AdminAuthService.AuthResult loginResult = adminAuthService.authenticate(new AuthRequest("admin", "admin123"));
        String token = loginResult.response().token();

        AdminAuthService.AuthResult verifyResult = adminAuthService.verifyToken("Bearer " + token);
        assertThat(verifyResult.isSuccess()).isTrue();
        assertThat(verifyResult.statusCode()).isEqualTo(200);
        assertThat(verifyResult.response().username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Verifying invalid token returns 401 Unauthorized")
    void testVerifyInvalidToken() {
        AdminAuthService.AuthResult verifyResult = adminAuthService.verifyToken("Bearer invalid.token.value");
        assertThat(verifyResult.isSuccess()).isFalse();
        assertThat(verifyResult.statusCode()).isEqualTo(401);
    }
}
