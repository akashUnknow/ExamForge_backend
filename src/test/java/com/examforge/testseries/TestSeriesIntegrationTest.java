package com.examforge.testseries;

import com.examforge.AbstractIntegrationTest;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.role.domain.Role;
import com.examforge.role.domain.RoleName;
import com.examforge.role.repository.RoleRepository;
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestSeriesIntegrationTest extends AbstractIntegrationTest {

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
    private TestPaperRepository testRepository;

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
        admin.setName("Series Admin");
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

    private Exam createPublishedExam() {
        Exam exam = new Exam();
        exam.setName("Series Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(10);
        exam.setMaximumMarks(new BigDecimal("10.00"));
        exam.setNegativeMarking(new BigDecimal("0.00"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.PUBLISHED);
        return examRepository.save(exam);
    }

    private TestPaper createPublishedTest(Exam exam) {
        TestPaper test = new TestPaper();
        test.setExam(exam);
        test.setName("Series Fixture Test " + System.nanoTime());
        test.setDurationMinutes(30);
        test.setTotalMarks(new BigDecimal("10.00"));
        test.setNegativeMarking(new BigDecimal("0.00"));
        test.setStatus(TestStatus.PUBLISHED);
        test.setFree(true);
        return testRepository.save(test);
    }

    @Test
    void draftSeries_hiddenUntilPublished_thenAttachDetachAndDeleteChain() throws Exception {
        Exam exam = createPublishedExam();
        TestPaper test = createPublishedTest(exam);
        String adminToken = createAdminAndLogin("series.admin");

        // Create - starts DRAFT
        Map<String, Object> payload = Map.of(
                "examId", exam.getId().toString(),
                "name", "Full Mock Series " + System.nanoTime(),
                "free", true
        );
        ResponseEntity<String> createResp = restTemplate.exchange(
                "/api/v1/admin/test-series", HttpMethod.POST, jsonBody(payload, adminToken), String.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = objectMapper.readTree(createResp.getBody()).get("data");
        String seriesId = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");

        // Hidden publicly while DRAFT
        ResponseEntity<String> publicGetDraft = restTemplate.getForEntity("/api/v1/test-series/" + seriesId, String.class);
        assertThat(publicGetDraft.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Attach a test
        ResponseEntity<String> attachResp = restTemplate.exchange(
                "/api/v1/admin/test-series/" + seriesId + "/tests", HttpMethod.POST,
                jsonBody(Map.of("testId", test.getId().toString()), adminToken), String.class);
        assertThat(attachResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Duplicate attach rejected
        ResponseEntity<String> dupeAttach = restTemplate.exchange(
                "/api/v1/admin/test-series/" + seriesId + "/tests", HttpMethod.POST,
                jsonBody(Map.of("testId", test.getId().toString()), adminToken), String.class);
        assertThat(dupeAttach.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Delete blocked while a test is attached
        ResponseEntity<String> blockedDelete = restTemplate.exchange(
                "/api/v1/admin/test-series/" + seriesId, HttpMethod.DELETE, authOnly(adminToken), String.class);
        assertThat(blockedDelete.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Publish
        Map<String, Object> publishPayload = new HashMap<>(payload);
        publishPayload.put("status", "PUBLISHED");
        ResponseEntity<String> publishResp = restTemplate.exchange(
                "/api/v1/admin/test-series/" + seriesId, HttpMethod.PUT, jsonBody(publishPayload, adminToken), String.class);
        assertThat(publishResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Now publicly visible, and its test list is browsable
        ResponseEntity<String> publicGetPublished = restTemplate.getForEntity("/api/v1/test-series/" + seriesId, String.class);
        assertThat(publicGetPublished.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> publicTestsResp = restTemplate.getForEntity("/api/v1/test-series/" + seriesId + "/tests", String.class);
        assertThat(publicTestsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicTestsResp.getBody()).contains(test.getId().toString());

        // Detach, then delete succeeds
        ResponseEntity<String> detachResp = restTemplate.exchange(
                "/api/v1/admin/test-series/" + seriesId + "/tests/" + test.getId(), HttpMethod.DELETE, authOnly(adminToken), String.class);
        assertThat(detachResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> deleteResp = restTemplate.exchange(
                "/api/v1/admin/test-series/" + seriesId, HttpMethod.DELETE, authOnly(adminToken), String.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void nonAdmin_cannotManageTestSeries() throws Exception {
        Exam exam = createPublishedExam();
        String email = "series.plain+" + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(Map.of(
                "name", "Plain User", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);
        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        String token = objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();

        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/v1/admin/test-series", HttpMethod.POST,
                jsonBody(Map.of("examId", exam.getId().toString(), "name", "Should Fail", "free", true), token), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void publicList_isReachableWithoutAuth() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/test-series?size=10", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
