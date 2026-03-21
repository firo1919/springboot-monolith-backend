package com.firomsa.monolith.v1.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.firomsa.monolith.v1.dto.UploadRequestDTO;
import com.firomsa.monolith.v1.dto.UploadResponseDTO;
import com.firomsa.monolith.v1.service.StorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/uploads")
@Tag(name = "Uploads", description = "API for file uploads")
@Slf4j
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    @PostMapping("/presign")
    public UploadResponseDTO createUploadPresignTicket(
            @Valid @RequestBody UploadRequestDTO filename) {
        return storageService.createUploadPresignTicket(filename);
    }
}
