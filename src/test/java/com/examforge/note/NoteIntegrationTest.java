package com.examforge.note;

import com.examforge.AbstractIntegrationTest;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionStatus;
import com.examforge.question.domain.QuestionType;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.subject.domain.Subject;
import com.examforge.subject.repository.SubjectRepository;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoteIntegrationTest extends AbstractIntegrationTest {

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
                "name", "Note Tester", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    private Question createQuestionFixture() {
        Exam exam = new Exam();
        exam.setName("Note Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(10);
        exam.setMaximumMarks(new BigDecimal("10.00"));
        exam.setNegativeMarking(new BigDecimal("0.00"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);

        Subject subject = new Subject();
        subject.setExam(exam);
        subject.setName("Note Fixture Subject " + System.nanoTime());
        subject.setDisplayOrder(1);
        subject = subjectRepository.save(subject);

        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setName("Note Fixture Topic " + System.nanoTime());
        topic.setDisplayOrder(1);
        topic = topicRepository.save(topic);

        Question question = new Question();
        question.setTopic(topic);
        question.setQuestionType(QuestionType.NUMERIC);
        question.setQuestionText("Note fixture question " + System.nanoTime());
        question.setDifficulty(ExamDifficulty.EASY);
        question.setMarks(new BigDecimal("1.00"));
        question.setNegativeMarks(new BigDecimal("0.00"));
        question.setStatus(QuestionStatus.PUBLISHED);
        question.setCorrectNumericAnswer(new BigDecimal("42"));
        return questionRepository.save(question);
    }

    @Test
    void createUpdateDelete_fullLifecycle_upsertSemantics() throws Exception {
        Question question = createQuestionFixture();
        String token = registerAndLogin("note.user");
        String questionId = question.getId().toString();

        // No note yet -> 404
        ResponseEntity<String> notFoundResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/notes", HttpMethod.GET, authOnly(token), String.class);
        assertThat(notFoundResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Create
        ResponseEntity<String> createResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/notes", HttpMethod.POST,
                jsonBody(Map.of("noteText", "Remember the trick for this one"), token), String.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(createResp.getBody()).get("data").get("noteText").asText())
                .isEqualTo("Remember the trick for this one");

        // Get
        ResponseEntity<String> getResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/notes", HttpMethod.GET, authOnly(token), String.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Update (upsert - same underlying row, not a new one)
        ResponseEntity<String> updateResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/notes", HttpMethod.PUT,
                jsonBody(Map.of("noteText", "Updated note text"), token), String.class);
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updated = objectMapper.readTree(updateResp.getBody()).get("data");
        assertThat(updated.get("noteText").asText()).isEqualTo("Updated note text");

        String originalId = objectMapper.readTree(createResp.getBody()).get("data").get("id").asText();
        assertThat(updated.get("id").asText()).isEqualTo(originalId);

        // Delete
        ResponseEntity<String> deleteResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/notes", HttpMethod.DELETE, authOnly(token), String.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> afterDeleteResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/notes", HttpMethod.GET, authOnly(token), String.class);
        assertThat(afterDeleteResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void notes_areScopedToOwner_notSharedAcrossUsers() throws Exception {
        Question question = createQuestionFixture();
        String questionId = question.getId().toString();
        String userA = registerAndLogin("note.userA");
        String userB = registerAndLogin("note.userB");

        restTemplate.exchange("/api/v1/questions/" + questionId + "/notes", HttpMethod.POST,
                jsonBody(Map.of("noteText", "User A's private note"), userA), String.class);

        // User B has no note of their own on this question - sees 404, not user A's note
        ResponseEntity<String> userBResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/notes", HttpMethod.GET, authOnly(userB), String.class);
        assertThat(userBResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // User B deleting has nothing to delete
        ResponseEntity<String> userBDelete = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/notes", HttpMethod.DELETE, authOnly(userB), String.class);
        assertThat(userBDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // User A's note is untouched
        ResponseEntity<String> userAResp = restTemplate.exchange(
                "/api/v1/questions/" + questionId + "/notes", HttpMethod.GET, authOnly(userA), String.class);
        assertThat(userAResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(userAResp.getBody()).get("data").get("noteText").asText())
                .isEqualTo("User A's private note");
    }

    @Test
    void notes_requireAuthentication() throws Exception {
        Question question = createQuestionFixture();
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/questions/" + question.getId() + "/notes", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
