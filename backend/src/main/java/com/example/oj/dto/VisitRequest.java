package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VisitRequest {
    @NotBlank
    @Size(max = 100)
    private String visitorKey;
    @Size(max = 255)
    private String path;
}
