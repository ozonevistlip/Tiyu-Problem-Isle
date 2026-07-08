package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String nickname;
    private String role;
    private Integer status;
}
