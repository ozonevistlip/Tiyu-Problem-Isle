package com.example.oj.controller;

import com.example.oj.common.BusinessException;
import com.example.oj.entity.Pet;
import com.example.oj.entity.PetGrant;
import com.example.oj.entity.User;
import com.example.oj.mapper.PetGrantMapper;
import com.example.oj.mapper.PetMapper;
import com.example.oj.mapper.UserMapper;
import com.example.oj.service.PetChatService;
import com.example.oj.utils.LoginUser;
import com.example.oj.utils.UserContext;
import com.example.oj.utils.WebpImageInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PetControllerTest {
    private final PetMapper pets = mock(PetMapper.class);
    private final PetGrantMapper grants = mock(PetGrantMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final PetChatService chat = mock(PetChatService.class);
    private final PetController controller = new PetController(pets, grants, users, chat);
    @TempDir Path petDir;

    @AfterEach
    void clear() { UserContext.clear(); }

    @Test
    void studentWithoutGrantCannotChat() {
        UserContext.set(LoginUser.builder().userId(7L).role("student").build());
        Pet pet = new Pet(); pet.setId(2L); pet.setStatus(1);
        when(pets.selectById(2L)).thenReturn(pet);
        PetController.ChatRequest request = new PetController.ChatRequest();
        request.messages = List.of(new PetChatService.ChatMessage("user", "你好"));

        assertThrows(BusinessException.class, () -> controller.reply(2L, request));
        verifyNoInteractions(chat);
    }

    @Test
    void teacherCannotGrantToAnotherTeachersStudent() {
        UserContext.set(LoginUser.builder().userId(10L).role("teacher").build());
        when(users.selectById(7L)).thenReturn(User.builder().id(7L).role("student").createdBy(11L).build());
        PetController.GrantRequest request = new PetController.GrantRequest();
        request.granted = true;

        assertThrows(BusinessException.class, () -> controller.setGrant(7L, 2L, request));
        verifyNoInteractions(grants, pets);
    }

    @Test
    void studentCannotDownloadUnownedAnimation() {
        UserContext.set(LoginUser.builder().userId(7L).role("student").build());
        Pet pet = new Pet(); pet.setId(2L); pet.setStatus(1); pet.setAtlasPath("some.png");
        when(pets.selectById(2L)).thenReturn(pet);

        assertThrows(BusinessException.class, () -> controller.media(2L, "atlas"));
    }

    @Test
    void studentCanViewPublishedPreviewWithoutGrant() throws Exception {
        UserContext.set(LoginUser.builder().userId(7L).role("student").build());
        Pet pet = new Pet(); pet.setId(1L); pet.setStatus(1); pet.setPreviewPath("builtin:preview");
        when(pets.selectById(1L)).thenReturn(pet);

        assertTrue(controller.media(1L, "preview").getBody().length > 0);
        verifyNoInteractions(grants);
    }

    @Test
    void grantingOnePetOnlyInsertsThatPetForTheStudent() {
        UserContext.set(LoginUser.builder().userId(10L).role("teacher").build());
        when(users.selectById(7L)).thenReturn(User.builder().id(7L).role("student").createdBy(10L).build());
        Pet pet = new Pet(); pet.setId(2L); pet.setStatus(1);
        when(pets.selectById(2L)).thenReturn(pet);
        PetController.GrantRequest request = new PetController.GrantRequest();
        request.granted = true;

        controller.setGrant(7L, 2L, request);

        ArgumentCaptor<PetGrant> inserted = ArgumentCaptor.forClass(PetGrant.class);
        verify(grants).insert(inserted.capture());
        assertTrue(inserted.getValue().getStudentId().equals(7L));
        assertTrue(inserted.getValue().getPetId().equals(2L));
        verify(grants, never()).delete(any());
    }

    @Test
    void revokingOnePetOnlyDeletesItsGrant() {
        UserContext.set(LoginUser.builder().userId(10L).role("teacher").build());
        when(users.selectById(7L)).thenReturn(User.builder().id(7L).role("student").createdBy(10L).build());
        Pet pet = new Pet(); pet.setId(2L); pet.setStatus(1);
        when(pets.selectById(2L)).thenReturn(pet);
        PetGrant grant = new PetGrant(); grant.setId(42L); grant.setStudentId(7L); grant.setPetId(2L);
        when(grants.selectOne(any())).thenReturn(grant);
        PetController.GrantRequest request = new PetController.GrantRequest();
        request.granted = false;

        controller.setGrant(7L, 2L, request);

        verify(grants).deleteById(42L);
        verify(grants, never()).delete(any());
        verify(grants, never()).insert(any(PetGrant.class));
    }

    @Test
    void uploadsStaticWebpAndServesItWithWebpContentType() throws Exception {
        UserContext.set(LoginUser.builder().userId(1L).role("SUPER_ADMIN").build());
        ReflectionTestUtils.setField(controller, "petDir", petDir.toString());
        Pet pet = new Pet(); pet.setId(2L); pet.setStatus(0);
        when(pets.selectById(2L)).thenReturn(pet);
        byte[] bytes = new ClassPathResource("pet/default.webp").getInputStream().readAllBytes();
        assertEquals(new WebpImageInfo(1536, 1872), WebpImageInfo.read(bytes));

        controller.upload(2L, "atlas", new MockMultipartFile("file", "atlas.webp", "image/webp", bytes));

        assertTrue(pet.getAtlasPath().endsWith(".webp"));
        assertTrue(Files.exists(petDir.resolve(pet.getAtlasPath())));
        assertEquals(MediaType.parseMediaType("image/webp"), controller.media(2L, "atlas").getHeaders().getContentType());
    }

    @Test
    void rejectsPngAndAnimatedWebpUploads() throws Exception {
        UserContext.set(LoginUser.builder().userId(1L).role("SUPER_ADMIN").build());
        Pet pet = new Pet(); pet.setId(2L); pet.setStatus(0);
        when(pets.selectById(2L)).thenReturn(pet);
        byte[] png = new ClassPathResource("pet/default.png").getInputStream().readAllBytes();
        assertThrows(BusinessException.class, () -> controller.upload(2L, "preview",
                new MockMultipartFile("file", "preview.png", "image/png", png)));

        byte[] webp = new ClassPathResource("pet/default.webp").getInputStream().readAllBytes();
        assertThrows(BusinessException.class, () -> controller.upload(2L, "atlas",
                new MockMultipartFile("file", "atlas.webp", "image/png", webp)));
        // Convert the lossless image to an extended container with the animation flag set.
        byte[] animated = new byte[webp.length + 18];
        System.arraycopy(webp, 0, animated, 0, 12);
        int size = animated.length - 8;
        for (int i = 0; i < 4; i++) animated[4 + i] = (byte) (size >>> (8 * i));
        System.arraycopy("VP8X".getBytes(), 0, animated, 12, 4);
        animated[16] = 10;
        animated[20] = 0x02;
        System.arraycopy(webp, 12, animated, 30, webp.length - 12);
        assertThrows(BusinessException.class, () -> controller.upload(2L, "atlas",
                new MockMultipartFile("file", "animated.webp", "image/webp", animated)));
    }

    @Test
    void servesExistingPngAssetsWithPngContentType() throws Exception {
        UserContext.set(LoginUser.builder().userId(1L).role("SUPER_ADMIN").build());
        ReflectionTestUtils.setField(controller, "petDir", petDir.toString());
        Pet pet = new Pet(); pet.setId(2L); pet.setPreviewPath("old.png");
        when(pets.selectById(2L)).thenReturn(pet);
        Files.write(petDir.resolve("old.png"), new byte[] {1, 2, 3});

        assertEquals(MediaType.IMAGE_PNG, controller.media(2L, "preview").getHeaders().getContentType());
    }
}
