package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateClassRequest {
    @NotBlank
    private String className;
    @jakarta.validation.constraints.NotNull
    private Long classTypeId;
    private String description;
}
