package com.examforge.result;

import com.examforge.AbstractIntegrationTest;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.subject.domain.Subject;
import com.examforge.subject.repository.SubjectRepository;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestStatus;
import com.examforge.test.repository.TestPaperRepository;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResultIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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

    private String registerAndLogin(String emailPrefix) throws Exception {
        String email = emailPrefix + "+" + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(Map.of(
                "name", "Result Tester", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    private TestPaper createPublishedTest(int durationMinutes) {
        Exam exam = new Exam();
        exam.setName("Result Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(10);
        exam.setMaximumMarks(new BigDecimal("10.00"));
        exam.setNegativeMarking(new BigDecimal("0.00"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);

        Subject subject = new Subject();
        subject.setExam(exam);
        subject.setName("Result Fixture Subject " + System.nanoTime());
        subject.setDisplayOrder(1);
        subjectRepository.save(subject);

        TestPaper test = new TestPaper();
        test.setExam(exam);
        test.setName("Result Fixture Test " + System.nanoTime());
        test.setDurationMinutes(durationMinutes);
        test.setTotalMarks(new BigDecimal("0.00"));
        test.setNegativeMarking(new BigDecimal("0.00"));
        test.setStatus(TestStatus.PUBLISHED);
        test.setFree(true);
        return testRepository.save(test);
    }

    @Test
    void myAttempts_showsHistoryAcrossTests_scopedToOwner() throws Exception {
        TestPaper test1 = createPublishedTest(30);
        TestPaper test2 = createPublishedTest(30);
        String userToken = registerAndLogin("history.user");
        String otherToken = registerAndLogin("history.other");

        // Start attempts on both tests as the main user
        restTemplate.exchange("/api/v1/tests/" + test1.getId() + "/attempts", HttpMethod.POST, authOnly(userToken), String.class);
        restTemplate.exchange("/api/v1/tests/" + test2.getId() + "/attempts", HttpMethod.POST, authOnly(userToken), String.class);

        // A different user's own attempt shouldn't show up in the first user's history
        restTemplate.exchange("/api/v1/tests/" + test1.getId() + "/attempts", HttpMethod.POST, authOnly(otherToken), String.class);

        ResponseEntity<String> historyResp = restTemplate.exchange(
                "/api/v1/users/me/attempts", HttpMethod.GET, authOnly(userToken), String.class);
        assertThat(historyResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode history = objectMapper.readTree(historyResp.getBody()).get("data").get("content");
        assertThat(history.size()).isEqualTo(2);

        // Per-test history for test1 only shows the main user's own attempt on it (1), not the other user's
        ResponseEntity<String> perTestResp = restTemplate.exchange(
                "/api/v1/tests/" + test1.getId() + "/attempts/me", HttpMethod.GET, authOnly(userToken), String.class);
        JsonNode perTest = objectMapper.readTree(perTestResp.getBody()).get("data").get("content");
        assertThat(perTest.size()).isEqualTo(1);
        assertThat(perTest.get(0).get("testId").asText()).isEqualTo(test1.getId().toString());
    }

    @Test
    void myAttempts_requiresAuthentication() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/me/attempts", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
