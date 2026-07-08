package com.example.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.entity.Submission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SubmissionMapper extends BaseMapper<Submission> {
    @Update("UPDATE submission SET status = 'JUDGING', updated_at = NOW() WHERE id = #{id} AND status = 'PENDING'")
    int markJudgingIfPending(@Param("id") Long id);
}
