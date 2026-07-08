package com.example.oj.service;

import com.example.oj.dto.SubmitCodeRequest;
import com.example.oj.vo.*;

import java.util.List;

public interface StudentService {
    List<ContestVO> listContests();

    ContestVO getContest(Long contestId);

    List<ContestProblemVO> listProblems(Long contestId);

    ContestProblemVO getProblem(Long contestId, Long problemId);

    HintView getContestProblemHint(Long contestId, Long problemId, Long studentId);

    SubmissionVO submit(Long contestId, Long problemId, SubmitCodeRequest request);

    List<SubmissionVO> submissions(Long contestId);

    List<RankVO> rank(Long contestId);
}
