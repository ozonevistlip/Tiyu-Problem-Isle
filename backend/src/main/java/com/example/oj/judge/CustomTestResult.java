package com.example.oj.judge;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomTestResult {
    private String status;
    private String stdout;
    private String stderr;
    private Integer timeUsedMs;
    private String errorMessage;
}
