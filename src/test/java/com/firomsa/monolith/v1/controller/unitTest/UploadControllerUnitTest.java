package com.firomsa.monolith.v1.controller.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.firomsa.monolith.v1.controller.UploadController;
import com.firomsa.monolith.v1.dto.UploadRequestDTO;
import com.firomsa.monolith.v1.dto.UploadResponseDTO;
import com.firomsa.monolith.v1.service.StorageService;

@WebMvcTest(UploadController.class)
@AutoConfigureMockMvc
@WithMockUser
public class UploadControllerUnitTest {

    @MockitoBean
    private StorageService storageService;

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void shouldCreateUploadPresignTicket() {
        UploadResponseDTO response =
                new UploadResponseDTO("12345_file.jpg", "https://signed.example.com/upload", "10");
        when(storageService.createUploadPresignTicket(any(UploadRequestDTO.class)))
                .thenReturn(response);

        MvcTestResult result = mockMvc.post().uri("/api/v1/uploads/presign").with(csrf())
                .contentType(APPLICATION_JSON).content("""
                            {
                                "filename": "file.jpg",
                                "contentType": "image/jpeg"
                            }
                        """).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.objectKey").asString()
                .isEqualTo("12345_file.jpg");
        assertThat(result).bodyJson().extractingPath("$.uploadUrl").asString()
                .isEqualTo("https://signed.example.com/upload");
        assertThat(result).bodyJson().extractingPath("$.expiresIn").asString().isEqualTo("10");

        verify(storageService).createUploadPresignTicket(any(UploadRequestDTO.class));
    }
}
