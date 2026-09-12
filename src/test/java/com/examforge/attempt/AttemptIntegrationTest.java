package com.examforge.attempt;

import com.examforge.AbstractIntegrationTest;
import com.examforge.attempt.domain.AttemptStatus;
import com.examforge.attempt.domain.TestAttempt;
import com.examforge.attempt.repository.TestAttemptRepository;
import com.examforge.attempt.service.AttemptService;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionOption;
import com.examforge.question.domain.QuestionStatus;
import com.examforge.question.domain.QuestionType;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.subject.domain.Subject;
import com.examforge.subject.repository.SubjectRepository;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestQuestion;
import com.examforge.test.domain.TestStatus;
import com.examforge.test.repository.TestPaperRepository;
import com.examforge.test.repository.TestQuestionRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AttemptIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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

    @Autowired
    private TestPaperRepository testRepository;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private TestAttemptRepository attemptRepository;

    @Autowired
    private AttemptService attemptService;

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
                "name", "Attempt Tester", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    /** Published exam -> subject -> topic -> a published MCQ test with a known answer key, worth 1 mark, -0.25 penalty. */
    private TestPaper createPublishedTestFixture(int durationMinutes) {
        Exam exam = new Exam();
        exam.setName("Attempt Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(50);
        exam.setMaximumMarks(new BigDecimal("100.00"));
        exam.setNegativeMarking(new BigDecimal("0.25"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);

        Subject subject = new Subject();
        subject.setExam(exam);
        subject.setName("Attempt Fixture Subject " + System.nanoTime());
        subject.setDisplayOrder(1);
        subject = subjectRepository.save(subject);

        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setName("Attempt Fixture Topic " + System.nanoTime());
        topic.setDisplayOrder(1);
        topic = topicRepository.save(topic);

        // Q1: MCQ, correct answer is "4"
        Question q1 = new Question();
        q1.setTopic(topic);
        q1.setQuestionType(QuestionType.MCQ);
        q1.setQuestionText("What is 2 + 2?");
        q1.setExplanation("Basic addition.");
        q1.setDifficulty(ExamDifficulty.EASY);
        q1.setMarks(new BigDecimal("1.00"));
        q1.setNegativeMarks(new BigDecimal("0.25"));
        q1.setStatus(QuestionStatus.PUBLISHED);
        q1.replaceOptions(List.of(
                new QuestionOption("3", false, 1),
                new QuestionOption("4", true, 2),
                new QuestionOption("5", false, 3)
        ));
        q1 = questionRepository.save(q1);

        // Q2: NUMERIC, correct answer is 10
        Question q2 = new Question();
        q2.setTopic(topic);
        q2.setQuestionType(QuestionType.NUMERIC);
        q2.setQuestionText("What is 5 * 2?");
        q2.setDifficulty(ExamDifficulty.EASY);
        q2.setMarks(new BigDecimal("2.00"));
        q2.setNegativeMarks(new BigDecimal("0.00"));
        q2.setStatus(QuestionStatus.PUBLISHED);
        q2.setCorrectNumericAnswer(new BigDecimal("10"));
        q2 = questionRepository.save(q2);

        TestPaper test = new TestPaper();
        test.setExam(exam);
        test.setName("Attempt Fixture Test " + System.nanoTime());
        test.setDurationMinutes(durationMinutes);
        test.setTotalMarks(new BigDecimal("3.00"));
        test.setNegativeMarking(new BigDecimal("0.25"));
        test.setStatus(TestStatus.PUBLISHED);
        test.setFree(true);
        test = testRepository.save(test);

        TestQuestion tq1 = new TestQuestion();
        tq1.setTest(test);
        tq1.setQuestion(q1);
        tq1.setDisplayOrder(1);
        tq1.setMarks(q1.getMarks());
        tq1.setNegativeMarks(q1.getNegativeMarks());
        testQuestionRepository.save(tq1);

        TestQuestion tq2 = new TestQuestion();
        tq2.setTest(test);
        tq2.setQuestion(q2);
        tq2.setDisplayOrder(2);
        tq2.setMarks(q2.getMarks());
        tq2.setNegativeMarks(q2.getNegativeMarks());
        testQuestionRepository.save(tq2);

        return test;
    }

    private UUID correctOptionId(Question question) {
        return question.getOptions().stream().filter(QuestionOption::isCorrect).findFirst().orElseThrow().getId();
    }

    @Test
    void fullLifecycle_correctAndIncorrectAnswers_scoredServerSide() throws Exception {
        TestPaper test = createPublishedTestFixture(30);
        String token = registerAndLogin("attempt.user");

        // 1. Start the attempt
        ResponseEntity<String> startResp = restTemplate.exchange(
                "/api/v1/tests/" + test.getId() + "/attempts", HttpMethod.POST, authOnly(token), String.class);
        assertThat(startResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode startData = objectMapper.readTree(startResp.getBody()).get("data");
        String attemptId = startData.get("id").asText();
        assertThat(startData.get("status").asText()).isEqualTo("IN_PROGRESS");

        // 2. Starting again resumes the SAME attempt (no duplicate)
        ResponseEntity<String> secondStart = restTemplate.exchange(
                "/api/v1/tests/" + test.getId() + "/attempts", HttpMethod.POST, authOnly(token), String.class);
        assertThat(objectMapper.readTree(secondStart.getBody()).get("data").get("id").asText()).isEqualTo(attemptId);

        // 3. Get questions - answer-free. Assert no leak of the answer key anywhere in the payload.
        ResponseEntity<String> questionsResp = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/questions", HttpMethod.GET, authOnly(token), String.class);
        assertThat(questionsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String questionsBody = questionsResp.getBody();
        assertThat(questionsBody)
                .doesNotContain("\"correct\"")
                .doesNotContain("explanation")
                .doesNotContain("correctNumericAnswer")
                .doesNotContain("Basic addition");

        JsonNode questions = objectMapper.readTree(questionsBody).get("data");
        assertThat(questions.size()).isEqualTo(2);
        String q1Id = questions.get(0).get("id").asText();
        String q2Id = questions.get(1).get("id").asText();

        // 4. Answer Q1 correctly (option "4") and Q2 incorrectly (7 instead of 10)
        Question q1 = questionRepository.findById(UUID.fromString(q1Id)).orElseThrow();
        UUID correctOption = correctOptionId(q1);

        ResponseEntity<String> answer1 = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/answers", HttpMethod.POST,
                jsonBody(Map.of("questionId", q1Id, "selectedOptionIds", List.of(correctOption.toString())), token), String.class);
        assertThat(answer1.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> answer2 = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/answers", HttpMethod.POST,
                jsonBody(Map.of("questionId", q2Id, "numericAnswer", 7), token), String.class);
        assertThat(answer2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 5. Answering a question that doesn't belong to this test is rejected
        ResponseEntity<String> badAnswer = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/answers", HttpMethod.POST,
                jsonBody(Map.of("questionId", UUID.randomUUID().toString()), token), String.class);
        assertThat(badAnswer.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 6. Result is not available before submission
        ResponseEntity<String> resultBeforeSubmit = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/result", HttpMethod.GET, authOnly(token), String.class);
        assertThat(resultBeforeSubmit.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 7. Submit - backend computes the score: +1 (Q1 correct) - 0 (Q2 has no negative marking) = 1.00
        ResponseEntity<String> submitResp = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/submit", HttpMethod.POST, authOnly(token), String.class);
        assertThat(submitResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode result = objectMapper.readTree(submitResp.getBody()).get("data");
        assertThat(result.get("status").asText()).isEqualTo("SUBMITTED");
        assertThat(result.get("score").asDouble()).isEqualTo(1.00);
        assertThat(result.get("correctCount").asInt()).isEqualTo(1);
        assertThat(result.get("incorrectCount").asInt()).isEqualTo(1);
        assertThat(result.get("unansweredCount").asInt()).isEqualTo(0);

        // 8. Submitting again is rejected
        ResponseEntity<String> resubmit = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/submit", HttpMethod.POST, authOnly(token), String.class);
        assertThat(resubmit.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 9. Result is now available and DOES include the answer key (post-submission is allowed to)
        ResponseEntity<String> resultResp = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/result", HttpMethod.GET, authOnly(token), String.class);
        assertThat(resultResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultResp.getBody()).contains("\"correct\"").contains("Basic addition");
    }

    @Test
    void expiredAttempt_isAutoSubmittedByBackend_notFrontend() throws Exception {
        TestPaper test = createPublishedTestFixture(1);
        String token = registerAndLogin("expiry.user");

        ResponseEntity<String> startResp = restTemplate.exchange(
                "/api/v1/tests/" + test.getId() + "/attempts", HttpMethod.POST, authOnly(token), String.class);
        String attemptId = objectMapper.readTree(startResp.getBody()).get("data").get("id").asText();

        // Simulate time passing by directly rewinding expiresAt - the backend,
        // not any client-supplied timer, is what decides the attempt is over.
        TestAttempt attempt = attemptRepository.findById(UUID.fromString(attemptId)).orElseThrow();
        attempt.setExpiresAt(Instant.now().minusSeconds(5));
        attemptRepository.save(attempt);

        // Any access now triggers auto-submission
        ResponseEntity<String> questionsAfterExpiry = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/questions", HttpMethod.GET, authOnly(token), String.class);
        assertThat(questionsAfterExpiry.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        TestAttempt reloaded = attemptRepository.findById(UUID.fromString(attemptId)).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AttemptStatus.AUTO_SUBMITTED);
        assertThat(reloaded.getScore()).isNotNull();

        // The result is now available, reflecting the auto-submitted state
        ResponseEntity<String> resultResp = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/result", HttpMethod.GET, authOnly(token), String.class);
        assertThat(resultResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(resultResp.getBody()).get("data").get("status").asText())
                .isEqualTo("AUTO_SUBMITTED");
    }

    @Test
    void attemptIsOwnerOnly_otherUsersGet404NotForbidden() throws Exception {
        TestPaper test = createPublishedTestFixture(30);
        String ownerToken = registerAndLogin("owner.user");
        String strangerToken = registerAndLogin("stranger.user");

        ResponseEntity<String> startResp = restTemplate.exchange(
                "/api/v1/tests/" + test.getId() + "/attempts", HttpMethod.POST, authOnly(ownerToken), String.class);
        String attemptId = objectMapper.readTree(startResp.getBody()).get("data").get("id").asText();

        ResponseEntity<String> strangerAccess = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/questions", HttpMethod.GET, authOnly(strangerToken), String.class);
        assertThat(strangerAccess.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unauthenticated_cannotStartAttempt() throws Exception {
        TestPaper test = createPublishedTestFixture(30);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/tests/" + test.getId() + "/attempts", null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sweepJob_autoSubmitsAbandonedAttempts_thatNobodyEverRevisits() throws Exception {
        TestPaper test = createPublishedTestFixture(1);
        String token = registerAndLogin("sweep.user");

        ResponseEntity<String> startResp = restTemplate.exchange(
                "/api/v1/tests/" + test.getId() + "/attempts", HttpMethod.POST, authOnly(token), String.class);
        String attemptId = objectMapper.readTree(startResp.getBody()).get("data").get("id").asText();

        // Backdate expiry - unlike the other expiry test, nobody touches this
        // attempt again afterward. Only the sweep (not a lazy per-request
        // check) can finalize it.
        TestAttempt attempt = attemptRepository.findById(UUID.fromString(attemptId)).orElseThrow();
        attempt.setExpiresAt(Instant.now().minusSeconds(5));
        attemptRepository.save(attempt);

        int finalizedCount = attemptService.autoSubmitAllExpired();
        assertThat(finalizedCount).isGreaterThanOrEqualTo(1);

        TestAttempt reloaded = attemptRepository.findById(UUID.fromString(attemptId)).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AttemptStatus.AUTO_SUBMITTED);
        assertThat(reloaded.getScore()).isNotNull();

        // Running it again is a no-op - nothing left to sweep for this attempt
        TestAttempt beforeSecondSweep = attemptRepository.findById(UUID.fromString(attemptId)).orElseThrow();
        Instant submittedAtBefore = beforeSecondSweep.getSubmittedAt();
        attemptService.autoSubmitAllExpired();
        TestAttempt afterSecondSweep = attemptRepository.findById(UUID.fromString(attemptId)).orElseThrow();
        assertThat(afterSecondSweep.getSubmittedAt()).isEqualTo(submittedAtBefore);
    }
}
