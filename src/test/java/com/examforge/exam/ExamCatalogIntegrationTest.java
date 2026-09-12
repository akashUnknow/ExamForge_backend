package com.examforge.exam;

import com.examforge.AbstractIntegrationTest;
import com.examforge.role.domain.Role;
import com.examforge.role.domain.RoleName;
import com.examforge.role.repository.RoleRepository;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExamCatalogIntegrationTest extends AbstractIntegrationTest {

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
        admin.setName("Catalog Admin");
        admin.setEmail(email);
        admin.setMobile("9" + String.valueOf(System.nanoTime()).substring(0, 9));
        admin.setPasswordHash(passwordEncoder.encode("SecurePass123"));
        admin.setActive(true);
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        admin.addRole(adminRole);
        userRepository.save(admin);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    private String registerAndLoginRegularUser() throws Exception {
        String email = "catalog.user+" + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(Map.of(
                "name", "Catalog User", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    @Test
    void fullCatalogLifecycle_draftIsHiddenUntilPublished_thenSubjectsAndTopicsFlow() throws Exception {
        String adminToken = createAdminAndLogin("catalog.admin");
        String userToken = registerAndLoginRegularUser();

        // 1. Non-admin cannot create a category
        ResponseEntity<String> forbidden = restTemplate.exchange(
                "/api/v1/admin/exam-categories", HttpMethod.POST,
                jsonBody(Map.of("name", "Should Fail " + System.nanoTime()), userToken), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 2. Admin creates a category
        String categoryName = "Banking " + System.nanoTime();
        ResponseEntity<String> categoryResp = restTemplate.exchange(
                "/api/v1/admin/exam-categories", HttpMethod.POST,
                jsonBody(Map.of("name", categoryName, "description", "Bank exams"), adminToken), String.class);
        assertThat(categoryResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String categoryId = objectMapper.readTree(categoryResp.getBody()).get("data").get("id").asText();

        // 3. Duplicate category name is rejected
        ResponseEntity<String> dupeCategory = restTemplate.exchange(
                "/api/v1/admin/exam-categories", HttpMethod.POST,
                jsonBody(Map.of("name", categoryName), adminToken), String.class);
        assertThat(dupeCategory.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 4. Admin creates an exam - defaults to DRAFT
        Map<String, Object> examPayload = Map.of(
                "categoryId", categoryId,
                "name", "SSC CGL " + System.nanoTime(),
                "description", "Staff Selection Commission Combined Graduate Level",
                "durationMinutes", 60,
                "totalQuestions", 100,
                "maximumMarks", 200,
                "negativeMarking", 0.25,
                "difficulty", "MEDIUM"
        );
        ResponseEntity<String> examResp = restTemplate.exchange(
                "/api/v1/admin/exams", HttpMethod.POST, jsonBody(examPayload, adminToken), String.class);
        assertThat(examResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode examData = objectMapper.readTree(examResp.getBody()).get("data");
        String examId = examData.get("id").asText();
        assertThat(examData.get("status").asText()).isEqualTo("DRAFT");

        // 5. Draft exam is invisible on the public list and 404s on direct fetch
        ResponseEntity<String> publicListBeforePublish = restTemplate.getForEntity("/api/v1/exams?size=100", String.class);
        assertThat(publicListBeforePublish.getBody()).doesNotContain(examId);

        ResponseEntity<String> publicGetDraft = restTemplate.getForEntity("/api/v1/exams/" + examId, String.class);
        assertThat(publicGetDraft.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 6. Admin can still fetch the draft exam directly
        ResponseEntity<String> adminGetDraft = restTemplate.exchange(
                "/api/v1/admin/exams/" + examId, HttpMethod.GET, authOnly(adminToken), String.class);
        assertThat(adminGetDraft.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 7. Admin publishes the exam
        Map<String, Object> publishPayload = new java.util.HashMap<>(examPayload);
        publishPayload.put("status", "PUBLISHED");
        ResponseEntity<String> publishResp = restTemplate.exchange(
                "/api/v1/admin/exams/" + examId, HttpMethod.PUT, jsonBody(publishPayload, adminToken), String.class);
        assertThat(publishResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(publishResp.getBody()).get("data").get("status").asText()).isEqualTo("PUBLISHED");

        // 8. Now it's visible publicly
        ResponseEntity<String> publicGetPublished = restTemplate.getForEntity("/api/v1/exams/" + examId, String.class);
        assertThat(publicGetPublished.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> publicListAfterPublish = restTemplate.getForEntity(
                "/api/v1/exams?difficulty=MEDIUM&size=100", String.class);
        assertThat(publicListAfterPublish.getBody()).contains(examId);

        // 9. Category deletion is blocked while the exam references it
        ResponseEntity<String> blockedCategoryDelete = restTemplate.exchange(
                "/api/v1/admin/exam-categories/" + categoryId, HttpMethod.DELETE, authOnly(adminToken), String.class);
        assertThat(blockedCategoryDelete.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 10. Admin creates a subject under the exam
        ResponseEntity<String> subjectResp = restTemplate.exchange(
                "/api/v1/admin/subjects", HttpMethod.POST,
                jsonBody(Map.of("examId", examId, "name", "Quantitative Aptitude", "displayOrder", 1), adminToken), String.class);
        assertThat(subjectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String subjectId = objectMapper.readTree(subjectResp.getBody()).get("data").get("id").asText();

        // 11. Public can list subjects under the now-published exam
        ResponseEntity<String> publicSubjects = restTemplate.getForEntity(
                "/api/v1/exams/" + examId + "/subjects", String.class);
        assertThat(publicSubjects.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicSubjects.getBody()).contains(subjectId);

        // 12. Duplicate subject name under the same exam is rejected
        ResponseEntity<String> dupeSubject = restTemplate.exchange(
                "/api/v1/admin/subjects", HttpMethod.POST,
                jsonBody(Map.of("examId", examId, "name", "Quantitative Aptitude"), adminToken), String.class);
        assertThat(dupeSubject.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 13. Exam deletion is blocked while the subject exists
        ResponseEntity<String> blockedExamDelete = restTemplate.exchange(
                "/api/v1/admin/exams/" + examId, HttpMethod.DELETE, authOnly(adminToken), String.class);
        assertThat(blockedExamDelete.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 14. Admin creates a topic under the subject
        ResponseEntity<String> topicResp = restTemplate.exchange(
                "/api/v1/admin/topics", HttpMethod.POST,
                jsonBody(Map.of("subjectId", subjectId, "name", "Percentage", "displayOrder", 1), adminToken), String.class);
        assertThat(topicResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String topicId = objectMapper.readTree(topicResp.getBody()).get("data").get("id").asText();

        // 15. Public can list topics under the subject
        ResponseEntity<String> publicTopics = restTemplate.getForEntity(
                "/api/v1/subjects/" + subjectId + "/topics", String.class);
        assertThat(publicTopics.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicTopics.getBody()).contains(topicId);

        // 16. Subject deletion is blocked while the topic exists
        ResponseEntity<String> blockedSubjectDelete = restTemplate.exchange(
                "/api/v1/admin/subjects/" + subjectId, HttpMethod.DELETE, authOnly(adminToken), String.class);
        assertThat(blockedSubjectDelete.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 17. Unwind the chain: delete topic, then subject, then exam, then category
        assertThat(restTemplate.exchange("/api/v1/admin/topics/" + topicId, HttpMethod.DELETE, authOnly(adminToken), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.exchange("/api/v1/admin/subjects/" + subjectId, HttpMethod.DELETE, authOnly(adminToken), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.exchange("/api/v1/admin/exams/" + examId, HttpMethod.DELETE, authOnly(adminToken), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.exchange("/api/v1/admin/exam-categories/" + categoryId, HttpMethod.DELETE, authOnly(adminToken), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        // 18. The exam is gone from the public catalog after deletion
        ResponseEntity<String> publicGetAfterDelete = restTemplate.getForEntity("/api/v1/exams/" + examId, String.class);
        assertThat(publicGetAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void examValidation_rejectsInvalidPayload() throws Exception {
        String adminToken = createAdminAndLogin("validation.admin");

        Map<String, Object> invalidPayload = Map.of(
                "name", "",
                "durationMinutes", -5,
                "totalQuestions", 0,
                "maximumMarks", 0,
                "negativeMarking", -1,
                "difficulty", "MEDIUM"
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/exams", HttpMethod.POST, jsonBody(invalidPayload, adminToken), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"success\":false");
    }

    @Test
    void examCategoryList_isPubliclyReadable() throws Exception {
        String adminToken = createAdminAndLogin("public.list.admin");
        String categoryName = "Railways " + System.nanoTime();

        restTemplate.exchange("/api/v1/admin/exam-categories", HttpMethod.POST,
                jsonBody(Map.of("name", categoryName), adminToken), String.class);

        ResponseEntity<String> publicList = restTemplate.getForEntity("/api/v1/exam-categories", String.class);
        assertThat(publicList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicList.getBody()).contains(categoryName);
    }
}
