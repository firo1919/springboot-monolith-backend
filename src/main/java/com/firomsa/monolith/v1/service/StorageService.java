package com.firomsa.monolith.v1.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.firomsa.monolith.config.S3Config;
import com.firomsa.monolith.v1.dto.UploadRequestDTO;
import com.firomsa.monolith.v1.dto.UploadResponseDTO;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Template s3Template;
    private final S3Config s3Config;

    public UploadResponseDTO createUploadPresignTicket(UploadRequestDTO file) {
        log.info("Generating presigned upload URL for file");
        String key = System.currentTimeMillis() + "_" + file.getFilename();
        return new UploadResponseDTO(key,
                s3Template.createSignedPutURL(s3Config.getBucketName(), key,
                        Duration.ofMinutes(s3Config.getUploadLinkExpiryMinutes()), null,
                        file.getContentType()).toString(),
                String.valueOf(s3Config.getUploadLinkExpiryMinutes()));
    }

    public boolean exists(String key) {
        return s3Template.objectExists(s3Config.getBucketName(), key);
    }

    public String getUrl(String key) {
        return s3Template.createSignedGetURL(s3Config.getBucketName(), key,
                Duration.ofMinutes(s3Config.getGetLinkExpiryMinutes())).toString();
    }
}
