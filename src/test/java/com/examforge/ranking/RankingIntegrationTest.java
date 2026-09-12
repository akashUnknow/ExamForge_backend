package com.examforge.ranking;

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
import com.examforge.subject.domain.Subject;
import com.examforge.subject.repository.SubjectRepository;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestQuestion;
import com.examforge.test.domain.TestStatus;
import com.examforge.test.repository.TestPaperRepository;
import com.examforge.test.repository.TestQuestionRepository;
import com.examforge.topic.domain.Topic;
import com.examforge.topic.repository.TopicRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RankingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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

    private String registerAndLogin(String namePrefix) throws Exception {
        String email = namePrefix.toLowerCase().replace(" ", ".") + "+" + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(Map.of(
                "name", namePrefix, "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    /** Published exam -> subject -> topic -> a single published MCQ question worth 1 mark, no negative marking. */
    private TestPaper createRankedTestFixture() {
        Exam exam = new Exam();
        exam.setName("Ranking Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(1);
        exam.setMaximumMarks(new BigDecimal("1.00"));
        exam.setNegativeMarking(new BigDecimal("0.00"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);

        Subject subject = new Subject();
        subject.setExam(exam);
        subject.setName("Ranking Fixture Subject " + System.nanoTime());
        subject.setDisplayOrder(1);
        subject = subjectRepository.save(subject);

        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setName("Ranking Fixture Topic " + System.nanoTime());
        topic.setDisplayOrder(1);
        topic = topicRepository.save(topic);

        Question question = new Question();
        question.setTopic(topic);
        question.setQuestionType(QuestionType.MCQ);
        question.setQuestionText("Ranking fixture question " + System.nanoTime());
        question.setDifficulty(ExamDifficulty.EASY);
        question.setMarks(new BigDecimal("1.00"));
        question.setNegativeMarks(new BigDecimal("0.00"));
        question.setStatus(QuestionStatus.PUBLISHED);
        question.replaceOptions(List.of(
                new QuestionOption("Wrong", false, 1),
                new QuestionOption("Right", true, 2)
        ));
        question = questionRepository.save(question);

        TestPaper test = new TestPaper();
        test.setExam(exam);
        test.setName("Ranking Fixture Test " + System.nanoTime());
        test.setDurationMinutes(30);
        test.setTotalMarks(new BigDecimal("1.00"));
        test.setNegativeMarking(new BigDecimal("0.00"));
        test.setStatus(TestStatus.PUBLISHED);
        test.setFree(true);
        test = testRepository.save(test);

        TestQuestion tq = new TestQuestion();
        tq.setTest(test);
        tq.setQuestion(question);
        tq.setDisplayOrder(1);
        tq.setMarks(question.getMarks());
        tq.setNegativeMarks(question.getNegativeMarks());
        testQuestionRepository.save(tq);

        return test;
    }

    private String attemptAndSubmit(TestPaper test, String userToken, boolean answerCorrectly) throws Exception {
        ResponseEntity<String> start = restTemplate.exchange(
                "/api/v1/tests/" + test.getId() + "/attempts", HttpMethod.POST, authOnly(userToken), String.class);
        String attemptId = objectMapper.readTree(start.getBody()).get("data").get("id").asText();

        ResponseEntity<String> questionsResp = restTemplate.exchange(
                "/api/v1/attempts/" + attemptId + "/questions", HttpMethod.GET, authOnly(userToken), String.class);
        JsonNode question = objectMapper.readTree(questionsResp.getBody()).get("data").get(0);
        String questionId = question.get("id").asText();

        if (answerCorrectly) {
            // Find the option with text "Right" since this response never includes correctness
            String optionId = null;
            for (JsonNode option : question.get("options")) {
                if (option.get("optionText").asText().equals("Right")) {
                    optionId = option.get("id").asText();
                }
            }
            restTemplate.exchange("/api/v1/attempts/" + attemptId + "/answers", HttpMethod.POST,
                    jsonBody(Map.of("questionId", questionId, "selectedOptionIds", List.of(optionId)), userToken), String.class);
        }
        // else: leave unanswered, resulting in a 0 score

        restTemplate.exchange("/api/v1/attempts/" + attemptId + "/submit", HttpMethod.POST, authOnly(userToken), String.class);
        return attemptId;
    }

    @Test
    void ranking_ordersByScore_oneEntryPerUser_noPersonalDataLeak() throws Exception {
        TestPaper test = createRankedTestFixture();

        String topScorer = registerAndLogin("Priya Topscorer");
        String lowScorer = registerAndLogin("Raj Lowscorer");

        attemptAndSubmit(test, topScorer, true);   // score 1.00
        attemptAndSubmit(test, lowScorer, false);  // score 0.00

        // The top scorer retakes and scores low the second time - ranking
        // should still only show their BEST attempt (1.00), not a duplicate entry
        attemptAndSubmit(test, topScorer, false);

        ResponseEntity<String> rankingResp = restTemplate.getForEntity(
                "/api/v1/tests/" + test.getId() + "/ranking", String.class);
        assertThat(rankingResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode content = objectMapper.readTree(rankingResp.getBody()).get("data").get("content");
        assertThat(content.size()).isEqualTo(2); // one entry per user, not three

        assertThat(content.get(0).get("rank").asInt()).isEqualTo(1);
        assertThat(content.get(0).get("userName").asText()).isEqualTo("Priya Topscorer");
        assertThat(content.get(0).get("score").asDouble()).isEqualTo(1.00);

        assertThat(content.get(1).get("rank").asInt()).isEqualTo(2);
        assertThat(content.get(1).get("userName").asText()).isEqualTo("Raj Lowscorer");
        assertThat(content.get(1).get("score").asDouble()).isEqualTo(0.00);

        // No email or mobile anywhere in the leaderboard payload
        assertThat(rankingResp.getBody()).doesNotContain("@example.com").doesNotContain("email").doesNotContain("mobile");
    }

    @Test
    void ranking_isPubliclyAccessible_noAuthRequired() throws Exception {
        TestPaper test = createRankedTestFixture();
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/tests/" + test.getId() + "/ranking", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void ranking_forNonPublishedTest_returns404() throws Exception {
        // A test id that was never created at all behaves the same as a draft one - not found
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/tests/" + UUID.randomUUID() + "/ranking", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
