package com.firomsa.monolith.v1.controller.integrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.firomsa.monolith.v1.dto.UploadResponseDTO;

public class UploadControllerIntTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mockMvc;

    private static final String BASE_URL = "/api/v1/uploads";

    @Test
    void shouldRejectUnauthorizedCreateUploadPresignTicket() {
        assertThat(mockMvc.post().uri(BASE_URL + "/presign").contentType(APPLICATION_JSON)
                .content("{}").exchange()).hasStatus(401);
    }

    @Test
    @WithMockUser
    void shouldCreateUploadPresignTicketWhenAuthenticated() {
        when(storageService.createUploadPresignTicket(any())).thenReturn(
                new UploadResponseDTO("key-1", "https://example.com/upload/key-1", "15"));

        MvcTestResult response = mockMvc.post().uri(BASE_URL + "/presign")
                .contentType(APPLICATION_JSON)
                .content("{\"filename\":\"avatar.png\",\"contentType\":\"image/png\"}").exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyJson().extractingPath("$.objectKey").asString().isEqualTo("key-1");
        assertThat(response).bodyJson().extractingPath("$.uploadUrl").asString()
                .isEqualTo("https://example.com/upload/key-1");
        assertThat(response).bodyJson().extractingPath("$.expiresIn").asString().isEqualTo("15");
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenUploadPayloadIsInvalid() {
        assertThat(mockMvc.post().uri(BASE_URL + "/presign").contentType(APPLICATION_JSON)
                .content("{\"filename\":\"\",\"contentType\":\"\"}").exchange()).hasStatus(400);
    }

    @Test
    void shouldReturnUnauthorizedWhenBearerTokenIsInvalid() {
        assertThat(mockMvc.post().uri(BASE_URL + "/presign")
                .header("Authorization", "Bearer invalid.token.value").contentType(APPLICATION_JSON)
                .content("{\"filename\":\"avatar.png\",\"contentType\":\"image/png\"}").exchange())
                        .hasStatus(401);
    }

    @Test
    @WithMockUser
    void shouldGenerateDifferentObjectKeysForSameFilename() {
        when(storageService.createUploadPresignTicket(any()))
                .thenReturn(
                        new UploadResponseDTO("key-1", "https://example.com/upload/key-1", "15"))
                .thenReturn(
                        new UploadResponseDTO("key-2", "https://example.com/upload/key-2", "15"));

        MvcTestResult firstResponse = mockMvc.post().uri(BASE_URL + "/presign")
                .contentType(APPLICATION_JSON)
                .content("{\"filename\":\"avatar.png\",\"contentType\":\"image/png\"}").exchange();
        MvcTestResult secondResponse = mockMvc.post().uri(BASE_URL + "/presign")
                .contentType(APPLICATION_JSON)
                .content("{\"filename\":\"avatar.png\",\"contentType\":\"image/png\"}").exchange();

        assertThat(firstResponse).hasStatusOk();
        assertThat(secondResponse).hasStatusOk();
        assertThat(firstResponse).bodyText().contains("\"objectKey\":\"key-1\"");
        assertThat(secondResponse).bodyText().contains("\"objectKey\":\"key-2\"");
    }
}
