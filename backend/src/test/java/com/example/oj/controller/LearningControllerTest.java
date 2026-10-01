package com.example.oj.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.entity.ClassGroup;
import com.example.oj.entity.ClassMember;
import com.example.oj.entity.Lesson;
import com.example.oj.entity.LessonMaterial;
import com.example.oj.entity.LessonStudentTier;
import com.example.oj.entity.User;
import com.example.oj.mapper.ClassGroupMapper;
import com.example.oj.mapper.ClassMemberMapper;
import com.example.oj.mapper.ClassTypeMapper;
import com.example.oj.mapper.LessonMapper;
import com.example.oj.mapper.LessonMaterialMapper;
import com.example.oj.mapper.LessonStudentTierMapper;
import com.example.oj.mapper.UserMapper;
import com.example.oj.utils.LoginUser;
import com.example.oj.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningControllerTest {
    private final ClassTypeMapper types = mock(ClassTypeMapper.class);
    private final LessonMapper lessons = mock(LessonMapper.class);
    private final LessonMaterialMapper materials = mock(LessonMaterialMapper.class);
    private final ClassGroupMapper classes = mock(ClassGroupMapper.class);
    private final ClassMemberMapper members = mock(ClassMemberMapper.class);
    private final LessonStudentTierMapper lessonTiers = mock(LessonStudentTierMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final LearningController controller = new LearningController(types, lessons, materials, classes, members, lessonTiers, users);

    @AfterEach
    void cleanup() { UserContext.clear(); }

    @Test
    void studentCanHaveDifferentCategoriesForDifferentLessons() {
        UserContext.set(LoginUser.builder().userId(7L).role("student").build());
        ClassMember member = new ClassMember();
        member.setClassId(10L);
        member.setStudentId(7L);
        ClassGroup group = new ClassGroup();
        group.setId(10L);
        group.setClassTypeId(4L);
        group.setStatus(1);
        Lesson lesson = new Lesson();
        lesson.setId(20L);
        lesson.setClassTypeId(4L);
        lesson.setTitle("第 1 课");
        lesson.setLessonOrder(1);
        Lesson second = new Lesson();
        second.setId(21L);
        second.setClassTypeId(4L);
        second.setTitle("第 2 课");
        second.setLessonOrder(2);
        Lesson third = new Lesson();
        third.setId(22L);
        third.setClassTypeId(4L);
        third.setTitle("第 3 课");
        third.setLessonOrder(3);
        LessonMaterial basic = material(30L, "BASIC");
        LessonMaterial growth = material(31L, "GROWTH");
        LessonMaterial challenge = material(32L, "CHALLENGE");
        when(members.selectOne(any(LambdaQueryWrapper.class))).thenReturn(member);
        when(classes.selectById(10L)).thenReturn(group);
        LessonStudentTier secondChoice = assignment("CHALLENGE");
        secondChoice.setLessonId(21L);
        when(lessonTiers.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(assignment("GROWTH"), secondChoice));
        when(lessons.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson, second, third));
        when(materials.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(basic, growth), List.of(basic, challenge), List.of(basic, growth));

        var folders = controller.myLessons(10L).getData();
        assertEquals(3, folders.size());
        assertEquals("GROWTH", folders.get(0).get("tier"));
        assertEquals(31L, ((java.util.Map<?, ?>) ((List<?>) folders.get(0).get("materials")).get(0)).get("id"));
        assertEquals("CHALLENGE", folders.get(1).get("tier"));
        assertEquals(32L, ((java.util.Map<?, ?>) ((List<?>) folders.get(1).get("materials")).get(0)).get("id"));
        assertEquals(null, folders.get(2).get("tier"));
        assertEquals(0, ((List<?>) folders.get(2).get("materials")).size());
    }

    @Test
    void studentCannotOpenVideoFromAnotherCategory() {
        UserContext.set(LoginUser.builder().userId(7L).role("student").build());
        LessonMaterial video = material(30L, "BASIC");
        video.setKind("VIDEO");
        Lesson lesson = new Lesson();
        lesson.setId(20L);
        lesson.setStatus(1);
        when(materials.selectById(30L)).thenReturn(video);
        when(users.selectById(7L)).thenReturn(User.builder().id(7L).role("student").status(1).build());
        when(lessons.selectById(20L)).thenReturn(lesson);
        when(members.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(lessonTiers.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> controller.video(30L));
    }

    @Test
    void videoTicketStopsWorkingAfterCategoryChanges() {
        UserContext.set(LoginUser.builder().userId(7L).role("student").build());
        LessonMaterial video = material(30L, "GROWTH");
        video.setKind("VIDEO");
        Lesson lesson = new Lesson();
        lesson.setId(20L);
        lesson.setClassTypeId(4L);
        lesson.setStatus(1);
        ClassMember member = new ClassMember();
        member.setClassId(10L);
        ClassGroup group = new ClassGroup();
        group.setClassTypeId(4L);
        group.setStatus(1);
        when(materials.selectById(30L)).thenReturn(video);
        when(users.selectById(7L)).thenReturn(User.builder().id(7L).role("student").status(1).build());
        when(lessons.selectById(20L)).thenReturn(lesson);
        when(classes.selectById(10L)).thenReturn(group);
        when(lessonTiers.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(assignment("GROWTH")), List.of());
        when(members.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        String ticket = controller.videoTicket(30L).getData().get("ticket");
        assertThrows(BusinessException.class, () -> controller.stream(ticket));
    }

    private LessonMaterial material(Long id, String tier) {
        LessonMaterial material = new LessonMaterial();
        material.setId(id);
        material.setLessonId(20L);
        material.setTier(tier);
        material.setKind("HOMEWORK");
        material.setTitle(tier);
        return material;
    }

    private LessonStudentTier assignment(String tier) {
        LessonStudentTier assignment = new LessonStudentTier();
        assignment.setLessonId(20L);
        assignment.setClassId(10L);
        assignment.setStudentId(7L);
        assignment.setTier(tier);
        return assignment;
    }
}
