package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pet_grant")
public class PetGrant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long petId;
    private Long grantedBy;
}
