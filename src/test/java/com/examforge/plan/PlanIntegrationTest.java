package com.examforge.plan;

import com.examforge.AbstractIntegrationTest;
import com.examforge.role.domain.Role;
import com.examforge.role.domain.RoleName;
import com.examforge.role.repository.RoleRepository;
import com.examforge.user.domain.User;
import com.examforge.user.repository.UserRepository;
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

class PlanIntegrationTest extends AbstractIntegrationTest {

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

    private HttpEntity<String> jsonBody(Object body, String bearerToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
    }

    private String createAdminAndLogin(String emailPrefix) throws Exception {
        String email = emailPrefix + "+" + System.nanoTime() + "@example.com";
        User admin = new User();
        admin.setName("Plan Admin");
        admin.setEmail(email);
        admin.setMobile("9" + String.valueOf(System.nanoTime()).substring(0, 9));
        admin.setPasswordHash(passwordEncoder.encode("SecurePass123"));
        admin.setActive(true);
        Role role = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        admin.addRole(role);
        userRepository.save(admin);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    @Test
    void adminCreatesPlan_thenItAppearsInPublicActiveList() throws Exception {
        String adminToken = createAdminAndLogin("plan.admin");
        String name = "Gold Plan " + System.nanoTime();

        Map<String, Object> payload = Map.of("name", name, "planType", "YEARLY", "price", 1999, "durationDays", 365);
        ResponseEntity<String> createResp = restTemplate.exchange(
                "/api/v1/admin/plans", HttpMethod.POST, jsonBody(payload, adminToken), String.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> publicList = restTemplate.getForEntity("/api/v1/plans", String.class);
        assertThat(publicList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicList.getBody()).contains(name);
    }

    @Test
    void freePlan_doesNotRequirePrice_paidPlanDoes() throws Exception {
        String adminToken = createAdminAndLogin("plan.pricing.admin");

        Map<String, Object> freePayload = Map.of("name", "Basic Free " + System.nanoTime(), "planType", "FREE");
        ResponseEntity<String> freeResp = restTemplate.exchange(
                "/api/v1/admin/plans", HttpMethod.POST, jsonBody(freePayload, adminToken), String.class);
        assertThat(freeResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> paidPayloadMissingPrice = Map.of("name", "Paid No Price " + System.nanoTime(), "planType", "MONTHLY");
        ResponseEntity<String> paidResp = restTemplate.exchange(
                "/api/v1/admin/plans", HttpMethod.POST, jsonBody(paidPayloadMissingPrice, adminToken), String.class);
        assertThat(paidResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void adminPlanEndpoints_requireAdmin() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/admin/plans", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
