package com.examforge.subscription;

import com.examforge.AbstractIntegrationTest;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.plan.domain.Plan;
import com.examforge.plan.domain.PlanType;
import com.examforge.plan.repository.PlanRepository;
import com.examforge.role.domain.Role;
import com.examforge.role.domain.RoleName;
import com.examforge.role.repository.RoleRepository;
import com.examforge.subject.domain.Subject;
import com.examforge.subject.repository.SubjectRepository;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestStatus;
import com.examforge.test.repository.TestPaperRepository;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionIntegrationTest extends AbstractIntegrationTest {

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

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TestPaperRepository testRepository;

    @Autowired
    private PlanRepository planRepository;

    private HttpEntity<String> jsonBody(Object body, String bearerToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
    }

    private HttpEntity<Void> authOnly(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        return new HttpEntity<>(headers);
    }

    private String createAdminAndLogin(String emailPrefix) throws Exception {
        String email = emailPrefix + "+" + System.nanoTime() + "@example.com";
        User admin = new User();
        admin.setName("Subscription Admin");
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

    private String registerAndLogin(String emailPrefix) throws Exception {
        String email = emailPrefix + "+" + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(Map.of(
                "name", "Subscriber", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    private UUID currentUserIdFromToken(String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> me = restTemplate.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return UUID.fromString(objectMapper.readTree(me.getBody()).get("data").get("id").asText());
    }

    private Plan createPlan(PlanType type, BigDecimal price, Integer durationDays) {
        Plan plan = new Plan();
        plan.setName(type + " Plan " + System.nanoTime());
        plan.setPlanType(type);
        plan.setPrice(price);
        plan.setDurationDays(durationDays);
        plan.setActive(true);
        return planRepository.save(plan);
    }

    private TestPaper createTestFixture(boolean free) {
        Exam exam = new Exam();
        exam.setName("Subscription Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(10);
        exam.setMaximumMarks(new BigDecimal("10.00"));
        exam.setNegativeMarking(new BigDecimal("0.00"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);

        Subject subject = new Subject();
        subject.setExam(exam);
        subject.setName("Subscription Fixture Subject " + System.nanoTime());
        subject.setDisplayOrder(1);
        subjectRepository.save(subject);

        TestPaper test = new TestPaper();
        test.setExam(exam);
        test.setName("Subscription Fixture Test " + System.nanoTime());
        test.setDurationMinutes(30);
        test.setTotalMarks(new BigDecimal("10.00"));
        test.setNegativeMarking(new BigDecimal("0.00"));
        test.setStatus(TestStatus.PUBLISHED);
        test.setFree(free);
        test.setPrice(free ? null : new BigDecimal("99.00"));
        return testRepository.save(test);
    }

    @Test
    void subscribingToFreePlan_activatesImmediately() throws Exception {
        Plan freePlan = createPlan(PlanType.FREE, null, null);
        String userToken = registerAndLogin("free.subscriber");

        ResponseEntity<String> subscribeResp = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.POST,
                jsonBody(Map.of("planId", freePlan.getId().toString()), userToken), String.class);
        assertThat(subscribeResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = objectMapper.readTree(subscribeResp.getBody()).get("data");
        assertThat(created.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(created.get("currentlyActive").asBoolean()).isTrue();

        ResponseEntity<String> activeResp = restTemplate.exchange(
                "/api/v1/subscriptions/me/active", HttpMethod.GET, authOnly(userToken), String.class);
        assertThat(activeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void subscribingToPaidPlan_landsInPending_notActive() throws Exception {
        Plan paidPlan = createPlan(PlanType.MONTHLY, new BigDecimal("499.00"), 30);
        String userToken = registerAndLogin("paid.subscriber");

        ResponseEntity<String> subscribeResp = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.POST,
                jsonBody(Map.of("planId", paidPlan.getId().toString()), userToken), String.class);
        assertThat(subscribeResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = objectMapper.readTree(subscribeResp.getBody()).get("data");
        assertThat(created.get("status").asText()).isEqualTo("PENDING");
        assertThat(created.get("currentlyActive").asBoolean()).isFalse();

        // No active subscription exists yet
        ResponseEntity<String> activeResp = restTemplate.exchange(
                "/api/v1/subscriptions/me/active", HttpMethod.GET, authOnly(userToken), String.class);
        assertThat(activeResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void secondActiveSubscription_isBlockedUntilFirstIsCancelled() throws Exception {
        Plan freePlan = createPlan(PlanType.FREE, null, null);
        String userToken = registerAndLogin("dupe.subscriber");

        ResponseEntity<String> first = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.POST,
                jsonBody(Map.of("planId", freePlan.getId().toString()), userToken), String.class);
        String subscriptionId = objectMapper.readTree(first.getBody()).get("data").get("id").asText();

        ResponseEntity<String> second = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.POST,
                jsonBody(Map.of("planId", freePlan.getId().toString()), userToken), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Cancel, then subscribing again succeeds
        ResponseEntity<String> cancelResp = restTemplate.exchange(
                "/api/v1/subscriptions/" + subscriptionId + "/cancel", HttpMethod.POST, authOnly(userToken), String.class);
        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(cancelResp.getBody()).get("data").get("status").asText()).isEqualTo("CANCELLED");

        ResponseEntity<String> third = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.POST,
                jsonBody(Map.of("planId", freePlan.getId().toString()), userToken), String.class);
        assertThat(third.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void paidTest_blocksAttemptStart_withoutActiveSubscription_thenUnblocksAfterAdminGrant() throws Exception {
        TestPaper paidTest = createTestFixture(false);
        Plan plan = createPlan(PlanType.PREMIUM, new BigDecimal("999.00"), 365);
        String adminToken = createAdminAndLogin("gate.admin");
        String userToken = registerAndLogin("gate.user");
        UUID userId = currentUserIdFromToken(userToken);

        // Blocked without any subscription
        ResponseEntity<String> blockedResp = restTemplate.exchange(
                "/api/v1/tests/" + paidTest.getId() + "/attempts", HttpMethod.POST, authOnly(userToken), String.class);
        assertThat(blockedResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Admin grants a subscription (bypasses payment)
        ResponseEntity<String> grantResp = restTemplate.exchange(
                "/api/v1/admin/subscriptions/grant", HttpMethod.POST,
                jsonBody(Map.of("userId", userId.toString(), "planId", plan.getId().toString()), adminToken), String.class);
        assertThat(grantResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(objectMapper.readTree(grantResp.getBody()).get("data").get("status").asText()).isEqualTo("ACTIVE");

        // Now unblocked
        ResponseEntity<String> allowedResp = restTemplate.exchange(
                "/api/v1/tests/" + paidTest.getId() + "/attempts", HttpMethod.POST, authOnly(userToken), String.class);
        assertThat(allowedResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void freeTest_neverRequiresSubscription() throws Exception {
        TestPaper freeTest = createTestFixture(true);
        String userToken = registerAndLogin("free.test.user");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/tests/" + freeTest.getId() + "/attempts", HttpMethod.POST, authOnly(userToken), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void adminGrant_requiresAdminRole() throws Exception {
        Plan plan = createPlan(PlanType.FREE, null, null);
        String userToken = registerAndLogin("nonadmin.granter");
        UUID userId = currentUserIdFromToken(userToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/subscriptions/grant", HttpMethod.POST,
                jsonBody(Map.of("userId", userId.toString(), "planId", plan.getId().toString()), userToken), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
