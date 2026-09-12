package com.examforge.bookmark;

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

class BookmarkIntegrationTest extends AbstractIntegrationTest {

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
                "name", "Bookmark Tester", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    private Question createQuestionFixture() {
        Exam exam = new Exam();
        exam.setName("Bookmark Fixture Exam " + System.nanoTime());
        exam.setDurationMinutes(60);
        exam.setTotalQuestions(10);
        exam.setMaximumMarks(new BigDecimal("10.00"));
        exam.setNegativeMarking(new BigDecimal("0.00"));
        exam.setDifficulty(ExamDifficulty.MEDIUM);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);

        Subject subject = new Subject();
        subject.setExam(exam);
        subject.setName("Bookmark Fixture Subject " + System.nanoTime());
        subject.setDisplayOrder(1);
        subject = subjectRepository.save(subject);

        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setName("Bookmark Fixture Topic " + System.nanoTime());
        topic.setDisplayOrder(1);
        topic = topicRepository.save(topic);

        Question question = new Question();
        question.setTopic(topic);
        question.setQuestionType(QuestionType.NUMERIC);
        question.setQuestionText("Bookmark fixture question " + System.nanoTime());
        question.setDifficulty(ExamDifficulty.EASY);
        question.setMarks(new BigDecimal("1.00"));
        question.setNegativeMarks(new BigDecimal("0.00"));
        question.setStatus(QuestionStatus.PUBLISHED);
        question.setCorrectNumericAnswer(new BigDecimal("42"));
        return questionRepository.save(question);
    }

    @Test
    void addListRemove_fullLifecycle_scopedToOwner() throws Exception {
        Question question = createQuestionFixture();
        String userToken = registerAndLogin("bookmark.user");
        String otherToken = registerAndLogin("bookmark.other");

        // Add
        ResponseEntity<String> addResp = restTemplate.exchange(
                "/api/v1/bookmarks", HttpMethod.POST,
                jsonBody(Map.of("questionId", question.getId().toString()), userToken), String.class);
        assertThat(addResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode added = objectMapper.readTree(addResp.getBody()).get("data");
        assertThat(added.get("questionId").asText()).isEqualTo(question.getId().toString());

        // Duplicate add rejected
        ResponseEntity<String> dupeResp = restTemplate.exchange(
                "/api/v1/bookmarks", HttpMethod.POST,
                jsonBody(Map.of("questionId", question.getId().toString()), userToken), String.class);
        assertThat(dupeResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Shows up in the owner's list
        ResponseEntity<String> listResp = restTemplate.exchange(
                "/api/v1/bookmarks", HttpMethod.GET, authOnly(userToken), String.class);
        JsonNode content = objectMapper.readTree(listResp.getBody()).get("data").get("content");
        assertThat(content.size()).isEqualTo(1);

        // Does NOT show up for a different user
        ResponseEntity<String> otherListResp = restTemplate.exchange(
                "/api/v1/bookmarks", HttpMethod.GET, authOnly(otherToken), String.class);
        JsonNode otherContent = objectMapper.readTree(otherListResp.getBody()).get("data").get("content");
        assertThat(otherContent.size()).isEqualTo(0);

        // Remove
        ResponseEntity<String> removeResp = restTemplate.exchange(
                "/api/v1/bookmarks/" + question.getId(), HttpMethod.DELETE, authOnly(userToken), String.class);
        assertThat(removeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> listAfterRemove = restTemplate.exchange(
                "/api/v1/bookmarks", HttpMethod.GET, authOnly(userToken), String.class);
        assertThat(objectMapper.readTree(listAfterRemove.getBody()).get("data").get("content").size()).isEqualTo(0);
    }

    @Test
    void removingSomeoneElsesBookmark_orNonexistentOne_returns404() throws Exception {
        Question question = createQuestionFixture();
        String userToken = registerAndLogin("bookmark.notfound.user");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/bookmarks/" + question.getId(), HttpMethod.DELETE, authOnly(userToken), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void bookmarks_requireAuthentication() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/bookmarks", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
