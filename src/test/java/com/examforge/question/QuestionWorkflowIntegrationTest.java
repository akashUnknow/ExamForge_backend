package com.examforge.question;

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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionWorkflowIntegrationTest extends AbstractIntegrationTest {

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

    private String createUserWithRoleAndLogin(String emailPrefix, RoleName roleName) throws Exception {
        String email = emailPrefix + "+" + System.nanoTime() + "@example.com";
        User user = new User();
        user.setName("Test " + roleName);
        user.setEmail(email);
        user.setMobile("9" + String.valueOf(System.nanoTime()).substring(0, 9));
        user.setPasswordHash(passwordEncoder.encode("SecurePass123"));
        user.setActive(true);
        Role role = roleRepository.findByName(roleName).orElseThrow();
        user.addRole(role);
        userRepository.save(user);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    private String registerAndLoginRegularUser() throws Exception {
        String email = "plain.user+" + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(Map.of(
                "name", "Plain User", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    /** Builds a minimal exam -> subject -> topic fixture directly via repositories. */
    private Topic createTopicFixture() {
        Exam exam = new Exam();
        exam.setName("Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(50);
        exam.setMaximumMarks(new BigDecimal("100.00"));
        exam.setNegativeMarking(new BigDecimal("0.25"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.DRAFT);
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
        return topicRepository.save(topic);
    }

    private Map<String, Object> mcqPayload(String topicId) {
        return Map.of(
                "topicId", topicId,
                "questionType", "MCQ",
                "questionText", "What is 2 + 2?",
                "difficulty", "EASY",
                "marks", 1,
                "negativeMarks", 0.25,
                "options", List.of(
                        Map.of("optionText", "3", "correct", false, "displayOrder", 1),
                        Map.of("optionText", "4", "correct", true, "displayOrder", 2),
                        Map.of("optionText", "5", "correct", false, "displayOrder", 3)
                )
        );
    }

    @Test
    void fullWorkflow_draftToPublish_withOwnershipAndReviewSeparation() throws Exception {
        Topic topic = createTopicFixture();
        String creatorToken = createUserWithRoleAndLogin("creator", RoleName.CONTENT_CREATOR);
        String reviewerToken = createUserWithRoleAndLogin("reviewer", RoleName.REVIEWER);
        String adminToken = createUserWithRoleAndLogin("qadmin", RoleName.ADMIN);
        String plainUserToken = registerAndLoginRegularUser();

        // 1. Plain user cannot access the question bank at all
        ResponseEntity<String> plainUserBlocked = restTemplate.exchange(
                "/api/v1/questions", HttpMethod.GET, authOnly(plainUserToken), String.class);
        assertThat(plainUserBlocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 2. Content creator creates a question - starts in DRAFT
        ResponseEntity<String> createResp = restTemplate.exchange(
                "/api/v1/questions", HttpMethod.POST,
                jsonBody(mcqPayload(topic.getId().toString()), creatorToken), String.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = objectMapper.readTree(createResp.getBody()).get("data");
        String questionId = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");

        // 3. Reviewer cannot edit someone else's draft
        ResponseEntity<String> reviewerEditBlocked = restTemplate.exchange(
                "/api/v1/questions/" + questionId, HttpMethod.PUT,
                jsonBody(mcqPayload(topic.getId().toString()), reviewerToken), String.class);
        assertThat(reviewerEditBlocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 4. Cannot delete once it leaves DRAFT - first submit for review
        ResponseEntity<String> submitResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/submit-for-review", HttpMethod.POST, authOnly(creatorToken), String.class);
        assertThat(submitResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(submitResp.getBody()).get("data").get("status").asText()).isEqualTo("IN_REVIEW");

        ResponseEntity<String> deleteBlocked = restTemplate.exchange(
                "/api/v1/questions/" + questionId, HttpMethod.DELETE, authOnly(creatorToken), String.class);
        assertThat(deleteBlocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 5. The creator (CONTENT_CREATOR only, no REVIEWER role) cannot approve -
        // blocked by the role gate before even reaching the ownership check
        ResponseEntity<String> selfApproveBlocked = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/approve", HttpMethod.POST, authOnly(creatorToken), String.class);
        assertThat(selfApproveBlocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 6. Reviewer rejects it with a reason
        ResponseEntity<String> rejectResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/reject", HttpMethod.POST,
                jsonBody(Map.of("reason", "Explanation missing"), reviewerToken), String.class);
        assertThat(rejectResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode rejected = objectMapper.readTree(rejectResp.getBody()).get("data");
        assertThat(rejected.get("status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.get("rejectionReason").asText()).isEqualTo("Explanation missing");

        // 7. Creator can edit again while REJECTED, then resubmit
        Map<String, Object> updatedPayload = mcqPayload(topic.getId().toString());
        ResponseEntity<String> updateResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId, HttpMethod.PUT, jsonBody(updatedPayload, creatorToken), String.class);
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        restTemplate.exchange("/api/v1/questions/" + questionId + "/submit-for-review", HttpMethod.POST, authOnly(creatorToken), String.class);

        // 8. Reviewer approves this time
        ResponseEntity<String> approveResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/approve", HttpMethod.POST, authOnly(reviewerToken), String.class);
        assertThat(approveResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(approveResp.getBody()).get("data").get("status").asText()).isEqualTo("APPROVED");

        // 9. Only ADMIN/SUPER_ADMIN can publish - reviewer cannot
        ResponseEntity<String> reviewerPublishBlocked = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/publish", HttpMethod.POST, authOnly(reviewerToken), String.class);
        assertThat(reviewerPublishBlocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> publishResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/publish", HttpMethod.POST, authOnly(adminToken), String.class);
        assertThat(publishResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(publishResp.getBody()).get("data").get("status").asText()).isEqualTo("PUBLISHED");

        // 10. Topic deletion is now blocked while this question exists
        ResponseEntity<String> topicDeleteBlocked = restTemplate.exchange(
                "/api/v1/admin/topics/" + topic.getId(), HttpMethod.DELETE, authOnly(adminToken), String.class);
        assertThat(topicDeleteBlocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 11. Admin archives the published question
        ResponseEntity<String> archiveResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/archive", HttpMethod.POST, authOnly(adminToken), String.class);
        assertThat(archiveResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(archiveResp.getBody()).get("data").get("status").asText()).isEqualTo("ARCHIVED");
    }

    @Test
    void reviewerCannotApproveTheirOwnSubmission_evenWithBothRoles() throws Exception {
        Topic topic = createTopicFixture();

        // A single user holding both roles - the realistic case the
        // role-gate alone can't catch, since they'd pass hasAnyRole('REVIEWER',...).
        String email = "dualrole+" + System.nanoTime() + "@example.com";
        User user = new User();
        user.setName("Dual Role User");
        user.setEmail(email);
        user.setMobile("9" + String.valueOf(System.nanoTime()).substring(0, 9));
        user.setPasswordHash(passwordEncoder.encode("SecurePass123"));
        user.setActive(true);
        user.addRole(roleRepository.findByName(RoleName.CONTENT_CREATOR).orElseThrow());
        user.addRole(roleRepository.findByName(RoleName.REVIEWER).orElseThrow());
        userRepository.save(user);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        String token = objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();

        ResponseEntity<String> createResp = restTemplate.exchange(
                "/api/v1/questions", HttpMethod.POST, jsonBody(mcqPayload(topic.getId().toString()), token), String.class);
        String questionId = objectMapper.readTree(createResp.getBody()).get("data").get("id").asText();

        restTemplate.exchange("/api/v1/questions/" + questionId + "/submit-for-review", HttpMethod.POST, authOnly(token), String.class);

        // Passes the role gate (has REVIEWER) but is blocked by the ownership check
        ResponseEntity<String> approveResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/approve", HttpMethod.POST, authOnly(token), String.class);
        assertThat(approveResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void answerKeyValidation_rejectsInvalidShapes() throws Exception {
        Topic topic = createTopicFixture();
        String creatorToken = createUserWithRoleAndLogin("validator", RoleName.CONTENT_CREATOR);

        // MCQ with zero correct options
        Map<String, Object> badMcq = Map.of(
                "topicId", topic.getId().toString(), "questionType", "MCQ", "questionText", "Bad MCQ",
                "difficulty", "EASY", "marks", 1, "negativeMarks", 0,
                "options", List.of(
                        Map.of("optionText", "A", "correct", false),
                        Map.of("optionText", "B", "correct", false)
                )
        );
        ResponseEntity<String> badMcqResp = restTemplate.exchange(
                "/api/v1/questions", HttpMethod.POST, jsonBody(badMcq, creatorToken), String.class);
        assertThat(badMcqResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // NUMERIC without a correct numeric answer
        Map<String, Object> badNumeric = Map.of(
                "topicId", topic.getId().toString(), "questionType", "NUMERIC", "questionText", "What is pi to 2dp?",
                "difficulty", "MEDIUM", "marks", 2, "negativeMarks", 0
        );
        ResponseEntity<String> badNumericResp = restTemplate.exchange(
                "/api/v1/questions", HttpMethod.POST, jsonBody(badNumeric, creatorToken), String.class);
        assertThat(badNumericResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Valid NUMERIC question succeeds
        Map<String, Object> goodNumeric = Map.of(
                "topicId", topic.getId().toString(), "questionType", "NUMERIC", "questionText", "What is 10 / 2?",
                "difficulty", "EASY", "marks", 1, "negativeMarks", 0, "correctNumericAnswer", 5
        );
        ResponseEntity<String> goodNumericResp = restTemplate.exchange(
                "/api/v1/questions", HttpMethod.POST, jsonBody(goodNumeric, creatorToken), String.class);
        assertThat(goodNumericResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
