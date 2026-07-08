package com.example.oj.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddContestProblemRequest {
    @NotNull
    private Long problemId;
    private Integer displayOrder;
    private Integer score;
    private Integer hintUnlockMinutes;
    private Boolean showHintAfterAc;
    private Boolean showHintAfterContest;
}
