package com.example.oj.controller;

import com.example.oj.common.BusinessException;
import com.example.oj.entity.User;
import com.example.oj.mapper.ClassGroupMapper;
import com.example.oj.mapper.ClassMemberMapper;
import com.example.oj.mapper.LessonMapper;
import com.example.oj.mapper.LessonStudentTierMapper;
import com.example.oj.mapper.UserMapper;
import com.example.oj.mapper.UserSessionMapper;
import com.example.oj.utils.LoginUser;
import com.example.oj.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeacherStudentControllerTest {
    private final UserMapper users = mock(UserMapper.class);
    private final UserSessionMapper sessions = mock(UserSessionMapper.class);
    private final ClassGroupMapper classes = mock(ClassGroupMapper.class);
    private final ClassMemberMapper members = mock(ClassMemberMapper.class);
    private final LessonMapper lessons = mock(LessonMapper.class);
    private final LessonStudentTierMapper lessonTiers = mock(LessonStudentTierMapper.class);
    private final TeacherStudentController controller = new TeacherStudentController(users, sessions, classes, members, lessons, lessonTiers);

    @AfterEach
    void cleanup() { UserContext.clear(); }

    @Test
    void teacherCannotResetAnotherTeachersStudentPassword() {
        UserContext.set(LoginUser.builder().userId(10L).role("teacher").build());
        User student = User.builder().id(30L).role("student").createdBy(11L).build();
        when(users.selectById(30L)).thenReturn(student);
        TeacherStudentController.PasswordRequest request = new TeacherStudentController.PasswordRequest();
        request.password = "newpassword";

        assertThrows(BusinessException.class, () -> controller.resetPassword(30L, request));
        verifyNoInteractions(sessions, classes, members, lessons, lessonTiers);
    }
}
