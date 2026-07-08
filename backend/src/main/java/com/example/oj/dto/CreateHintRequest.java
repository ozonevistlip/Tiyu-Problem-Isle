package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateHintRequest {
    private String hintTitle;
    @NotBlank
    private String hintContent;
    private Integer hintLevel;
}
