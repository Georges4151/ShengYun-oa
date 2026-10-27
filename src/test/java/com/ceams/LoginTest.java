package com.ceams;

import com.ceams.model.entity.User;
import com.ceams.service.UserService;
import com.ceams.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录功能测试
 * 直接测试UserService的登录相关功能
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class LoginTest {

    @Autowired
    private UserService userService;

    @Test
    public void testLoginWithCorrectPassword() {
        // 测试使用正确的密码登录
        String username = "admin";
        String correctPassword = "123456";

        // 查找用户
        User user = userService.findByUsername(username);
        assertNotNull(user, "用户应该存在");

        // 验证密码匹配
        boolean matches = PasswordUtil.matches(correctPassword, user.getPassword());
        assertTrue(matches, "正确密码应该匹配成功");

        // 验证UserService的checkPassword方法
        boolean serviceMatches = userService.checkPassword(user, correctPassword);
        assertTrue(serviceMatches, "UserService的checkPassword方法应该返回true");
    }

    @Test
    public void testLoginWithWrongPassword() {
        // 测试使用错误的密码登录
        String username = "admin";
        String wrongPassword = "wrongpassword";

        // 查找用户
        User user = userService.findByUsername(username);
        assertNotNull(user, "用户应该存在");

        // 验证密码不匹配
        boolean matches = PasswordUtil.matches(wrongPassword, user.getPassword());
        assertFalse(matches, "错误密码不应该匹配");

        // 验证UserService的checkPassword方法
        boolean serviceMatches = userService.checkPassword(user, wrongPassword);
        assertFalse(serviceMatches, "UserService的checkPassword方法应该返回false");
    }

    @Test
    public void testLoginWithNonexistentUser() {
        // 测试使用不存在的用户名登录
        String username = "nonexistent";

        // 查找用户
        User user = userService.findByUsername(username);
        assertNull(user, "不存在的用户应该返回null");
    }
}
