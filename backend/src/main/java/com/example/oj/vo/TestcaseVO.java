package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestcaseVO {
    private Long id;
    private Long problemId;
    private String inputData;
    private String outputData;
    private Integer score;
    private Integer sortOrder;
    private Boolean sample;
}
