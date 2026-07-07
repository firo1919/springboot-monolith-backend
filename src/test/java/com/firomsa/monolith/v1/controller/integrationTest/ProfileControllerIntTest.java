package com.firomsa.monolith.v1.controller.integrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.firomsa.monolith.model.User;
import com.firomsa.monolith.repository.RoleRepository;
import com.firomsa.monolith.repository.UserRepository;

public class ProfileControllerIntTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String BASE_URL = "/api/v1/profile";
    private static final String EMAIL = "john.doe@example.com";

    @BeforeEach
    void setUpUser() {
        userRepository.deleteAll();
        User user = User.builder().firstName("John").lastName("Doe").username("john_doe")
                .password(passwordEncoder.encode("password123")).email(EMAIL)
                .phone("+251900000001").enabled(true).active(true)
                .role(roleRepository.findByName(com.firomsa.monolith.model.Roles.ADMIN)
                        .orElseThrow())
                .build();
        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRejectUnauthorizedGetProfile() {
        assertThat(mockMvc.get().uri(BASE_URL).exchange()).hasStatus(401);
    }

    @Test
    void shouldRejectUnauthorizedUpdateProfile() {
        assertThat(mockMvc.put().uri(BASE_URL).contentType(APPLICATION_JSON).content("{}")
                .exchange()).hasStatus(401);
    }

    @Test
    void shouldRejectUnauthorizedAddProfilePicture() {
        assertThat(mockMvc.post().uri(BASE_URL + "/profile-picture").contentType(APPLICATION_JSON)
                .content("{}").exchange()).hasStatus(401);
    }

    @Test
    @WithMockUser(username = EMAIL)
    void shouldGetProfileWhenAuthenticated() {
        MvcTestResult response = mockMvc.get().uri(BASE_URL).exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyJson().extractingPath("$.email").asString().isEqualTo(EMAIL);
        assertThat(response).bodyJson().extractingPath("$.username").asString()
                .isEqualTo("john_doe");
    }

    @Test
    @WithMockUser(username = EMAIL)
    void shouldUpdateProfileWhenAuthenticated() {
        MvcTestResult response = mockMvc.put().uri(BASE_URL).contentType(APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Johnny",
                            "lastName": "Doer",
                            "username": "johnny_doe",
                            "password": "newpassword123",
                            "email": "johnny.doe@example.com",
                            "phone": "+251900000002"
                        }
                        """)
                .exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyJson().extractingPath("$.firstName").asString()
                .isEqualTo("Johnny");
        assertThat(response).bodyJson().extractingPath("$.username").asString()
                .isEqualTo("johnny_doe");
        assertThat(response).bodyJson().extractingPath("$.email").asString()
                .isEqualTo("johnny.doe@example.com");
    }

    @Test
    @WithMockUser(username = EMAIL)
    void shouldAddProfilePictureWhenAuthenticated() {
        String objectKey = "profile-" + UUID.randomUUID() + ".png";
        when(storageService.exists(objectKey)).thenReturn(true);
        when(storageService.getUrl(objectKey)).thenReturn("https://example.com/" + objectKey);

        MvcTestResult response = mockMvc.post().uri(BASE_URL + "/profile-picture")
                .contentType(APPLICATION_JSON)
                .content("{\"objectKey\":\"" + objectKey + "\"}")
                .exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyJson().extractingPath("$.profilePictureUrl").asString()
                .contains(objectKey);
    }

    @Test
    @WithMockUser(username = EMAIL)
    void shouldReturnBadRequestWhenProfileUpdatePayloadIsInvalid() {
        assertThat(mockMvc.put().uri(BASE_URL).contentType(APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "",
                            "lastName": "",
                            "username": "",
                            "password": "short",
                            "email": "invalid-email",
                            "phone": ""
                        }
                        """)
                .exchange()).hasStatus(400);
    }

    @Test
    @WithMockUser(username = EMAIL)
    void shouldReturnBadRequestWhenProfilePicturePayloadIsInvalid() {
        assertThat(mockMvc.post().uri(BASE_URL + "/profile-picture").contentType(APPLICATION_JSON)
                .content("{\"objectKey\":\"\"}").exchange()).hasStatus(400);
    }

    @Test
    @WithMockUser(username = EMAIL)
    void shouldReturnNotFoundWhenProfilePictureObjectDoesNotExist() {
        when(storageService.exists("missing/object-key.png")).thenReturn(false);

        assertThat(mockMvc.post().uri(BASE_URL + "/profile-picture").contentType(APPLICATION_JSON)
                .content("{\"objectKey\":\"missing/object-key.png\"}").exchange())
                .hasStatus(404);
    }
}
