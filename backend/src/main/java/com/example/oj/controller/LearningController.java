package com.example.oj.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.common.Result;
import com.example.oj.entity.*;
import com.example.oj.mapper.*;
import com.example.oj.utils.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning")
public class LearningController {
    private final ClassTypeMapper typeMapper;
    private final LessonMapper lessonMapper;
    private final LessonMaterialMapper materialMapper;
    private final ClassGroupMapper classMapper;
    private final ClassMemberMapper memberMapper;
    private final LessonStudentTierMapper lessonTierMapper;
    private final UserMapper userMapper;

    @Value("${oj.video-dir:./teaching-videos}")
    private String videoDir;
    private final Map<String, VideoTicket> videoTickets = new ConcurrentHashMap<>();

    private record VideoTicket(Long materialId, Long studentId, Instant expiresAt) {}

    @GetMapping("/types")
    public Result<List<ClassType>> types() {
        if (!"teacher".equals(UserContext.role()) && !"SUPER_ADMIN".equals(UserContext.role())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return Result.success(typeMapper.selectList(new LambdaQueryWrapper<ClassType>()
                .eq(ClassType::getStatus, 1).orderByAsc(ClassType::getId)));
    }

    @PostMapping("/types")
    public Result<ClassType> createType(@Valid @RequestBody TypeRequest request) {
        UserContext.requireSuperAdmin();
        if (typeMapper.selectCount(new LambdaQueryWrapper<ClassType>().eq(ClassType::getName, request.name.trim())) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "班级类型已存在");
        }
        ClassType type = new ClassType();
        type.setName(request.name.trim());
        type.setDescription(request.description);
        type.setStatus(1);
        typeMapper.insert(type);
        return Result.success(type);
    }

    @PutMapping("/types/{id}")
    public Result<ClassType> updateType(@PathVariable Long id, @Valid @RequestBody TypeRequest request) {
        UserContext.requireSuperAdmin();
        ClassType type = requireType(id);
        type.setName(request.name.trim());
        type.setDescription(request.description);
        typeMapper.updateById(type);
        return Result.success(type);
    }

    @DeleteMapping("/types/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        UserContext.requireSuperAdmin();
        ClassType type = requireType(id);
        if (classMapper.selectCount(new LambdaQueryWrapper<ClassGroup>().eq(ClassGroup::getClassTypeId, id)) > 0
                || lessonMapper.selectCount(new LambdaQueryWrapper<Lesson>().eq(Lesson::getClassTypeId, id)) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该类型已有班级或课次，不能删除");
        }
        typeMapper.deleteById(type.getId());
        return Result.success(null);
    }

