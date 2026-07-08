package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HintVO {
    private Long id;
    private Long problemId;
    private String hintTitle;
    private String hintContent;
    private Integer hintLevel;
}
