package com.ceams;

import com.ceams.util.PasswordUtil;

/**
 * 简单的密码测试程序
 * 直接测试PasswordUtil的功能，绕过数据库和Spring框架
 */
public class SimplePasswordTest {

    public static void main(String[] args) {
        System.out.println("=== 简单密码测试程序 ===");

        // 测试密码
        String rawPassword = "123456";
        System.out.println("测试密码: " + rawPassword);

        // 测试1: 加密密码
        System.out.println("\n--- 测试1: 加密密码 ---");
        String encodedPassword = PasswordUtil.encode(rawPassword);
        System.out.println("加密后的密码: " + encodedPassword);
        System.out.println("密码长度: " + encodedPassword.length());
        System.out.println("是否为BCrypt格式: " + encodedPassword.startsWith("$2a$"));

        // 测试2: 验证正确密码
        System.out.println("\n--- 测试2: 验证正确密码 ---");
        boolean matches = PasswordUtil.matches(rawPassword, encodedPassword);
        System.out.println("密码匹配结果: " + matches);

        // 测试3: 验证错误密码
        System.out.println("\n--- 测试3: 验证错误密码 ---");
        boolean wrongMatches = PasswordUtil.matches("wrongpassword", encodedPassword);
        System.out.println("错误密码匹配结果: " + wrongMatches);

        // 测试4: 验证空密码
        System.out.println("\n--- 测试4: 验证空密码 ---");
        boolean emptyMatches1 = PasswordUtil.matches("", encodedPassword);
        boolean emptyMatches2 = PasswordUtil.matches(rawPassword, "");
        System.out.println("空明文密码匹配结果: " + emptyMatches1);
        System.out.println("空加密密码匹配结果: " + emptyMatches2);

        // 测试5: 多次加密同一密码
        System.out.println("\n--- 测试5: 多次加密同一密码 ---");
        String encodedPassword2 = PasswordUtil.encode(rawPassword);
        String encodedPassword3 = PasswordUtil.encode(rawPassword);
        System.out.println("第一次加密结果: " + encodedPassword);
        System.out.println("第二次加密结果: " + encodedPassword2);
        System.out.println("第三次加密结果: " + encodedPassword3);
        System.out.println("三次加密结果是否相同: "
                + (encodedPassword.equals(encodedPassword2) && encodedPassword2.equals(encodedPassword3)));

        // 测试6: 验证不同加密结果
        System.out.println("\n--- 测试6: 验证不同加密结果 ---");
        boolean matches2 = PasswordUtil.matches(rawPassword, encodedPassword2);
        boolean matches3 = PasswordUtil.matches(rawPassword, encodedPassword3);
        System.out.println("第一次加密结果匹配: " + matches);
        System.out.println("第二次加密结果匹配: " + matches2);
        System.out.println("第三次加密结果匹配: " + matches3);

        // 测试7: 测试密码强度检查
        System.out.println("\n--- 测试7: 测试密码强度检查 ---");
        System.out.println("密码'123456'强度: " + PasswordUtil.checkPasswordStrength("123456"));
        System.out.println("密码'12345'强度: " + PasswordUtil.checkPasswordStrength("12345"));
        System.out.println("密码'password123'强度: " + PasswordUtil.checkPasswordStrength("password123"));

        System.out.println("\n=== 测试程序结束 ===");
    }
}
