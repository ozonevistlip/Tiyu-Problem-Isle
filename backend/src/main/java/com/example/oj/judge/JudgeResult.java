package com.example.oj.judge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResult {
    private String status;
    private Integer score;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;
    private String errorMessage;
    @Builder.Default
    private List<JudgeCaseResult> cases = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JudgeCaseResult {
        private Long testcaseId;
        private String status;
        private Integer score;
        private Integer timeUsedMs;
        private Integer memoryUsedKb;
        private String inputPreview;
        private String expectedOutputPreview;
        private String actualOutputPreview;
        private String errorMessage;
    }
}
