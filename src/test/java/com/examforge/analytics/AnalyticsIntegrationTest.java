package com.examforge.analytics;

import com.examforge.AbstractIntegrationTest;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsIntegrationTest extends AbstractIntegrationTest {

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
        admin.setName("Analytics Admin");
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
                "name", "Analytics User", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    private TestPaper createPublishedTest(int durationMinutes) {
        Exam exam = new Exam();
        exam.setName("Analytics Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(10);
        exam.setMaximumMarks(new BigDecimal("10.00"));
        exam.setNegativeMarking(new BigDecimal("0.00"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);

        Subject subject = new Subject();
        subject.setExam(exam);
        subject.setName("Analytics Fixture Subject " + System.nanoTime());
        subject.setDisplayOrder(1);
        subjectRepository.save(subject);

        TestPaper test = new TestPaper();
        test.setExam(exam);
        test.setName("Analytics Fixture Test " + System.nanoTime());
        test.setDurationMinutes(durationMinutes);
        test.setTotalMarks(new BigDecimal("0.00"));
        test.setNegativeMarking(new BigDecimal("0.00"));
        test.setStatus(TestStatus.PUBLISHED);
        test.setFree(true);
        return testRepository.save(test);
    }

    @Test
    void testAnalytics_reflectsCompletedAndInProgressAttempts() throws Exception {
        TestPaper test = createPublishedTest(30);
        String adminToken = createAdminAndLogin("analytics.admin");

        // Two users each start and submit (0 questions attached -> score 0, completed)
        String user1 = registerAndLogin("analytics.u1");
        String user2 = registerAndLogin("analytics.u2");
        String user3 = registerAndLogin("analytics.u3");

        for (String token : new String[]{user1, user2}) {
            ResponseEntity<String> start = restTemplate.exchange(
                    "/api/v1/tests/" + test.getId() + "/attempts", HttpMethod.POST, authOnly(token), String.class);
            String attemptId = objectMapper.readTree(start.getBody()).get("data").get("id").asText();
            restTemplate.exchange("/api/v1/attempts/" + attemptId + "/submit", HttpMethod.POST, authOnly(token), String.class);
        }
        // Third user just starts, never submits - stays IN_PROGRESS
        restTemplate.exchange("/api/v1/tests/" + test.getId() + "/attempts", HttpMethod.POST, authOnly(user3), String.class);

        // Non-admin is blocked
        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/v1/admin/analytics/tests/" + test.getId(), HttpMethod.GET, authOnly(user1), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/analytics/tests/" + test.getId(), HttpMethod.GET, authOnly(adminToken), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).get("data");
        assertThat(data.get("totalAttempts").asInt()).isEqualTo(3);
        assertThat(data.get("completedAttempts").asInt()).isEqualTo(2);
        assertThat(data.get("inProgressAttempts").asInt()).isEqualTo(1);
        assertThat(data.get("completionRate").asDouble()).isEqualTo(66.67);
    }

    @Test
    void analyticsEndpoints_allRequireAdmin() throws Exception {
        String userToken = registerAndLogin("analytics.plain");

        assertThat(restTemplate.exchange("/api/v1/admin/analytics/popular-tests", HttpMethod.GET, authOnly(userToken), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.exchange("/api/v1/admin/analytics/popular-exams", HttpMethod.GET, authOnly(userToken), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.exchange("/api/v1/admin/analytics/user-registrations", HttpMethod.GET, authOnly(userToken), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // And unauthenticated entirely
        assertThat(restTemplate.getForEntity("/api/v1/admin/analytics/popular-tests", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void popularTestsAndExams_and_registrationTrend_areReachableByAdmin() throws Exception {
        String adminToken = createAdminAndLogin("analytics.dashboard.admin");

        ResponseEntity<String> popularTests = restTemplate.exchange(
                "/api/v1/admin/analytics/popular-tests?limit=5", HttpMethod.GET, authOnly(adminToken), String.class);
        assertThat(popularTests.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> popularExams = restTemplate.exchange(
                "/api/v1/admin/analytics/popular-exams?limit=5", HttpMethod.GET, authOnly(adminToken), String.class);
        assertThat(popularExams.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> trend = restTemplate.exchange(
                "/api/v1/admin/analytics/user-registrations?days=7", HttpMethod.GET, authOnly(adminToken), String.class);
        assertThat(trend.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
