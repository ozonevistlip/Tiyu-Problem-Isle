package com.example.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.*;
import com.example.oj.entity.*;
import com.example.oj.mapper.*;
import com.example.oj.service.TeacherService;
import com.example.oj.utils.UserContext;
import com.example.oj.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {
    private final ClassGroupMapper classGroupMapper;
    private final ClassTypeMapper classTypeMapper;
    private final ClassMemberMapper classMemberMapper;
    private final LessonStudentTierMapper lessonStudentTierMapper;
    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final TestcaseMapper testcaseMapper;
    private final ProblemHintMapper problemHintMapper;
    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestParticipantMapper contestParticipantMapper;
    private final SubmissionMapper submissionMapper;

    @Override
    public ClassVO createClass(CreateClassRequest request) {
        UserContext.requireTeacher();
        requireActiveType(request.getClassTypeId());
        ClassGroup group = ClassGroup.builder()
                .teacherId(UserContext.userId())
                .classTypeId(request.getClassTypeId())
                .className(request.getClassName())
                .description(request.getDescription())
                .status(1)
                .build();
        classGroupMapper.insert(group);
        return toClassVO(group);
    }

    @Override
    public List<ClassVO> listClasses() {
        UserContext.requireTeacher();
        return classGroupMapper.selectList(new LambdaQueryWrapper<ClassGroup>()
                        .eq(ClassGroup::getTeacherId, UserContext.userId())
                        .orderByDesc(ClassGroup::getId))
                .stream().map(this::toClassVO).toList();
    }

    @Override
    @Transactional
    public ClassVO updateClass(Long classId, CreateClassRequest request) {
        ClassGroup group = requireOwnClass(classId);
        requireActiveType(request.getClassTypeId());
        if (!request.getClassTypeId().equals(group.getClassTypeId())) {
            lessonStudentTierMapper.delete(new LambdaQueryWrapper<LessonStudentTier>()
                    .eq(LessonStudentTier::getClassId, classId));
        }
        group.setClassTypeId(request.getClassTypeId());
        group.setClassName(request.getClassName());
        group.setDescription(request.getDescription());
        classGroupMapper.updateById(group);
        return toClassVO(group);
    }

    @Override
    @Transactional
    public void deleteClass(Long classId) {
        requireOwnClass(classId);
        lessonStudentTierMapper.delete(new LambdaQueryWrapper<LessonStudentTier>()
                .eq(LessonStudentTier::getClassId, classId));
        classMemberMapper.delete(new LambdaQueryWrapper<ClassMember>().eq(ClassMember::getClassId, classId));
        classGroupMapper.deleteById(classId);
    }

    @Override
    public void addStudent(Long classId, AddStudentRequest request) {
        requireOwnClass(classId);
        User student = findStudent(request);
        if (student == null || !"student".equals(student.getRole())
                || !UserContext.userId().equals(student.getCreatedBy())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "学生不存在");
        }
        Long exists = classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getStudentId, student.getId()));
        if (exists == 0) {
            classMemberMapper.insert(ClassMember.builder()
                    .classId(classId)
                    .studentId(student.getId())
                    .build());
        }
    }

    @Override
    public List<UserVO> listStudents(Long classId) {
        requireOwnClass(classId);
        List<Long> studentIds = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                        .eq(ClassMember::getClassId, classId))
                .stream().map(ClassMember::getStudentId).toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(studentIds).stream()
                .map(AuthServiceImpl::toUserVO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteStudent(Long classId, Long studentId) {
        requireOwnClass(classId);
        lessonStudentTierMapper.delete(new LambdaQueryWrapper<LessonStudentTier>()
                .eq(LessonStudentTier::getClassId, classId)
                .eq(LessonStudentTier::getStudentId, studentId));
        classMemberMapper.delete(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getStudentId, studentId));
    }

    @Override
    public ProblemVO createProblem(CreateProblemRequest request) {
        UserContext.requireTeacher();
        Problem problem = Problem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .inputFormat(request.getInputFormat())
                .outputFormat(request.getOutputFormat())
                .sampleInput(request.getSampleInput())
                .sampleOutput(request.getSampleOutput())
                .difficulty(defaultString(request.getDifficulty(), "easy"))
                .timeLimitMs(defaultInt(request.getTimeLimitMs(), 2000))
                .memoryLimitMb(defaultInt(request.getMemoryLimitMb(), 128))
                .compareMode(defaultString(request.getCompareMode(), "ignore_trailing_space"))
                .createdBy(UserContext.userId())
                .visibility(defaultString(request.getVisibility(), "private"))
                .status(1)
                .acceptedCount(0)
                .submitCount(0)
                .build();
        problemMapper.insert(problem);
        return toProblemVO(problem);
    }

    @Override
    public List<ProblemVO> listProblems() {
        UserContext.requireTeacher();
        return problemMapper.selectList(new LambdaQueryWrapper<Problem>()
                        .and(w -> w.eq(Problem::getCreatedBy, UserContext.userId()).or().eq(Problem::getVisibility, "public"))
                        .orderByDesc(Problem::getId))
                .stream().map(this::toProblemVO).toList();
    }

    @Override
    public ProblemVO getProblem(Long problemId) {
        return toProblemVO(requireProblemVisibleToTeacher(problemId));
    }

    @Override
    public ProblemVO updateProblem(Long problemId, CreateProblemRequest request) {
        Problem problem = requireOwnProblem(problemId);
        problem.setTitle(request.getTitle());
        problem.setDescription(request.getDescription());
        problem.setInputFormat(request.getInputFormat());
        problem.setOutputFormat(request.getOutputFormat());
        problem.setSampleInput(request.getSampleInput());
        problem.setSampleOutput(request.getSampleOutput());
        problem.setDifficulty(defaultString(request.getDifficulty(), problem.getDifficulty()));
        problem.setTimeLimitMs(defaultInt(request.getTimeLimitMs(), problem.getTimeLimitMs()));
        problem.setMemoryLimitMb(defaultInt(request.getMemoryLimitMb(), problem.getMemoryLimitMb()));
        problem.setCompareMode(defaultString(request.getCompareMode(), problem.getCompareMode()));
        problem.setVisibility(defaultString(request.getVisibility(), problem.getVisibility()));
        problemMapper.updateById(problem);
        return toProblemVO(problem);
    }

    @Override
    public void deleteProblem(Long problemId) {
        requireOwnProblem(problemId);
        problemMapper.deleteById(problemId);
    }

    @Override
    public TestcaseVO createTestcase(Long problemId, CreateTestcaseRequest request) {
        requireOwnProblem(problemId);
        Testcase testcase = Testcase.builder()
                .problemId(problemId)
                .inputData(request.getInputData())
                .outputData(request.getOutputData())
                .score(defaultInt(request.getScore(), 100))
                .sortOrder(defaultInt(request.getSortOrder(), 0))
                .sample(Boolean.TRUE.equals(request.getSample()))
                .build();
        testcaseMapper.insert(testcase);
        return toTestcaseVO(testcase);
    }

    @Override
    public List<TestcaseVO> listTestcases(Long problemId) {
        requireOwnProblem(problemId);
        return testcaseMapper.selectList(new LambdaQueryWrapper<Testcase>()
                        .eq(Testcase::getProblemId, problemId)
                        .orderByAsc(Testcase::getSortOrder))
                .stream().map(this::toTestcaseVO).toList();
    }

    @Override
    public TestcaseVO updateTestcase(Long testcaseId, CreateTestcaseRequest request) {
        Testcase testcase = requireTestcaseOnOwnProblem(testcaseId);
        testcase.setInputData(request.getInputData());
        testcase.setOutputData(request.getOutputData());
        testcase.setScore(defaultInt(request.getScore(), testcase.getScore()));
        testcase.setSortOrder(defaultInt(request.getSortOrder(), testcase.getSortOrder()));
        testcase.setSample(Boolean.TRUE.equals(request.getSample()));
        testcaseMapper.updateById(testcase);
        return toTestcaseVO(testcase);
    }

    @Override
    public void deleteTestcase(Long testcaseId) {
        requireTestcaseOnOwnProblem(testcaseId);
        testcaseMapper.deleteById(testcaseId);
    }

    @Override
    public HintVO createHint(Long problemId, CreateHintRequest request) {
        requireOwnProblem(problemId);
        ProblemHint hint = ProblemHint.builder()
                .problemId(problemId)
                .teacherId(UserContext.userId())
                .hintTitle(request.getHintTitle())
                .hintContent(request.getHintContent())
                .hintLevel(defaultInt(request.getHintLevel(), 1))
                .build();
        problemHintMapper.insert(hint);
        return toHintVO(hint);
    }

    @Override
    public List<HintVO> listHints(Long problemId) {
        requireOwnProblem(problemId);
        return problemHintMapper.selectList(new LambdaQueryWrapper<ProblemHint>()
                        .eq(ProblemHint::getProblemId, problemId)
                        .orderByAsc(ProblemHint::getHintLevel))
                .stream().map(this::toHintVO).toList();
    }

    @Override
    public HintVO updateHint(Long hintId, CreateHintRequest request) {
        ProblemHint hint = requireOwnHint(hintId);
        hint.setHintTitle(request.getHintTitle());
        hint.setHintContent(request.getHintContent());
        hint.setHintLevel(defaultInt(request.getHintLevel(), hint.getHintLevel()));
        problemHintMapper.updateById(hint);
        return toHintVO(hint);
    }

    @Override
    public void deleteHint(Long hintId) {
        requireOwnHint(hintId);
        problemHintMapper.deleteById(hintId);
    }

    @Override
    public ContestVO createContest(CreateContestRequest request) {
        requireOwnClass(request.getClassId());
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        Contest contest = Contest.builder()
                .teacherId(UserContext.userId())
                .classId(request.getClassId())
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status("DRAFT")
                .showRank(!Boolean.FALSE.equals(request.getShowRank()))
                .allowSubmitAfterEnd(Boolean.TRUE.equals(request.getAllowSubmitAfterEnd()))
                .build();
        contestMapper.insert(contest);
        return toContestVO(contest);
    }

    @Override
    public List<ContestVO> listContests() {
        UserContext.requireTeacher();
        return contestMapper.selectList(new LambdaQueryWrapper<Contest>()
                        .eq(Contest::getTeacherId, UserContext.userId())
                        .orderByDesc(Contest::getId))
                .stream().map(this::toContestVO).toList();
    }

    @Override
    public ContestVO getContest(Long contestId) {
        return toContestVO(requireOwnContest(contestId));
    }

    @Override
    public ContestVO updateContest(Long contestId, CreateContestRequest request) {
        Contest contest = requireOwnContest(contestId);
        requireOwnClass(request.getClassId());
        contest.setClassId(request.getClassId());
        contest.setTitle(request.getTitle());
        contest.setDescription(request.getDescription());
        contest.setStartTime(request.getStartTime());
        contest.setEndTime(request.getEndTime());
        contest.setShowRank(!Boolean.FALSE.equals(request.getShowRank()));
        contest.setAllowSubmitAfterEnd(Boolean.TRUE.equals(request.getAllowSubmitAfterEnd()));
        contestMapper.updateById(contest);
        return toContestVO(contest);
    }

    @Override
    public ContestProblemVO addContestProblem(Long contestId, AddContestProblemRequest request) {
        requireOwnContest(contestId);
        Problem problem = requireProblemVisibleToTeacher(request.getProblemId());
        requireProblemHasTestcases(problem.getId(), problem.getTitle());
        Long exists = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, request.getProblemId()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "题目已在比赛中");
        }
        ContestProblem contestProblem = ContestProblem.builder()
                .contestId(contestId)
                .problemId(request.getProblemId())
                .displayOrder(defaultInt(request.getDisplayOrder(), 0))
                .score(defaultInt(request.getScore(), 100))
                .hintUnlockMinutes(defaultInt(request.getHintUnlockMinutes(), 10))
                .showHintAfterAc(!Boolean.FALSE.equals(request.getShowHintAfterAc()))
                .showHintAfterContest(!Boolean.FALSE.equals(request.getShowHintAfterContest()))
                .build();
        contestProblemMapper.insert(contestProblem);
        return ContestProblemVO.builder()
                .problemId(problem.getId())
                .title(problem.getTitle())
                .displayOrder(contestProblem.getDisplayOrder())
                .score(contestProblem.getScore())
                .hintUnlockMinutes(contestProblem.getHintUnlockMinutes())
                .build();
    }

    @Override
    public void deleteContestProblem(Long contestId, Long problemId) {
        requireOwnContest(contestId);
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problemId));
    }

    @Override
    @Transactional
    public void publishContest(Long contestId) {
        Contest contest = requireOwnContest(contestId);
        List<ContestProblem> contestProblems = contestProblemMapper.selectList(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));
        if (contestProblems.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "比赛至少需要一道题");
        }
        for (ContestProblem contestProblem : contestProblems) {
            Problem problem = problemMapper.selectById(contestProblem.getProblemId());
            String title = problem == null ? String.valueOf(contestProblem.getProblemId()) : problem.getTitle();
            requireProblemHasTestcases(contestProblem.getProblemId(), title);
        }
        contestMapper.update(new LambdaUpdateWrapper<Contest>()
                .eq(Contest::getId, contestId)
                .set(Contest::getStatus, "PUBLISHED"));
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, contest.getClassId()));
        for (ClassMember member : members) {
            Long exists = contestParticipantMapper.selectCount(new LambdaQueryWrapper<ContestParticipant>()
                    .eq(ContestParticipant::getContestId, contestId)
                    .eq(ContestParticipant::getStudentId, member.getStudentId()));
            if (exists == 0) {
                contestParticipantMapper.insert(ContestParticipant.builder()
                        .contestId(contestId)
                        .studentId(member.getStudentId())
                        .totalScore(0)
                        .acceptedCount(0)
                        .submitCount(0)
                        .build());
            }
        }
    }

    @Override
    public List<RankVO> rank(Long contestId) {
        requireOwnContest(contestId);
        return buildRank(contestId);
    }

    @Override
    public List<SubmissionVO> submissions(Long contestId) {
        requireOwnContest(contestId);
        return submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getContestId, contestId)
                        .orderByDesc(Submission::getId))
                .stream().map(this::toSubmissionVO).toList();
    }

    private ClassGroup requireOwnClass(Long classId) {
        UserContext.requireTeacher();
        ClassGroup group = classGroupMapper.selectById(classId);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
        }
        if (!UserContext.userId().equals(group.getTeacherId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能管理自己的班级");
        }
        return group;
    }

    private void requireActiveType(Long typeId) {
        ClassType type = classTypeMapper.selectById(typeId);
        if (type == null || !Integer.valueOf(1).equals(type.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择超级管理员创建的班级类型");
        }
    }

    private User findStudent(AddStudentRequest request) {
        if (request.getStudentId() != null) {
            return userMapper.selectById(request.getStudentId());
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入学生账号或 ID");
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername().trim())
                .last("LIMIT 1"));
    }

    private Problem requireOwnProblem(Long problemId) {
        UserContext.requireTeacher();
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if (!UserContext.userId().equals(problem.getCreatedBy())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能管理自己创建的题目");
        }
        return problem;
    }

    private Problem requireProblemVisibleToTeacher(Long problemId) {
        UserContext.requireTeacher();
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if (!UserContext.userId().equals(problem.getCreatedBy()) && !"public".equals(problem.getVisibility())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权使用该题目");
        }
        return problem;
    }

    private Testcase requireTestcaseOnOwnProblem(Long testcaseId) {
        Testcase testcase = testcaseMapper.selectById(testcaseId);
        if (testcase == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "测试点不存在");
        }
        requireOwnProblem(testcase.getProblemId());
        return testcase;
    }

    private void requireProblemHasTestcases(Long problemId, String problemTitle) {
        Long testcaseCount = testcaseMapper.selectCount(new LambdaQueryWrapper<Testcase>()
                .eq(Testcase::getProblemId, problemId));
        if (testcaseCount == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "题目「" + problemTitle + "」未配置测试点，请先添加至少一个测试点");
        }
    }

    private ProblemHint requireOwnHint(Long hintId) {
        ProblemHint hint = problemHintMapper.selectById(hintId);
        if (hint == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "提示不存在");
        }
        requireOwnProblem(hint.getProblemId());
        return hint;
    }

    private Contest requireOwnContest(Long contestId) {
        UserContext.requireTeacher();
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "比赛不存在");
        }
        if (!UserContext.userId().equals(contest.getTeacherId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能管理自己的比赛");
        }
        return contest;
    }

    private List<RankVO> buildRank(Long contestId) {
        List<ContestParticipant> participants = contestParticipantMapper.selectList(new LambdaQueryWrapper<ContestParticipant>()
                .eq(ContestParticipant::getContestId, contestId));
        Map<Long, User> users = participants.isEmpty() ? Map.of() : userMapper.selectBatchIds(
                participants.stream().map(ContestParticipant::getStudentId).toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return participants.stream()
                .sorted(Comparator.comparing(ContestParticipant::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ContestParticipant::getLastSubmitAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(p -> RankVO.builder()
                        .studentId(p.getStudentId())
                        .studentName(users.containsKey(p.getStudentId()) ? users.get(p.getStudentId()).getRealName() : null)
                        .totalScore(p.getTotalScore())
                        .acceptedCount(p.getAcceptedCount())
                        .submitCount(p.getSubmitCount())
                        .lastSubmitAt(p.getLastSubmitAt())
                        .build())
                .toList();
    }

    private ClassVO toClassVO(ClassGroup group) {
        return ClassVO.builder()
                .id(group.getId())
                .teacherId(group.getTeacherId())
                .classTypeId(group.getClassTypeId())
                .classTypeName(group.getClassTypeId() == null ? null :
                        java.util.Optional.ofNullable(classTypeMapper.selectById(group.getClassTypeId()))
                                .map(ClassType::getName).orElse(null))
                .className(group.getClassName())
                .description(group.getDescription())
                .status(group.getStatus())
                .createdAt(group.getCreatedAt())
                .build();
    }

    private ProblemVO toProblemVO(Problem problem) {
        return ProblemVO.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .inputFormat(problem.getInputFormat())
                .outputFormat(problem.getOutputFormat())
                .sampleInput(problem.getSampleInput())
                .sampleOutput(problem.getSampleOutput())
                .difficulty(problem.getDifficulty())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .compareMode(problem.getCompareMode())
                .visibility(problem.getVisibility())
                .acceptedCount(problem.getAcceptedCount())
                .submitCount(problem.getSubmitCount())
                .build();
    }

    private TestcaseVO toTestcaseVO(Testcase testcase) {
        return TestcaseVO.builder()
                .id(testcase.getId())
                .problemId(testcase.getProblemId())
                .inputData(testcase.getInputData())
                .outputData(testcase.getOutputData())
                .score(testcase.getScore())
                .sortOrder(testcase.getSortOrder())
                .sample(testcase.getSample())
                .build();
    }

    private HintVO toHintVO(ProblemHint hint) {
        return HintVO.builder()
                .id(hint.getId())
                .problemId(hint.getProblemId())
                .hintTitle(hint.getHintTitle())
                .hintContent(hint.getHintContent())
                .hintLevel(hint.getHintLevel())
                .build();
    }

    private ContestVO toContestVO(Contest contest) {
        return ContestVO.builder()
                .id(contest.getId())
                .teacherId(contest.getTeacherId())
                .classId(contest.getClassId())
                .title(contest.getTitle())
                .description(contest.getDescription())
                .startTime(contest.getStartTime())
                .endTime(contest.getEndTime())
                .status(contest.getStatus())
                .showRank(contest.getShowRank())
                .allowSubmitAfterEnd(contest.getAllowSubmitAfterEnd())
                .build();
    }

    private SubmissionVO toSubmissionVO(Submission submission) {
        return SubmissionVO.builder()
                .id(submission.getId())
                .userId(submission.getUserId())
                .problemId(submission.getProblemId())
                .contestId(submission.getContestId())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .score(submission.getScore())
                .timeUsedMs(submission.getTimeUsedMs())
                .memoryUsedKb(submission.getMemoryUsedKb())
                .errorMessage(submission.getErrorMessage())
                .judgedAt(submission.getJudgedAt())
                .createdAt(submission.getCreatedAt())
                .build();
    }

    private static Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
