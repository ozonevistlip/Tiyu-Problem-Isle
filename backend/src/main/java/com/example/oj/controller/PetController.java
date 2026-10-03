package com.example.oj.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.common.Result;
import com.example.oj.entity.Pet;
import com.example.oj.entity.PetGrant;
import com.example.oj.entity.User;
import com.example.oj.mapper.PetGrantMapper;
import com.example.oj.mapper.PetMapper;
import com.example.oj.mapper.UserMapper;
import com.example.oj.service.PetChatService;
import com.example.oj.utils.UserContext;
import com.example.oj.utils.WebpImageInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets")
public class PetController {
    private final PetMapper pets;
    private final PetGrantMapper grants;
    private final UserMapper users;
    private final PetChatService chat;

    @Value("${oj.pet-dir:./pet-assets}")
    private String petDir;

    public record PetView(Long id, String name, String description, boolean published, boolean owned,
                          boolean hasPreview, boolean hasAnimation) {}
    public record AdminPetView(Long id, String name, String description, String personalityPrompt,
                               boolean published, boolean hasPreview, boolean hasAnimation) {}

    @GetMapping
    public Result<List<PetView>> list() {
        String role = UserContext.role();
        Set<Long> owned = new HashSet<>();
        if ("student".equals(role)) {
            grants.selectList(new LambdaQueryWrapper<PetGrant>()
                    .eq(PetGrant::getStudentId, UserContext.userId()))
                    .forEach(grant -> owned.add(grant.getPetId()));
        }
        return Result.success(pets.selectList(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getStatus, 1).orderByAsc(Pet::getId)).stream()
                .map(pet -> view(pet, !"student".equals(role) || owned.contains(pet.getId())))
                .toList());
    }

    @GetMapping("/admin")
    public Result<List<AdminPetView>> adminList() {
        UserContext.requireSuperAdmin();
        return Result.success(pets.selectList(new LambdaQueryWrapper<Pet>().orderByDesc(Pet::getId))
                .stream().map(this::adminView).toList());
    }

    @PostMapping("/admin")
    public Result<AdminPetView> create(@Valid @RequestBody PetRequest request) {
        UserContext.requireSuperAdmin();
        Pet pet = new Pet();
        pet.setStatus(0);
        apply(pet, request);
        pets.insert(pet);
        return Result.success(adminView(pet));
    }

    @PutMapping("/admin/{id}")
    public Result<AdminPetView> update(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        UserContext.requireSuperAdmin();
        Pet pet = requirePet(id);
        apply(pet, request);
        pets.updateById(pet);
        return Result.success(adminView(pet));
    }

    @PutMapping("/admin/{id}/publish")
    public Result<AdminPetView> publish(@PathVariable Long id, @RequestBody PublishRequest request) {
        UserContext.requireSuperAdmin();
        Pet pet = requirePet(id);
        if (request.published && (pet.getPreviewPath() == null || pet.getAtlasPath() == null)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先上传预览图和动画图集");
        }
        pet.setStatus(request.published ? 1 : 0);
        pets.updateById(pet);
        return Result.success(adminView(pet));
    }

    @PostMapping(value = "/admin/{id}/assets/{kind}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AdminPetView> upload(@PathVariable Long id, @PathVariable String kind,
                                        @RequestPart MultipartFile file) throws IOException {
        UserContext.requireSuperAdmin();
        Pet pet = requirePet(id);
        if (!"preview".equals(kind) && !"atlas".equals(kind)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源类型无效");
        }
        if (file.isEmpty() || file.getSize() > 8L * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片必须小于 8MB");
        }
        if (!"image/webp".equalsIgnoreCase(file.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件类型必须为 image/webp");
        }
        byte[] content = file.getBytes();
        WebpImageInfo image = WebpImageInfo.read(content);
        if (image == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "请上传有效的静态 WebP 图片");
        if ("atlas".equals(kind) && (image.width() != 1536 || image.height() != 1872)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "动画图集必须为 1536×1872 像素（8 列×9 行）");
        }
        if ("preview".equals(kind) && (image.width() > 2048 || image.height() > 2048)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预览图宽高不能超过 2048 像素");
        }
        Path directory = Path.of(petDir).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String fileName = UUID.randomUUID() + ".webp";
        Path destination = directory.resolve(fileName);
        Files.write(destination, content);
        if ("preview".equals(kind)) pet.setPreviewPath(fileName);
        else pet.setAtlasPath(fileName);
        pets.updateById(pet);
        return Result.success(adminView(pet));
    }

    @GetMapping("/{id}/media/{kind}")
    public ResponseEntity<byte[]> media(@PathVariable Long id, @PathVariable String kind) throws IOException {
        Pet pet = requirePet(id);
        if (!"preview".equals(kind) && !"atlas".equals(kind)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        boolean admin = "SUPER_ADMIN".equals(UserContext.role());
        if (!admin && !Integer.valueOf(1).equals(pet.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if ("atlas".equals(kind) && !admin && !canUse(pet.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        String fileName = "preview".equals(kind) ? pet.getPreviewPath() : pet.getAtlasPath();
        if (fileName == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        byte[] bytes;
        MediaType type;
        if (fileName.startsWith("builtin:")) {
            boolean preview = "preview".equals(kind);
            try (InputStream stream = new ClassPathResource(preview ? "pet/default.png" : "pet/default.webp").getInputStream()) {
                bytes = stream.readAllBytes();
            }
            type = preview ? MediaType.IMAGE_PNG : MediaType.parseMediaType("image/webp");
        } else {
            Path directory = Path.of(petDir).toAbsolutePath().normalize();
            Path path = directory.resolve(fileName).normalize();
            if (!path.startsWith(directory) || !Files.isRegularFile(path)) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            bytes = Files.readAllBytes(path);
            String lowerName = fileName.toLowerCase(Locale.ROOT);
            if (lowerName.endsWith(".webp")) type = MediaType.parseMediaType("image/webp");
            else if (lowerName.endsWith(".png")) type = MediaType.IMAGE_PNG;
            else throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.noStore()).body(bytes);
    }

    @GetMapping("/teacher/students/{studentId}/grants")
    public Result<List<Long>> studentGrants(@PathVariable Long studentId) {
        requireOwnStudent(studentId);
        return Result.success(grants.selectList(new LambdaQueryWrapper<PetGrant>()
                .eq(PetGrant::getStudentId, studentId)).stream().map(PetGrant::getPetId).toList());
    }

    @PutMapping("/teacher/students/{studentId}/grants/{petId}")
    @Transactional
    public Result<Void> setGrant(@PathVariable Long studentId, @PathVariable Long petId,
                                  @RequestBody GrantRequest request) {
        requireOwnStudent(studentId);
        Pet pet = requirePet(petId);
        if (request.granted && !Integer.valueOf(1).equals(pet.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只能发放已发布的宠物");
        }
        PetGrant existing = grants.selectOne(new LambdaQueryWrapper<PetGrant>()
                .eq(PetGrant::getStudentId, studentId).eq(PetGrant::getPetId, petId));
        if (request.granted && existing == null) {
            PetGrant grant = new PetGrant();
            grant.setStudentId(studentId);
            grant.setPetId(petId);
            grant.setGrantedBy(UserContext.userId());
            grants.insert(grant);
        } else if (!request.granted && existing != null) {
            grants.deleteById(existing.getId());
        }
        return Result.success(null);
    }

    @PostMapping("/{id}/chat")
    public Result<Map<String, String>> reply(@PathVariable Long id, @Valid @RequestBody ChatRequest request) {
        Pet pet = requirePet(id);
        if ((!Integer.valueOf(1).equals(pet.getStatus()) && !"SUPER_ADMIN".equals(UserContext.role())) || !canUse(id)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "尚无此宠物的使用权限");
        }
        return Result.success(Map.of("reply", chat.reply(pet.getPersonalityPrompt(), request.messages)));
    }

    private boolean canUse(Long petId) {
        if ("teacher".equals(UserContext.role()) || "SUPER_ADMIN".equals(UserContext.role())) return true;
        return "student".equals(UserContext.role()) && grants.selectCount(new LambdaQueryWrapper<PetGrant>()
                .eq(PetGrant::getStudentId, UserContext.userId()).eq(PetGrant::getPetId, petId)) > 0;
    }

    private User requireOwnStudent(Long studentId) {
        UserContext.requireTeacher();
        User student = users.selectById(studentId);
        if (student == null || !"student".equals(student.getRole())
                || !UserContext.userId().equals(student.getCreatedBy())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能管理自己创建的学生账号");
        }
        return student;
    }

    private Pet requirePet(Long id) {
        Pet pet = pets.selectById(id);
        if (pet == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return pet;
    }

    private PetView view(Pet pet, boolean owned) {
        return new PetView(pet.getId(), pet.getName(), pet.getDescription(), true, owned,
                pet.getPreviewPath() != null, pet.getAtlasPath() != null);
    }

    private AdminPetView adminView(Pet pet) {
        return new AdminPetView(pet.getId(), pet.getName(), pet.getDescription(), pet.getPersonalityPrompt(),
                Integer.valueOf(1).equals(pet.getStatus()), pet.getPreviewPath() != null, pet.getAtlasPath() != null);
    }

    private void apply(Pet pet, PetRequest request) {
        pet.setName(request.name.trim());
        pet.setDescription(request.description == null ? null : request.description.trim());
        pet.setPersonalityPrompt(request.personalityPrompt.trim());
    }

    @Data public static class PetRequest {
        @NotBlank @Size(max = 80) public String name;
        @Size(max = 500) public String description;
        @NotBlank @Size(max = 5000) public String personalityPrompt;
    }
    @Data public static class PublishRequest { public boolean published; }
    @Data public static class GrantRequest { public boolean granted; }
    @Data public static class ChatRequest { public List<PetChatService.ChatMessage> messages; }
}
