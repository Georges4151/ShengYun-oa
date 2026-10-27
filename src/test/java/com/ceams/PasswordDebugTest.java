package com.ceams;

import com.ceams.model.entity.User;
import com.ceams.service.UserService;
import com.ceams.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 密码调试测试
 * 用于调试密码匹配问题
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class PasswordDebugTest {

    @Autowired
    private UserService userService;

    @Test
    public void debugPasswordMatching() {
        // 测试使用正确的密码登录
        String username = "admin";
        String expectedPassword = "123456";

        System.out.println("=== 密码匹配调试开始 ===");
        System.out.println("测试用户名: " + username);
        System.out.println("预期密码: " + expectedPassword);

        // 查找用户
        User user = userService.findByUsername(username);
        System.out.println("找到用户: " + (user != null ? "是" : "否"));

        if (user != null) {
            System.out.println("用户ID: " + user.getUserId());
            System.out.println("用户名: " + user.getUsername());
            System.out.println("存储的密码哈希: " + user.getPassword());

            // 生成预期密码的哈希值，用于比较
            String expectedHash = PasswordUtil.encode(expectedPassword);
            System.out.println("预期密码生成的哈希: " + expectedHash);

            // 检查两个哈希值是否相同
            System.out.println("存储的哈希与预期哈希是否相同: " + expectedHash.equals(user.getPassword()));

            // 检查存储的哈希是否为BCrypt格式
            boolean isBCrypt = user.getPassword().startsWith("$2a$");
            System.out.println("存储的哈希是否为BCrypt格式: " + isBCrypt);

            // 测试密码匹配
            boolean matches = PasswordUtil.matches(expectedPassword, user.getPassword());
            System.out.println("密码匹配结果: " + matches);

            // 测试使用生成的哈希值进行匹配
            boolean matchesWithGeneratedHash = PasswordUtil.matches(expectedPassword, expectedHash);
            System.out.println("使用生成的哈希值进行匹配: " + matchesWithGeneratedHash);

            // 测试使用UserService的checkPassword方法
            boolean serviceMatches = userService.checkPassword(user, expectedPassword);
            System.out.println("UserService.checkPassword结果: " + serviceMatches);

            // 测试直接使用PasswordEncoder进行匹配
            boolean directMatches = PasswordUtil.getPasswordEncoder().matches(expectedPassword, user.getPassword());
            System.out.println("直接使用PasswordEncoder匹配结果: " + directMatches);
        }

        System.out.println("=== 密码匹配调试结束 ===");
    }
}
