package com.examforge.auth;

import com.examforge.AbstractIntegrationTest;
import com.examforge.role.domain.Role;
import com.examforge.role.domain.RoleName;
import com.examforge.role.repository.RoleRepository;
import com.examforge.user.domain.User;
import com.examforge.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private HttpEntity<String> jsonBody(Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
    }

    @Test
    void fullAuthLifecycle_registerLoginRefreshMeLogout() throws Exception {
        String email = "priya.sharma+" + System.nanoTime() + "@example.com";

        // 1. Register
        Map<String, String> registerPayload = Map.of(
                "name", "Priya Sharma",
                "email", email,
                "mobile", "9876543210",
                "password", "SecurePass123",
                "confirmPassword", "SecurePass123"
        );
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(registerPayload), String.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode registerBody = objectMapper.readTree(registerResponse.getBody());
        assertThat(registerBody.get("success").asBoolean()).isTrue();
        assertThat(registerBody.get("data").get("email").asText()).isEqualTo(email);
        assertThat(registerBody.get("data").get("roles")).anyMatch(r -> r.asText().equals("USER"));

        // 2. Duplicate registration is rejected
        ResponseEntity<String> dupeResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(registerPayload), String.class);
        assertThat(dupeResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 3. Login
        Map<String, String> loginPayload = Map.of("email", email, "password", "SecurePass123");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(loginPayload), String.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode loginBody = objectMapper.readTree(loginResponse.getBody()).get("data");
        String accessToken = loginBody.get("accessToken").asText();
        String refreshToken = loginBody.get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 4. Wrong password is rejected
        ResponseEntity<String> badLogin = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "WrongPass1")), String.class);
        assertThat(badLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 5. /me without token is rejected
        ResponseEntity<String> meUnauth = restTemplate.getForEntity("/api/v1/auth/me", String.class);
        assertThat(meUnauth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 6. /me with valid access token succeeds
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);
        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(meResponse.getBody()).get("data").get("email").asText()).isEqualTo(email);

        // 7. Refresh rotates the token - new tokens are issued
        ResponseEntity<String> refreshResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh", jsonBody(Map.of("refreshToken", refreshToken)), String.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode refreshedBody = objectMapper.readTree(refreshResponse.getBody()).get("data");
        String newRefreshToken = refreshedBody.get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // 8. The old refresh token can no longer be used (rotation/single-use)
        ResponseEntity<String> reuseOldRefresh = restTemplate.postForEntity(
                "/api/v1/auth/refresh", jsonBody(Map.of("refreshToken", refreshToken)), String.class);
        assertThat(reuseOldRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 9. Logout revokes the current refresh token
        ResponseEntity<String> logoutResponse = restTemplate.postForEntity(
                "/api/v1/auth/logout", jsonBody(Map.of("refreshToken", newRefreshToken)), String.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> refreshAfterLogout = restTemplate.postForEntity(
                "/api/v1/auth/refresh", jsonBody(Map.of("refreshToken", newRefreshToken)), String.class);
        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminEndpoint_rejectsPlainUser_allowsAdmin() throws Exception {
        // Plain USER is forbidden
        String userEmail = "user.rbac+" + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(Map.of(
                "name", "Regular User", "email", userEmail, "mobile", "9123456780",
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        )), String.class);

        ResponseEntity<String> userLogin = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", userEmail, "password", "SecurePass123")), String.class);
        String userAccessToken = objectMapper.readTree(userLogin.getBody()).get("data").get("accessToken").asText();

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userAccessToken);
        ResponseEntity<String> forbidden = restTemplate.exchange(
                "/api/v1/admin/ping", HttpMethod.GET, new HttpEntity<>(userHeaders), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Elevate a second user to ADMIN directly via the repository (no
        // admin-management endpoint exists yet - that arrives with the
        // Admin module) and confirm they CAN reach the admin endpoint.
        String adminEmail = "admin.rbac+" + System.nanoTime() + "@example.com";
        User adminUser = new User();
        adminUser.setName("Platform Admin");
        adminUser.setEmail(adminEmail);
        adminUser.setMobile("9000000001");
        adminUser.setPasswordHash(passwordEncoder.encode("SecurePass123"));
        adminUser.setActive(true);
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        adminUser.addRole(adminRole);
        userRepository.save(adminUser);

        ResponseEntity<String> adminLogin = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", adminEmail, "password", "SecurePass123")), String.class);
        String adminAccessToken = objectMapper.readTree(adminLogin.getBody()).get("data").get("accessToken").asText();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);
        ResponseEntity<String> allowed = restTemplate.exchange(
                "/api/v1/admin/ping", HttpMethod.GET, new HttpEntity<>(adminHeaders), String.class);
        assertThat(allowed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allowed.getBody()).contains("pong");
    }

    @Test
    void register_rejectsMismatchedPasswords() throws Exception {
        Map<String, String> payload = Map.of(
                "name", "Mismatch Test",
                "email", "mismatch+" + System.nanoTime() + "@example.com",
                "mobile", "9988776655",
                "password", "SecurePass123",
                "confirmPassword", "DoesNotMatch1"
        );
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(payload), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
