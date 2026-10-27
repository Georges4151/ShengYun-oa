package com.ceams.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码匹配测试
 * 用于验证密码加密和匹配逻辑是否正确
 */
public class PasswordMatchingTest {

    @Test
    public void testPasswordMatchingFlow() {
        // 模拟完整的密码流程：加密 -> 存储 -> 验证
        String rawPassword = "test123";

        // 1. 模拟用户注册/添加时的密码加密
        String encodedPassword = PasswordUtil.encode(rawPassword);
        System.out.println("加密后的密码: " + encodedPassword);

        // 2. 模拟从数据库获取加密密码（假设已经存储）
        String storedPassword = encodedPassword; // 模拟从数据库获取

        // 3. 模拟登录时的密码验证
        boolean matches = PasswordUtil.matches(rawPassword, storedPassword);
        System.out.println("密码匹配结果: " + matches);

        // 验证密码匹配成功
        assertTrue(matches, "密码应该匹配成功");

        // 验证错误密码不匹配
        assertFalse(PasswordUtil.matches("wrongpassword", storedPassword), "错误密码不应匹配");
    }

    @Test
    public void testDatabasePasswordFormat() {
        // 测试常见的密码格式是否能被正确处理
        // 这里可以添加数据库中实际的密码示例进行测试
        // 例如：假设数据库中有一个密码为 "$2a$10$7F5fR6J5z4X3cV2bN1mO0lK9jH8gF7eD6cB5aA4sS3dF2gH1jK0l"

        String rawPassword = "password123";
        String dbPassword = "$2a$10$7F5fR6J5z4X3cV2bN1mO0lK9jH8gF7eD6cB5aA4sS3dF2gH1jK0l"; // 示例数据库密码

        System.out.println("测试数据库密码格式: " + dbPassword);
        System.out.println("测试密码: " + rawPassword);
        System.out.println("匹配结果: " + PasswordUtil.matches(rawPassword, dbPassword));

        // 这个测试可能会失败，因为示例密码可能不是由当前的加密逻辑生成的
        // 但它可以帮助我们了解数据库中现有密码的格式
    }
}
