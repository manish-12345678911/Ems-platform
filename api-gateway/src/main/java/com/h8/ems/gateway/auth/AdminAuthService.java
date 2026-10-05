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

        // 14 Default Registered Ambulance Units (passcode: crew123)
        for (int i = 1; i <= 14; i++) {
            String callSign = String.format("AMB-%02d", i);
            String un1 = String.format("amb-%02d", i);
            String un2 = String.format("amb%02d", i);
            String un3 = callSign.toLowerCase();
            String type = (i == 2 || i == 4 || i == 6 || i == 8 || i == 10 || i == 12) ? "BLS" : "ALS";
            UserRecord record = new UserRecord(callSign, "crew123", callSign + " (" + type + ") Paramedic Crew", List.of("CREW"), "CREW");
            userStore.put(un1, record);
            userStore.put(un2, record);
            userStore.put(un3, record);
            userStore.put(callSign, record);
        }
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

    public AuthResult authenticateCrew(AuthRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return AuthResult.unauthorized("Call sign and passcode are required.");
        }

        String usernameKey = request.username().trim().toLowerCase().replace("-", "");
        UserRecord user = userStore.get(usernameKey);
        if (user == null) {
            user = userStore.get(request.username().trim().toLowerCase());
        }

        if (user == null || !user.password().equals(request.password())) {
            log.warn("Failed crew login attempt for call sign: {}", request.username());
            return AuthResult.unauthorized("Invalid ambulance call sign or passcode.");
        }

        String token = tokenService.generateToken(user.username(), user.displayName(), user.roles());
        log.info("Crew authentication successful for: {}", user.username());

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

    public AuthResult registerCrew(String callSign, String type, String password) {
        if (callSign == null || callSign.isBlank() || password == null || password.isBlank()) {
            return AuthResult.unauthorized("Call sign and passcode are required.");
        }
        String clean = callSign.trim().toUpperCase();
        if (userStore.containsKey(clean.toLowerCase()) || userStore.containsKey(clean.toLowerCase().replace("-", ""))) {
            return AuthResult.forbidden("Ambulance unit " + clean + " is already registered.");
        }
        UserRecord record = new UserRecord(clean, password, clean + " (" + type + ") Crew", List.of("CREW"), "CREW");
        userStore.put(clean, record);
        userStore.put(clean.toLowerCase(), record);
        userStore.put(clean.toLowerCase().replace("-", ""), record);

        String token = tokenService.generateToken(clean, record.displayName(), record.roles());
        AuthResponse response = AuthResponse.success(
                token,
                clean,
                record.displayName(),
                record.roles(),
                record.primaryRole(),
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
