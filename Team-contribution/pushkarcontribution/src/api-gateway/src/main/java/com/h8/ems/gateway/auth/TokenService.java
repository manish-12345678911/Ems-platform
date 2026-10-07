package com.h8.ems.gateway.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class TokenService {

    private final byte[] secretKeyBytes;
    private final long expirationSeconds;

    public TokenService(
            @Value("${h8.security.jwt-secret:h8-tactical-ems-super-secret-key-for-admin-authentication-must-be-256-bits-long!}") String secret,
            @Value("${h8.security.token-expiration-seconds:86400}") long expirationSeconds) {
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String username, String displayName, List<String> roles) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + (expirationSeconds * 1000L));

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issuer("https://h8-ems.local/auth")
                    .issueTime(now)
                    .expirationTime(expiry)
                    .claim("name", displayName)
                    .claim("roles", roles)
                    .claim("realm_access", Map.of("roles", roles))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(new MACSigner(secretKeyBytes));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT token", e);
        }
    }

    public SignedJWT parseAndVerify(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }
            if (token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }
            SignedJWT signedJWT = SignedJWT.parse(token);
            MACVerifier verifier = new MACVerifier(secretKeyBytes);
            if (!signedJWT.verify(verifier)) {
                return null;
            }
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (exp != null && exp.before(new Date())) {
                return null;
            }
            return signedJWT;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasAdminPrivilege(SignedJWT jwt) {
        if (jwt == null) {
            return false;
        }
        try {
            List<String> roles = jwt.getJWTClaimsSet().getStringListClaim("roles");
            if (roles != null && checkRolesForAdmin(roles)) {
                return true;
            }
            Object realmAccess = jwt.getJWTClaimsSet().getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> map) {
                Object rList = map.get("roles");
                if (rList instanceof List<?> list) {
                    List<String> stringList = list.stream().map(String::valueOf).toList();
                    return checkRolesForAdmin(stringList);
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRolesForAdmin(List<String> roles) {
        return roles.stream().anyMatch(r ->
                "ADMIN".equalsIgnoreCase(r) ||
                "ROLE_ADMIN".equalsIgnoreCase(r) ||
                "DISPATCHER".equalsIgnoreCase(r) ||
                "ROLE_DISPATCHER".equalsIgnoreCase(r) ||
                "SUPERVISOR".equalsIgnoreCase(r) ||
                "ROLE_SUPERVISOR".equalsIgnoreCase(r)
        );
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
