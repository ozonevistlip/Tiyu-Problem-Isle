package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.AddStudentRequest;
import com.example.oj.dto.CreateClassRequest;
import com.example.oj.service.TeacherService;
import com.example.oj.vo.ClassVO;
import com.example.oj.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/classes")
public class TeacherClassController {
    private final TeacherService teacherService;

    @PostMapping
    public Result<ClassVO> create(@Valid @RequestBody CreateClassRequest request) {
        return Result.success(teacherService.createClass(request));
    }

    @GetMapping
    public Result<List<ClassVO>> list() {
        return Result.success(teacherService.listClasses());
    }

    @PutMapping("/{classId}")
    public Result<ClassVO> update(@PathVariable Long classId, @Valid @RequestBody CreateClassRequest request) {
        return Result.success(teacherService.updateClass(classId, request));
    }

    @DeleteMapping("/{classId}")
    public Result<Void> delete(@PathVariable Long classId) {
        teacherService.deleteClass(classId);
        return Result.success(null);
    }

    @PostMapping("/{classId}/students")
    public Result<Void> addStudent(@PathVariable Long classId, @Valid @RequestBody AddStudentRequest request) {
        teacherService.addStudent(classId, request);
        return Result.success(null);
    }

    @GetMapping("/{classId}/students")
    public Result<List<UserVO>> listStudents(@PathVariable Long classId) {
        return Result.success(teacherService.listStudents(classId));
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    public Result<Void> deleteStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        teacherService.deleteStudent(classId, studentId);
        return Result.success(null);
    }
}
