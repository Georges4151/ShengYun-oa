package com.ceams.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码工具类测试
 */
public class PasswordUtilTest {

    @Test
    public void testEncode() {
        // 测试密码加密功能
        String rawPassword = "test123";
        String encodedPassword = PasswordUtil.encode(rawPassword);

        // 加密结果不应为空
        assertNotNull(encodedPassword);
        // 加密结果不应与原始密码相同
        assertNotEquals(rawPassword, encodedPassword);
        // 加密结果长度应符合BCrypt加密规则
        assertTrue(encodedPassword.length() > 50);
    }

    @Test
    public void testMatches() {
        // 测试密码匹配功能
        String rawPassword = "test123";
        String encodedPassword = PasswordUtil.encode(rawPassword);

        // 正确密码应匹配
        assertTrue(PasswordUtil.matches(rawPassword, encodedPassword));

        // 错误密码不应匹配
        assertFalse(PasswordUtil.matches("wrongpassword", encodedPassword));

        // 空密码不应匹配
        assertFalse(PasswordUtil.matches("", encodedPassword));
        assertFalse(PasswordUtil.matches(rawPassword, ""));
    }

    @Test
    public void testCheckPasswordStrength() {
        // 测试密码强度检查
        // 短密码应返回false
        assertFalse(PasswordUtil.checkPasswordStrength("12345"));

        // 足够长度的密码应返回true
        assertTrue(PasswordUtil.checkPasswordStrength("123456"));
        assertTrue(PasswordUtil.checkPasswordStrength("password123"));

        // 空密码应返回false
        assertFalse(PasswordUtil.checkPasswordStrength(null));
        assertFalse(PasswordUtil.checkPasswordStrength(""));
    }

    @Test
    public void testSamePasswordDifferentEncoding() {
        // 测试相同密码加密后结果不同
        String rawPassword = "test123";
        String encodedPassword1 = PasswordUtil.encode(rawPassword);
        String encodedPassword2 = PasswordUtil.encode(rawPassword);

        // 相同密码加密后结果应不同（BCrypt每次生成不同的salt）
        assertNotEquals(encodedPassword1, encodedPassword2);

        // 但都应匹配原始密码
        assertTrue(PasswordUtil.matches(rawPassword, encodedPassword1));
        assertTrue(PasswordUtil.matches(rawPassword, encodedPassword2));
    }
}
