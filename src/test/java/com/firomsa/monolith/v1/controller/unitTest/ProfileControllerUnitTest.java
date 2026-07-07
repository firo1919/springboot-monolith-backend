package com.firomsa.monolith.v1.controller.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.firomsa.monolith.model.Roles;
import com.firomsa.monolith.support.TestCacheConfig;
import com.firomsa.monolith.v1.controller.ProfileController;
import com.firomsa.monolith.v1.dto.ProfileUpdateDTO;
import com.firomsa.monolith.v1.dto.UserResponseDTO;
import com.firomsa.monolith.v1.service.UserService;

@WebMvcTest(ProfileController.class)
@Import(TestCacheConfig.class)
@AutoConfigureMockMvc
@WithMockUser(username = "employee.one@example.com")
public class ProfileControllerUnitTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvcTester mockMvc;

    private static final String BASE_URL = "/api/v1/profile";

    private UserResponseDTO sampleUser(UUID id, String username) {
        return new UserResponseDTO(id, "John", "Doe", username, username, "+251911111111",
                Roles.EMPLOYEE.name(), "https://cdn.example.com/profile.jpg", "2026-03-19T10:15:30",
                true, true);
    }

    @Test
    void shouldGetProfile() {
        String email = "employee.one@example.com";
        UserResponseDTO response = sampleUser(UUID.randomUUID(), email);
        when(userService.getProfile(email)).thenReturn(response);

        MvcTestResult result = mockMvc.get().uri(BASE_URL).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.email").asString().isEqualTo(email);
        assertThat(result).bodyJson().extractingPath("$.role").asString().isEqualTo("EMPLOYEE");
        verify(userService).getProfile(email);
    }

    @Test
    void shouldUpdateProfile() {
        String email = "employee.one@example.com";
        UserResponseDTO response = sampleUser(UUID.randomUUID(), email);
        when(userService.updateProfile(eq(email), any(ProfileUpdateDTO.class)))
                .thenReturn(response);

        MvcTestResult result = mockMvc.put().uri(BASE_URL).with(csrf()).contentType(APPLICATION_JSON).content("""
                {
                    "firstName": "Updated",
                    "lastName": "User",
                    "username": "updated.user",
                    "password": "password123",
                    "email": "employee.one@example.com",
                    "phone": "+251933333333"
                }
                    """).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.email").asString().isEqualTo(email);

        verify(userService).updateProfile(eq(email), any(ProfileUpdateDTO.class));
    }

    @Test
    void shouldAddProfilePicture() {
        String email = "employee.one@example.com";
        String objectKey = "profiles/employee-one.jpg";
        UserResponseDTO response = sampleUser(UUID.randomUUID(), email);
        when(userService.addProfilePicture(email, objectKey)).thenReturn(response);

        MvcTestResult result = mockMvc.post().uri(BASE_URL + "/profile-picture").with(csrf())
                .contentType(APPLICATION_JSON).content("{" + "\"objectKey\":\"" + objectKey + "\"}")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.email").asString().isEqualTo(email);

        verify(userService).addProfilePicture(email, objectKey);
    }
}
