package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HintView {
    private Boolean canShowHint;
    private Long unlockRemainSeconds;
    private String hintTitle;
    private String hintContent;
    private Integer hintLevel;
    private Boolean hintShown;
}
