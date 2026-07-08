package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitCodeRequest {
    @NotBlank
    private String language;
    @NotBlank
    private String code;
}
