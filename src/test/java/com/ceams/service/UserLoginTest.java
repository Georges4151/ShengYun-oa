package com.ceams.service;

import com.ceams.model.entity.User;
import com.ceams.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户登录测试
 * 直接测试完整的登录流程：查找用户 -> 验证密码
 */
@SpringBootTest
public class UserLoginTest {

    @Autowired
    private UserService userService;

    @Test
    public void testUserLoginFlow() {
        // 测试现有的admin用户登录，密码应该是 '123456'
        String username = "admin";
        String password = "123456";

        System.out.println("测试用户登录: username=" + username + ", password=" + password);

        // 1. 查找用户
        User user = userService.findByUsername(username);
        assertNotNull(user, "用户应该存在");
        System.out.println("找到用户: " + user.getUsername() + ", 密码哈希: " + user.getPassword());

        // 2. 验证密码格式是否为BCrypt格式
        String encodedPassword = user.getPassword();
        assertTrue(encodedPassword.startsWith("$2a$"), "密码应该是BCrypt格式");

        // 3. 验证密码匹配
        boolean matches = PasswordUtil.matches(password, encodedPassword);
        System.out.println("密码匹配结果: " + matches);
        assertTrue(matches, "密码应该匹配成功");

        // 4. 测试使用UserService的checkPassword方法
        boolean serviceMatches = userService.checkPassword(user, password);
        System.out.println("UserService密码匹配结果: " + serviceMatches);
        assertTrue(serviceMatches, "UserService密码匹配应该成功");
    }

    @Test
    public void testUserNotFound() {
        // 测试不存在的用户
        User user = userService.findByUsername("nonexistent");
        assertNull(user, "不存在的用户应该返回null");
    }

    @Test
    public void testWrongPassword() {
        // 测试错误密码
        String username = "admin";
        String wrongPassword = "wrongpassword";

        User user = userService.findByUsername(username);
        assertNotNull(user, "用户应该存在");

        boolean matches = PasswordUtil.matches(wrongPassword, user.getPassword());
        assertFalse(matches, "错误密码不应匹配");
    }
}
