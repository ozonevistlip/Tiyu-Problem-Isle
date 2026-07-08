package com.example.oj.service;

import com.example.oj.vo.SubmissionCaseVO;
import com.example.oj.vo.SubmissionVO;

import java.util.List;

public interface SubmissionService {
    SubmissionVO getSubmission(Long submissionId);

    List<SubmissionCaseVO> listCases(Long submissionId);
}