    @GetMapping("/types/{typeId}/lessons")
    public Result<List<Lesson>> lessons(@PathVariable Long typeId) {
        if (!"teacher".equals(UserContext.role()) && !"SUPER_ADMIN".equals(UserContext.role())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        requireType(typeId);
        return Result.success(lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getClassTypeId, typeId).orderByAsc(Lesson::getLessonOrder)));
    }

    @PostMapping("/types/{typeId}/lessons")
    public Result<Lesson> createLesson(@PathVariable Long typeId, @Valid @RequestBody LessonRequest request) {
        UserContext.requireSuperAdmin();
        requireType(typeId);
        Lesson lesson = new Lesson();
        lesson.setClassTypeId(typeId);
        lesson.setTitle(request.title.trim());
        lesson.setLessonOrder(request.lessonOrder);
        lesson.setStatus(request.status == null ? 0 : request.status);
        lessonMapper.insert(lesson);
        return Result.success(lesson);
    }

    @PutMapping("/lessons/{id}")
    public Result<Lesson> updateLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        UserContext.requireSuperAdmin();
        Lesson lesson = requireLesson(id);
        lesson.setTitle(request.title.trim());
        lesson.setLessonOrder(request.lessonOrder);
        lesson.setStatus(request.status == null ? 0 : request.status);
        lessonMapper.updateById(lesson);
        return Result.success(lesson);
    }

    @DeleteMapping("/lessons/{id}")
    @Transactional
    public Result<Void> deleteLesson(@PathVariable Long id) {
        UserContext.requireSuperAdmin();
        requireLesson(id);
        lessonTierMapper.delete(new LambdaQueryWrapper<LessonStudentTier>()
                .eq(LessonStudentTier::getLessonId, id));
        for (LessonMaterial material : materialsFor(id)) deleteMaterialFile(material);
        materialMapper.delete(new LambdaQueryWrapper<LessonMaterial>().eq(LessonMaterial::getLessonId, id));
        lessonMapper.deleteById(id);
        return Result.success(null);
    }

    @GetMapping("/lessons/{id}/materials")
    public Result<List<LessonMaterial>> materials(@PathVariable Long id) {
        UserContext.requireSuperAdmin();
        requireLesson(id);
        return Result.success(materialsFor(id));
    }

    @PostMapping("/lessons/{id}/homework")
    public Result<LessonMaterial> createHomework(@PathVariable Long id, @Valid @RequestBody HomeworkRequest request) {
        UserContext.requireSuperAdmin();
        requireLesson(id);
        checkTier(request.tier);
        LessonMaterial material = new LessonMaterial();
        material.setLessonId(id);
        material.setTier(request.tier);
        material.setKind("HOMEWORK");
        material.setTitle(request.title.trim());
        material.setContent(request.content);
        materialMapper.insert(material);
        return Result.success(material);
    }

    @PostMapping(value = "/lessons/{id}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<LessonMaterial> uploadVideo(@PathVariable Long id, @RequestParam String tier,
                                               @RequestParam String title, @RequestPart MultipartFile file) throws IOException {
        UserContext.requireSuperAdmin();
        requireLesson(id);
        checkTier(tier);
        if (title.isBlank() || file.isEmpty() || file.getSize() > 500L * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标题不能为空，视频大小不能超过 500MB");
        }
        String mime = file.getContentType();
        if (mime == null || !mime.startsWith("video/")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只能上传视频文件");
        }
        Path directory = Path.of(videoDir).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String key = UUID.randomUUID().toString();
        Path target = directory.resolve(key);
        file.transferTo(target);
        LessonMaterial material = new LessonMaterial();
        material.setLessonId(id);
        material.setTier(tier);
        material.setKind("VIDEO");
        material.setTitle(title.trim());
        material.setFilePath(key);
        material.setFileName(file.getOriginalFilename());
        material.setMimeType(mime);
        try {
            materialMapper.insert(material);
        } catch (RuntimeException ex) {
            Files.deleteIfExists(target);
            throw ex;
        }
        return Result.success(material);
    }

    @DeleteMapping("/materials/{id}")
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        UserContext.requireSuperAdmin();
        LessonMaterial material = requireMaterial(id);
        materialMapper.deleteById(id);
        deleteMaterialFile(material);
        return Result.success(null);
    }

    @GetMapping("/my-classes")
    public Result<List<Map<String, Object>>> myClasses() {
        UserContext.requireStudent();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassMember member : memberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getStudentId, UserContext.userId()))) {
            ClassGroup group = classMapper.selectById(member.getClassId());
            if (group == null || group.getClassTypeId() == null || !Integer.valueOf(1).equals(group.getStatus())) continue;
            ClassType type = typeMapper.selectById(group.getClassTypeId());
            if (type == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("classId", group.getId());
            item.put("className", group.getClassName());
            item.put("classTypeName", type.getName());
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/my-classes/{classId}/lessons")
    public Result<List<Map<String, Object>>> myLessons(@PathVariable Long classId) {
        UserContext.requireStudent();
        requireMembership(classId);
        ClassGroup group = classMapper.selectById(classId);
        if (group == null || group.getClassTypeId() == null || !Integer.valueOf(1).equals(group.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Long, String> assignedTiers = new HashMap<>();
        for (LessonStudentTier assignment : lessonTierMapper.selectList(new LambdaQueryWrapper<LessonStudentTier>()
                .eq(LessonStudentTier::getClassId, classId)
                .eq(LessonStudentTier::getStudentId, UserContext.userId()))) {
            assignedTiers.put(assignment.getLessonId(), assignment.getTier());
        }
        for (Lesson lesson : lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getClassTypeId, group.getClassTypeId()).eq(Lesson::getStatus, 1)
                .orderByAsc(Lesson::getLessonOrder))) {
            String tier = assignedTiers.get(lesson.getId());
            List<Map<String, Object>> visible = materialsFor(lesson.getId()).stream()
                    .filter(m -> tier != null && tier.equals(m.getTier()))
                    .map(m -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("id", m.getId());
                        entry.put("kind", m.getKind());
                        entry.put("title", m.getTitle());
                        entry.put("content", m.getContent());
                        return entry;
                    }).toList();
            Map<String, Object> folder = new LinkedHashMap<>();
            folder.put("id", lesson.getId());
            folder.put("title", lesson.getTitle());
            folder.put("lessonOrder", lesson.getLessonOrder());
            folder.put("tier", tier);
            folder.put("materials", visible);
            result.add(folder);
        }
        return Result.success(result);
    }

    @GetMapping("/videos/{id}")
    public ResponseEntity<FileSystemResource> video(@PathVariable Long id) {
        UserContext.requireStudent();
        return videoForStudent(id, UserContext.userId());
    }

    @GetMapping("/videos/{id}/ticket")
    public Result<Map<String, String>> videoTicket(@PathVariable Long id) {
        UserContext.requireStudent();
        authorizeVideo(id, UserContext.userId());
        Instant now = Instant.now();
        videoTickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        String ticket = UUID.randomUUID().toString();
        videoTickets.put(ticket, new VideoTicket(id, UserContext.userId(), now.plusSeconds(3600)));
        return Result.success(Map.of("ticket", ticket));
    }

    @GetMapping("/stream/{ticket}")
    public ResponseEntity<FileSystemResource> stream(@PathVariable String ticket) {
        VideoTicket grant = videoTickets.get(ticket);
        if (grant == null || grant.expiresAt().isBefore(Instant.now())) {
            videoTickets.remove(ticket);
            throw new BusinessException(ErrorCode.FORBIDDEN, "播放凭证已失效");
        }
        return videoForStudent(grant.materialId(), grant.studentId());
    }

    private ResponseEntity<FileSystemResource> videoForStudent(Long id, Long studentId) {
        LessonMaterial material = authorizeVideo(id, studentId);
        Path path = Path.of(videoDir).toAbsolutePath().normalize().resolve(material.getFilePath()).normalize();
        if (!path.startsWith(Path.of(videoDir).toAbsolutePath().normalize()) || !Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频文件不存在");
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("Referrer-Policy", "no-referrer")
                .contentType(MediaType.parseMediaType(material.getMimeType()))
                .body(new FileSystemResource(path));
    }

    private LessonMaterial authorizeVideo(Long id, Long studentId) {
        User student = userMapper.selectById(studentId);
        if (student == null || !"student".equals(student.getRole())
                || !Integer.valueOf(1).equals(student.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        LessonMaterial material = requireMaterial(id);
        if (!"VIDEO".equals(material.getKind())) throw new BusinessException(ErrorCode.NOT_FOUND);
        Lesson lesson = requireLesson(material.getLessonId());
        if (!Integer.valueOf(1).equals(lesson.getStatus())) throw new BusinessException(ErrorCode.FORBIDDEN);
        boolean allowed = lessonTierMapper.selectList(new LambdaQueryWrapper<LessonStudentTier>()
                .eq(LessonStudentTier::getLessonId, lesson.getId())
                .eq(LessonStudentTier::getStudentId, studentId)
                .eq(LessonStudentTier::getTier, material.getTier())).stream().anyMatch(assignment -> {
                    ClassGroup group = classMapper.selectById(assignment.getClassId());
                    boolean isMember = memberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                            .eq(ClassMember::getClassId, assignment.getClassId())
                            .eq(ClassMember::getStudentId, studentId)) > 0;
                    return group != null && Integer.valueOf(1).equals(group.getStatus())
                            && lesson.getClassTypeId().equals(group.getClassTypeId()) && isMember;
                });
        if (!allowed) throw new BusinessException(ErrorCode.FORBIDDEN);
        return material;
    }

    private ClassMember requireMembership(Long classId) {
        ClassMember member = memberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId).eq(ClassMember::getStudentId, UserContext.userId()));
        if (member == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        return member;
    }

    private ClassType requireType(Long id) {
        ClassType type = typeMapper.selectById(id);
        if (type == null) throw new BusinessException(ErrorCode.NOT_FOUND, "班级类型不存在");
        return type;
    }

    private Lesson requireLesson(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) throw new BusinessException(ErrorCode.NOT_FOUND, "课次不存在");
        return lesson;
    }

    private LessonMaterial requireMaterial(Long id) {
        LessonMaterial material = materialMapper.selectById(id);
        if (material == null) throw new BusinessException(ErrorCode.NOT_FOUND, "课后内容不存在");
        return material;
    }

    private List<LessonMaterial> materialsFor(Long lessonId) {
        return materialMapper.selectList(new LambdaQueryWrapper<LessonMaterial>()
                .eq(LessonMaterial::getLessonId, lessonId).orderByAsc(LessonMaterial::getId));
    }

    private void deleteMaterialFile(LessonMaterial material) {
        if (material.getFilePath() == null) return;
        try { Files.deleteIfExists(Path.of(videoDir).toAbsolutePath().normalize().resolve(material.getFilePath())); }
        catch (IOException ignored) { /* Metadata deletion still succeeds; orphan cleanup can be retried. */ }
    }

    private void checkTier(String tier) {
        if (!List.of("BASIC", "GROWTH", "CHALLENGE").contains(tier)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "学习类别无效");
        }
    }

    @Data public static class TypeRequest {
        @NotBlank public String name;
        public String description;
    }
    @Data public static class LessonRequest {
        @NotBlank public String title;
        @NotNull public Integer lessonOrder;
        public Integer status;
    }
    @Data public static class HomeworkRequest {
        @NotBlank public String title;
        @NotBlank public String tier;
        @NotBlank public String content;
    }
}
