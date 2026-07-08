package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContestProblemVO {
    private Long problemId;
    private String title;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String sampleInput;
    private String sampleOutput;
    private String difficulty;
    private Integer displayOrder;
    private Integer score;
    private Integer hintUnlockMinutes;
    private String status;
    private Integer bestScore;
    private Integer submitCount;
    private Boolean canShowHint;
    private Long hintUnlockRemainSeconds;
}
