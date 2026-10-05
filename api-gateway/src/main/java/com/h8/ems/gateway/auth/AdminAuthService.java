package com.h8.ems.gateway.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    private final TokenService tokenService;
    private final Map<String, UserRecord> userStore = new ConcurrentHashMap<>();

    public record UserRecord(
            String username,
            String password,
            String displayName,
            List<String> roles,
            String primaryRole
    ) {}

    public AdminAuthService(
            TokenService tokenService,
            @Value("${h8.admin.username:admin}") String adminUsername,
            @Value("${h8.admin.password:admin123}") String adminPassword) {
        this.tokenService = tokenService;

        // Register default accounts
        userStore.put(adminUsername.toLowerCase(), new UserRecord(
                adminUsername,
                adminPassword,
                "Tactical Chief Administrator",
                List.of("ADMIN", "DISPATCHER", "SUPERVISOR"),
                "ADMIN"
        ));

        userStore.put("dispatcher1", new UserRecord(
                "dispatcher1",
                "test123",
                "Senior Dispatch Controller",
                List.of("DISPATCHER", "ADMIN"),
                "DISPATCHER"
        ));

        userStore.put("supervisor1", new UserRecord(
                "supervisor1",
                "test123",
                "Tactical Operations Supervisor",
                List.of("SUPERVISOR", "ADMIN"),
                "SUPERVISOR"
        ));

        // Restricted / Non-admin accounts for role enforcement verification
        userStore.put("crew1", new UserRecord(
                "crew1",
                "test123",
                "Paramedic Unit Crew",
                List.of("CREW"),
                "CREW"
        ));

        userStore.put("ednurse1", new UserRecord(
                "ednurse1",
                "test123",
                "ED Triage Charge Nurse",
                List.of("ED_STAFF"),
                "ED_STAFF"
        ));

        userStore.put("auditor1", new UserRecord(
                "auditor1",
                "test123",
                "Clinical Quality Auditor",
                List.of("AUDITOR"),
                "AUDITOR"
        ));
    }

    public AuthResult authenticate(AuthRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return AuthResult.unauthorized("Username and password are required.");
        }

        String usernameKey = request.username().trim().toLowerCase();
        UserRecord user = userStore.get(usernameKey);

        if (user == null || !user.password().equals(request.password())) {
            log.warn("Failed login attempt for username: {}", request.username());
            return AuthResult.unauthorized("Invalid username or password.");
        }

        // Enforce Admin / Dispatcher clearance
        boolean hasAdminClearance = user.roles().stream().anyMatch(role ->
                "ADMIN".equalsIgnoreCase(role) ||
                "DISPATCHER".equalsIgnoreCase(role) ||
                "SUPERVISOR".equalsIgnoreCase(role)
        );

        if (!hasAdminClearance) {
            log.warn("Access denied for user {}: lacking admin clearance (roles: {})", user.username(), user.roles());
            return AuthResult.forbidden(
                    "Access Denied: Account '" + user.username() + "' has role '" + user.primaryRole() +
                    "'. The Dispatcher Command Center requires Administrator or Tactical Dispatch clearance."
            );
        }

        String token = tokenService.generateToken(user.username(), user.displayName(), user.roles());
        log.info("Admin authentication successful for: {} [{}]", user.username(), user.primaryRole());

        AuthResponse response = AuthResponse.success(
                token,
                user.username(),
                user.displayName(),
                user.roles(),
                user.primaryRole(),
                tokenService.getExpirationSeconds()
        );
        return AuthResult.ok(response);
    }

    public AuthResult verifyToken(String authHeader) {
        var jwt = tokenService.parseAndVerify(authHeader);
        if (jwt == null) {
            return AuthResult.unauthorized("Invalid, missing or expired authorization token.");
        }

        if (!tokenService.hasAdminPrivilege(jwt)) {
            return AuthResult.forbidden("Access Denied: Token does not possess administrator clearance.");
        }

        try {
            String subject = jwt.getJWTClaimsSet().getSubject();
            String name = (String) jwt.getJWTClaimsSet().getClaim("name");
            List<String> roles = jwt.getJWTClaimsSet().getStringListClaim("roles");
            String primaryRole = roles != null && !roles.isEmpty() ? roles.getFirst() : "ADMIN";

            AuthResponse response = AuthResponse.success(
                    authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader,
                    subject,
                    name != null ? name : subject,
                    roles != null ? roles : List.of("ADMIN"),
                    primaryRole,
                    tokenService.getExpirationSeconds()
            );
            return AuthResult.ok(response);
        } catch (Exception e) {
            return AuthResult.unauthorized("Failed to extract claims from token.");
        }
    }

    public record AuthResult(
            boolean isSuccess,
            int statusCode,
            AuthResponse response
    ) {
        public static AuthResult ok(AuthResponse response) {
            return new AuthResult(true, 200, response);
        }

        public static AuthResult unauthorized(String message) {
            return new AuthResult(false, 401, AuthResponse.failure(message));
        }

        public static AuthResult forbidden(String message) {
            return new AuthResult(false, 403, AuthResponse.failure(message));
        }
    }
}
