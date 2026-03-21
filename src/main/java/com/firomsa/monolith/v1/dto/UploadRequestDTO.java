package com.firomsa.monolith.v1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequestDTO {

    @NotBlank
    private String filename;

    @NotBlank
    private String contentType;
}
