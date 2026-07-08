package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubmissionVO {
    private Long id;
    private Long userId;
    private Long problemId;
    private Long contestId;
    private String language;
    private String status;
    private Integer score;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;
    private String errorMessage;
    private LocalDateTime judgedAt;
    private LocalDateTime createdAt;
}
