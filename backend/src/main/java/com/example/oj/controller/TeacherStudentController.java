package com.example.oj.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.common.Result;
import com.example.oj.entity.ClassGroup;
import com.example.oj.entity.ClassMember;
import com.example.oj.entity.Lesson;
import com.example.oj.entity.LessonStudentTier;
import com.example.oj.entity.PetGrant;
import com.example.oj.entity.User;
import com.example.oj.entity.UserSession;
import com.example.oj.mapper.ClassGroupMapper;
import com.example.oj.mapper.ClassMemberMapper;
import com.example.oj.mapper.LessonMapper;
import com.example.oj.mapper.LessonStudentTierMapper;
import com.example.oj.mapper.PetGrantMapper;
import com.example.oj.mapper.UserMapper;
import com.example.oj.mapper.UserSessionMapper;
import com.example.oj.service.impl.AuthServiceImpl;
import com.example.oj.utils.PasswordUtils;
import com.example.oj.utils.UserContext;
import com.example.oj.vo.UserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/students")
public class TeacherStudentController {
    private final UserMapper userMapper;
    private final UserSessionMapper sessionMapper;
    private final ClassGroupMapper classMapper;
    private final ClassMemberMapper memberMapper;
    private final LessonMapper lessonMapper;
    private final LessonStudentTierMapper lessonTierMapper;
    private final PetGrantMapper petGrantMapper;

    @GetMapping
    public Result<List<UserVO>> list() {
        UserContext.requireTeacher();
        return Result.success(userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "student").eq(User::getCreatedBy, UserContext.userId())
                .orderByDesc(User::getId)).stream().map(AuthServiceImpl::toUserVO).toList());
    }

    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody CreateStudentRequest request) {
        UserContext.requireTeacher();
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.username.trim())) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已存在");
        }
        User student = User.builder().username(request.username.trim())
                .passwordHash(PasswordUtils.hash(request.password))
                .realName(request.realName).nickname(request.nickname)
                .role("student").createdBy(UserContext.userId()).status(1).build();
        userMapper.insert(student);
        return Result.success(AuthServiceImpl.toUserVO(student));
    }

    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request) {
        User student = requireOwnStudent(id);
        User duplicate = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username.trim()).last("LIMIT 1"));
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已存在");
        }
        student.setUsername(request.username.trim());
        student.setRealName(request.realName);
        student.setNickname(request.nickname);
        userMapper.updateById(student);
        return Result.success(AuthServiceImpl.toUserVO(student));
    }

    @PutMapping("/{id}/password")
    @Transactional
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordRequest request) {
        User student = requireOwnStudent(id);
        student.setPasswordHash(PasswordUtils.hash(request.password));
        userMapper.updateById(student);
        sessionMapper.delete(new LambdaQueryWrapper<UserSession>().eq(UserSession::getUserId, id));
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        requireOwnStudent(id);
        lessonTierMapper.delete(new LambdaQueryWrapper<LessonStudentTier>()
                .eq(LessonStudentTier::getStudentId, id));
        petGrantMapper.delete(new LambdaQueryWrapper<PetGrant>().eq(PetGrant::getStudentId, id));
        memberMapper.delete(new LambdaQueryWrapper<ClassMember>().eq(ClassMember::getStudentId, id));
        sessionMapper.delete(new LambdaQueryWrapper<UserSession>().eq(UserSession::getUserId, id));
        userMapper.deleteById(id);
        return Result.success(null);
    }

    @PutMapping("/classes/{classId}/lessons/{lessonId}/students/{studentId}/tier")
    public Result<Void> setTier(@PathVariable Long classId, @PathVariable Long lessonId,
                                 @PathVariable Long studentId, @Valid @RequestBody TierRequest request) {
        UserContext.requireTeacher();
        ClassGroup group = classMapper.selectById(classId);
        if (group == null || !UserContext.userId().equals(group.getTeacherId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null || !lesson.getClassTypeId().equals(group.getClassTypeId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "课次不属于该班级类型");
        }
        ClassMember member = memberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId).eq(ClassMember::getStudentId, studentId));
        if (member == null) throw new BusinessException(ErrorCode.NOT_FOUND, "学生不在该班级");
        if (!List.of("BASIC", "GROWTH", "CHALLENGE").contains(request.tier)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "学习类别无效");
        }
        LessonStudentTier assignment = lessonTierMapper.selectOne(new LambdaQueryWrapper<LessonStudentTier>()
                .eq(LessonStudentTier::getClassId, classId)
                .eq(LessonStudentTier::getLessonId, lessonId)
                .eq(LessonStudentTier::getStudentId, studentId));
        if (assignment == null) {
            assignment = new LessonStudentTier();
            assignment.setClassId(classId);
            assignment.setLessonId(lessonId);
            assignment.setStudentId(studentId);
            assignment.setTier(request.tier);
            lessonTierMapper.insert(assignment);
        } else {
            assignment.setTier(request.tier);
            lessonTierMapper.updateById(assignment);
        }
        return Result.success(null);
    }

    @GetMapping("/classes/{classId}/lesson-tiers")
    public Result<List<LessonStudentTier>> tiers(@PathVariable Long classId) {
        UserContext.requireTeacher();
        ClassGroup group = classMapper.selectById(classId);
        if (group == null || !UserContext.userId().equals(group.getTeacherId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return Result.success(lessonTierMapper.selectList(new LambdaQueryWrapper<LessonStudentTier>()
                .eq(LessonStudentTier::getClassId, classId)));
    }

    private User requireOwnStudent(Long id) {
        UserContext.requireTeacher();
        User student = userMapper.selectById(id);
        if (student == null || !"student".equals(student.getRole())
                || !UserContext.userId().equals(student.getCreatedBy())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能管理自己创建的学生账号");
        }
        return student;
    }

    @Data public static class CreateStudentRequest {
        @NotBlank @Size(min = 3, max = 30) public String username;
        @NotBlank @Size(min = 6, max = 64) public String password;
        public String realName;
        public String nickname;
    }
    @Data public static class UpdateStudentRequest {
        @NotBlank @Size(min = 3, max = 30) public String username;
        public String realName;
        public String nickname;
    }
    @Data public static class PasswordRequest {
        @NotBlank @Size(min = 6, max = 64) public String password;
    }
    @Data public static class TierRequest {
        @NotBlank public String tier;
    }
}
