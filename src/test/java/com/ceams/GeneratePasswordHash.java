package com.ceams;

import com.ceams.util.PasswordUtil;

/**
 * 生成密码哈希值的工具类
 * 用于生成正确的BCrypt密码哈希
 */
public class GeneratePasswordHash {

    public static void main(String[] args) {
        // 要生成哈希的密码
        String password = "123456";

        // 生成BCrypt哈希值
        String hash = PasswordUtil.encode(password);

        System.out.println("密码: " + password);
        System.out.println("BCrypt哈希: " + hash);

        // 验证生成的哈希是否正确
        boolean matches = PasswordUtil.matches(password, hash);
        System.out.println("哈希验证结果: " + matches);
    }
}
