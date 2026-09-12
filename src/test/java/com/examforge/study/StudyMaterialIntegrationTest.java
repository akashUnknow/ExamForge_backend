package com.examforge.study;

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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StudyMaterialIntegrationTest extends AbstractIntegrationTest {

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
        admin.setName("Study Admin");
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
                "name", "Plain User", "email", email, "mobile", "9" + String.valueOf(System.nanoTime()).substring(0, 9),
                "password", "SecurePass123", "confirmPassword", "SecurePass123"
        ), null), String.class);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(Map.of("email", email, "password", "SecurePass123"), null), String.class);
        return objectMapper.readTree(login.getBody()).get("data").get("accessToken").asText();
    }

    private ResponseEntity<String> uploadFixtureFile(String adminToken, String title) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("title", title);
        body.add("description", "A fixture study material");
        body.add("type", "NOTES");

        byte[] content = "Hello, this is fixture study material content.".getBytes(StandardCharsets.UTF_8);
        ByteArrayResource fileResource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "fixture-notes.txt";
            }
        };
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(adminToken);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return restTemplate.exchange("/api/v1/admin/study-materials", HttpMethod.POST, requestEntity, String.class);
    }

    @Test
    void upload_thenPubliclyListable_gettable_andDownloadable() throws Exception {
        String adminToken = createAdminAndLogin("study.admin");
        String title = "Percentage Formula Sheet " + System.nanoTime();

        ResponseEntity<String> uploadResp = uploadFixtureFile(adminToken, title);
        assertThat(uploadResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode uploaded = objectMapper.readTree(uploadResp.getBody()).get("data");
        String materialId = uploaded.get("id").asText();
        assertThat(uploaded.get("title").asText()).isEqualTo(title);
        assertThat(uploaded.get("materialType").asText()).isEqualTo("NOTES");
        assertThat(uploaded.get("fileSizeBytes").asLong()).isGreaterThan(0);

        // Publicly listable, no auth needed
        ResponseEntity<String> listResp = restTemplate.getForEntity("/api/v1/study-materials?type=NOTES&size=100", String.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).contains(materialId);

        // Publicly gettable by id
        ResponseEntity<String> getResp = restTemplate.getForEntity("/api/v1/study-materials/" + materialId, String.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Publicly downloadable, and the content matches what was uploaded
        ResponseEntity<byte[]> downloadResp = restTemplate.getForEntity(
                "/api/v1/study-materials/" + materialId + "/file", byte[].class);
        assertThat(downloadResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(downloadResp.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("Hello, this is fixture study material content.");
    }

    @Test
    void nonAdmin_cannotUpload() throws Exception {
        String userToken = registerAndLogin("study.plain");
        ResponseEntity<String> response = uploadFixtureFile(userToken, "Should Fail " + System.nanoTime());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_removesMetadataAndUnderlyingFile() throws Exception {
        String adminToken = createAdminAndLogin("study.delete.admin");
        ResponseEntity<String> uploadResp = uploadFixtureFile(adminToken, "To Be Deleted " + System.nanoTime());
        String materialId = objectMapper.readTree(uploadResp.getBody()).get("data").get("id").asText();

        ResponseEntity<String> deleteResp = restTemplate.exchange(
                "/api/v1/admin/study-materials/" + materialId, HttpMethod.DELETE, authOnly(adminToken), String.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Metadata is gone (soft-deleted, filtered by @SQLRestriction)
        ResponseEntity<String> getAfterDelete = restTemplate.getForEntity("/api/v1/study-materials/" + materialId, String.class);
        assertThat(getAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
