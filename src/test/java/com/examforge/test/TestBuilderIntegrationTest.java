package com.examforge.test;

import com.examforge.AbstractIntegrationTest;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionOption;
import com.examforge.question.domain.QuestionStatus;
import com.examforge.question.domain.QuestionType;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.role.domain.Role;
import com.examforge.role.domain.RoleName;
import com.examforge.role.repository.RoleRepository;
import com.examforge.subject.domain.Subject;
import com.examforge.subject.repository.SubjectRepository;
import com.examforge.topic.domain.Topic;
import com.examforge.topic.repository.TopicRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestBuilderIntegrationTest extends AbstractIntegrationTest {

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
    private TopicRepository topicRepository;

    @Autowired
    private QuestionRepository questionRepository;

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
        admin.setName("Test Builder Admin");
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

    /** Exam (published) -> subject -> topic, plus a given number of PUBLISHED MCQ questions at each difficulty. */
    private Topic createPublishedFixture(int easyQuestions, int mediumQuestions) {
        Exam exam = new Exam();
        exam.setName("Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(50);
        exam.setMaximumMarks(new BigDecimal("100.00"));
        exam.setNegativeMarking(new BigDecimal("0.25"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);

        Subject subject = new Subject();
        subject.setExam(exam);
        subject.setName("Fixture Subject " + System.nanoTime());
        subject.setDisplayOrder(1);
        subject = subjectRepository.save(subject);

        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setName("Fixture Topic " + System.nanoTime());
        topic.setDisplayOrder(1);
        topic = topicRepository.save(topic);

        seedPublishedQuestions(topic, ExamDifficulty.EASY, easyQuestions);
        seedPublishedQuestions(topic, ExamDifficulty.MEDIUM, mediumQuestions);

        return topic;
    }

    private void seedPublishedQuestions(Topic topic, ExamDifficulty difficulty, int count) {
        for (int i = 0; i < count; i++) {
            Question question = new Question();
            question.setTopic(topic);
            question.setQuestionType(QuestionType.MCQ);
            question.setQuestionText("Fixture question " + difficulty + " #" + i + " " + System.nanoTime());
            question.setDifficulty(difficulty);
            question.setMarks(new BigDecimal("1.00"));
            question.setNegativeMarks(new BigDecimal("0.25"));
            question.setStatus(QuestionStatus.PUBLISHED);
            question.replaceOptions(List.of(
                    new QuestionOption("A", false, 1),
                    new QuestionOption("B", true, 2)
            ));
            questionRepository.save(question);
        }
    }

    @Test
    void manualAssembly_draftHiddenUntilPublished_thenPublicVisibility() throws Exception {
        Topic topic = createPublishedFixture(3, 0);
        String adminToken = createAdminAndLogin("builder.admin");

        UUID examId = topic.getSubject().getExam().getId();

        // 1. Create a test - starts DRAFT
        Map<String, Object> testPayload = Map.of(
                "examId", examId.toString(),
                "name", "Manual Assembly Test " + System.nanoTime(),
                "durationMinutes", 30,
                "totalMarks", 10,
                "negativeMarking", 0.25,
                "free", true
        );
        ResponseEntity<String> createResp = restTemplate.exchange(
                "/api/v1/admin/tests", HttpMethod.POST, jsonBody(testPayload, adminToken), String.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = objectMapper.readTree(createResp.getBody()).get("data");
        String testId = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");

        // 2. Draft test invisible publicly
        ResponseEntity<String> publicGetDraft = restTemplate.getForEntity("/api/v1/tests/" + testId, String.class);
        assertThat(publicGetDraft.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 3. Fetch a PUBLISHED question from the fixture to attach manually
        ResponseEntity<String> questionsResp = restTemplate.exchange(
                "/api/v1/questions?topicId=" + topic.getId() + "&status=PUBLISHED",
                HttpMethod.GET, authOnly(adminToken), String.class);
        String questionId = objectMapper.readTree(questionsResp.getBody())
                .get("data").get("content").get(0).get("id").asText();

        // 4. Attach it manually
        ResponseEntity<String> attachResp = restTemplate.exchange(
                "/api/v1/admin/tests/" + testId + "/questions", HttpMethod.POST,
                jsonBody(Map.of("questionId", questionId), adminToken), String.class);
        assertThat(attachResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 5. Duplicate attach is rejected
        ResponseEntity<String> dupeAttach = restTemplate.exchange(
                "/api/v1/admin/tests/" + testId + "/questions", HttpMethod.POST,
                jsonBody(Map.of("questionId", questionId), adminToken), String.class);
        assertThat(dupeAttach.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 6. Deleting the test is blocked while a question is attached
        ResponseEntity<String> deleteBlocked = restTemplate.exchange(
                "/api/v1/admin/tests/" + testId, HttpMethod.DELETE, authOnly(adminToken), String.class);
        assertThat(deleteBlocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 7. Publish the test
        Map<String, Object> publishPayload = new HashMap<>(testPayload);
        publishPayload.put("status", "PUBLISHED");
        ResponseEntity<String> publishResp = restTemplate.exchange(
                "/api/v1/admin/tests/" + testId, HttpMethod.PUT, jsonBody(publishPayload, adminToken), String.class);
        assertThat(publishResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 8. Now publicly visible, and the public payload contains NO question content at all
        ResponseEntity<String> publicGetPublished = restTemplate.getForEntity("/api/v1/tests/" + testId, String.class);
        assertThat(publicGetPublished.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicGetPublished.getBody()).doesNotContain("questionText").doesNotContain("\"correct\"");

        ResponseEntity<String> publicList = restTemplate.getForEntity("/api/v1/tests?size=100", String.class);
        assertThat(publicList.getBody()).contains(testId);
    }

    @Test
    void autoGenerate_allOrNothing_andRandomSelection() throws Exception {
        Topic topic = createPublishedFixture(5, 2);
        String adminToken = createAdminAndLogin("autogen.admin");
        UUID examId = topic.getSubject().getExam().getId();

        Map<String, Object> testPayload = Map.of(
                "examId", examId.toString(), "name", "Auto Gen Test " + System.nanoTime(),
                "durationMinutes", 30, "totalMarks", 10, "negativeMarking", 0.25, "free", true
        );
        ResponseEntity<String> createResp = restTemplate.exchange(
                "/api/v1/admin/tests", HttpMethod.POST, jsonBody(testPayload, adminToken), String.class);
        String testId = objectMapper.readTree(createResp.getBody()).get("data").get("id").asText();

        // Request more MEDIUM questions than exist (only 2 seeded) - should fail, adding nothing
        Map<String, Object> tooMany = Map.of("buckets", List.of(
                Map.of("topicId", topic.getId().toString(), "difficulty", "MEDIUM", "count", 5)
        ));
        ResponseEntity<String> overRequest = restTemplate.exchange(
                "/api/v1/admin/tests/" + testId + "/questions/auto-generate", HttpMethod.POST,
                jsonBody(tooMany, adminToken), String.class);
        assertThat(overRequest.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> afterFailedGenerate = restTemplate.exchange(
                "/api/v1/admin/tests/" + testId + "/questions", HttpMethod.GET, authOnly(adminToken), String.class);
        assertThat(objectMapper.readTree(afterFailedGenerate.getBody()).get("data").size()).isEqualTo(0);

        // Request within available counts - should succeed
        Map<String, Object> validRequest = Map.of("buckets", List.of(
                Map.of("topicId", topic.getId().toString(), "difficulty", "EASY", "count", 3),
                Map.of("topicId", topic.getId().toString(), "difficulty", "MEDIUM", "count", 2)
        ));
        ResponseEntity<String> generateResp = restTemplate.exchange(
                "/api/v1/admin/tests/" + testId + "/questions/auto-generate", HttpMethod.POST,
                jsonBody(validRequest, adminToken), String.class);
        assertThat(generateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(generateResp.getBody()).get("data").size()).isEqualTo(5);

        // Running it again with the same buckets now fails - those questions are already attached
        ResponseEntity<String> secondRun = restTemplate.exchange(
                "/api/v1/admin/tests/" + testId + "/questions/auto-generate", HttpMethod.POST,
                jsonBody(validRequest, adminToken), String.class);
        assertThat(secondRun.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void nonAdmin_cannotManageTests() throws Exception {
        String email = "plain.builder+" + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(Map.of(
                "name", "Plain User", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);
        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        String token = objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();

        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/v1/admin/tests", HttpMethod.GET, authOnly(token), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
