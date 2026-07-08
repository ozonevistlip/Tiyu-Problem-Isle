package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProblemRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String sampleInput;
    private String sampleOutput;
    private String difficulty;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private String compareMode;
    private String visibility;
}
