package com.example.oj.service;

import com.example.oj.dto.*;
import com.example.oj.vo.*;

import java.util.List;

public interface TeacherService {
    ClassVO createClass(CreateClassRequest request);

    List<ClassVO> listClasses();

    ClassVO updateClass(Long classId, CreateClassRequest request);

    void deleteClass(Long classId);

    void addStudent(Long classId, AddStudentRequest request);

    List<UserVO> listStudents(Long classId);

    void deleteStudent(Long classId, Long studentId);

    ProblemVO createProblem(CreateProblemRequest request);

    List<ProblemVO> listProblems();

    ProblemVO getProblem(Long problemId);

    ProblemVO updateProblem(Long problemId, CreateProblemRequest request);

    void deleteProblem(Long problemId);

    TestcaseVO createTestcase(Long problemId, CreateTestcaseRequest request);

    List<TestcaseVO> listTestcases(Long problemId);

    TestcaseVO updateTestcase(Long testcaseId, CreateTestcaseRequest request);

    void deleteTestcase(Long testcaseId);

    HintVO createHint(Long problemId, CreateHintRequest request);

    List<HintVO> listHints(Long problemId);

    HintVO updateHint(Long hintId, CreateHintRequest request);

    void deleteHint(Long hintId);

    ContestVO createContest(CreateContestRequest request);

    List<ContestVO> listContests();

    ContestVO getContest(Long contestId);

    ContestVO updateContest(Long contestId, CreateContestRequest request);

    ContestProblemVO addContestProblem(Long contestId, AddContestProblemRequest request);

    void deleteContestProblem(Long contestId, Long problemId);

    void publishContest(Long contestId);

    List<RankVO> rank(Long contestId);

    List<SubmissionVO> submissions(Long contestId);
}
