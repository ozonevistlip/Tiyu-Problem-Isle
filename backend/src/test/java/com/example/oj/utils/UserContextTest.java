package com.example.oj.utils;

import com.example.oj.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserContextTest {
    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    @Test
    void superAdminPermissionAcceptsOnlySuperAdminRole() {
        UserContext.set(LoginUser.builder().userId(1L).role("SUPER_ADMIN").sessionId("s1").build());
        assertDoesNotThrow(UserContext::requireSuperAdmin);

        UserContext.set(LoginUser.builder().userId(2L).role("teacher").sessionId("s2").build());
        assertThrows(BusinessException.class, UserContext::requireSuperAdmin);
    }
}
