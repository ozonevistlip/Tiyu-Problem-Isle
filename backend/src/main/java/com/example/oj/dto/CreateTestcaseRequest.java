package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTestcaseRequest {
    @NotBlank
    private String inputData;
    @NotBlank
    private String outputData;
    private Integer score;
    private Integer sortOrder;
    private Boolean sample;
}
