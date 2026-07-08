package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionCaseVO {
    private Long id;
    private Long testcaseId;
    private String status;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;
    private String inputPreview;
    private String expectedOutputPreview;
    private String actualOutputPreview;
    private String errorMessage;
}
