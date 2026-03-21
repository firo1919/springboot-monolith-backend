package com.firomsa.monolith.v1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.firomsa.monolith.config.S3Config;
import com.firomsa.monolith.v1.dto.UploadRequestDTO;
import com.firomsa.monolith.v1.dto.UploadResponseDTO;
import io.awspring.cloud.s3.S3Template;

@ExtendWith(MockitoExtension.class)
class StorageServiceUnitTest {

    @Mock
    private S3Template s3Template;
    @Mock
    private S3Config s3Config;
    @InjectMocks
    private StorageService storageService;

    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        lenient().when(s3Config.getBucketName()).thenReturn(bucketName);
    }

    @Test
    @DisplayName("Should create presigned upload ticket")
    void createUploadPresignTicket_ShouldReturnTicket()
            throws MalformedURLException, URISyntaxException {
        // Arrange
        UploadRequestDTO requestDTO = new UploadRequestDTO();
        requestDTO.setFilename("test.jpg");
        requestDTO.setContentType("image/jpeg");

        when(s3Config.getUploadLinkExpiryMinutes()).thenReturn(15);
        URL mockUrl = new URI("http://example.com/upload").toURL();
        when(s3Template.createSignedPutURL(eq(bucketName), anyString(), any(Duration.class),
                isNull(), eq("image/jpeg"))).thenReturn(mockUrl);

        // Act
        UploadResponseDTO response = storageService.createUploadPresignTicket(requestDTO);

        // Assert
        assertNotNull(response);
        assertTrue(response.objectKey().endsWith("_test.jpg"));
        assertEquals("http://example.com/upload", response.uploadUrl());
        assertEquals("15", response.expiresIn());
        verify(s3Template, times(1)).createSignedPutURL(eq(bucketName), anyString(),
                any(Duration.class), isNull(), eq("image/jpeg"));
    }

    @Test
    @DisplayName("Should check if object exists")
    void exists_ShouldReturnBoolean() {
        // Arrange
        String key = "test.jpg";
        when(s3Template.objectExists(bucketName, key)).thenReturn(true);

        // Act
        boolean result = storageService.exists(key);

        // Assert
        assertTrue(result);
        verify(s3Template, times(1)).objectExists(bucketName, key);
    }

    @Test
    @DisplayName("Should get presigned URL")
    void getUrl_ShouldReturnUrlString() throws MalformedURLException, URISyntaxException {
        // Arrange
        String key = "test.jpg";
        when(s3Config.getGetLinkExpiryMinutes()).thenReturn(60);
        URL mockUrl = new URI("http://example.com/get").toURL();
        when(s3Template.createSignedGetURL(eq(bucketName), eq(key), any(Duration.class)))
                .thenReturn(mockUrl);

        // Act
        String result = storageService.getUrl(key);

        // Assert
        assertNotNull(result);
        assertEquals("http://example.com/get", result);
        verify(s3Template, times(1)).createSignedGetURL(eq(bucketName), eq(key),
                any(Duration.class));
    }
}
