package com.h8.ems.gateway.auth;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private TokenService tokenService;
    private final String secret = "test-secret-key-must-be-at-least-256-bits-long-1234567890-abcdef";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(secret, 3600);
    }

    @Test
    @DisplayName("Generates signed JWT and parses/verifies successfully")
    void testGenerateAndVerify() throws Exception {
        String token = tokenService.generateToken("admin", "Admin User", List.of("ADMIN", "DISPATCHER"));
        assertThat(token).isNotBlank();

        SignedJWT jwt = tokenService.parseAndVerify(token);
        assertThat(jwt).isNotNull();
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("admin");
        assertThat(jwt.getJWTClaimsSet().getClaim("name")).isEqualTo("Admin User");
        assertThat(tokenService.hasAdminPrivilege(jwt)).isTrue();
    }

    @Test
    @DisplayName("Verifies token with Bearer prefix")
    void testVerifyWithBearerPrefix() throws Exception {
        String token = tokenService.generateToken("dispatcher1", "Senior Dispatcher", List.of("DISPATCHER"));
        SignedJWT jwt = tokenService.parseAndVerify("Bearer " + token);
        assertThat(jwt).isNotNull();
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("dispatcher1");
        assertThat(tokenService.hasAdminPrivilege(jwt)).isTrue();
    }

    @Test
    @DisplayName("Returns null for tampered token")
    void testTamperedToken() {
        String token = tokenService.generateToken("admin", "Admin", List.of("ADMIN"));
        String tampered = token.substring(0, token.length() - 5) + "abcde";
        SignedJWT jwt = tokenService.parseAndVerify(tampered);
        assertThat(jwt).isNull();
    }

    @Test
    @DisplayName("Non-admin role does not have admin privilege")
    void testNonAdminPrivilege() {
        String token = tokenService.generateToken("crew1", "Paramedic", List.of("CREW"));
        SignedJWT jwt = tokenService.parseAndVerify(token);
        assertThat(jwt).isNotNull();
        assertThat(tokenService.hasAdminPrivilege(jwt)).isFalse();
    }
}
