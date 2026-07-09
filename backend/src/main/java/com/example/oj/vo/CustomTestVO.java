package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomTestVO {
    private String status;
    private String stdout;
    private String stderr;
    private Integer timeUsedMs;
    private String errorMessage;
}
