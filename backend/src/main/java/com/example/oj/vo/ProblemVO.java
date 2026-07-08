package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemVO {
    private Long id;
    private String title;
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
    private Integer acceptedCount;
    private Integer submitCount;
}
