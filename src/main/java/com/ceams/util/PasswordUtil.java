package com.ceams.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码处理工具类
 * 提供统一的密码加密和验证功能
 */
public class PasswordUtil {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 私有构造函数，防止实例化
     */
    private PasswordUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 对密码进行加密
     * 
     * @param rawPassword 明文密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 验证密码是否匹配
     * 
     * @param rawPassword     明文密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return false;
        }
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 获取密码编码器实例
     * 
     * @return 密码编码器
     */
    public static PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    /**
     * 检查密码强度
     * 
     * @param password 密码
     * @return 是否符合密码强度要求
     */
    public static boolean checkPasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        // 可以添加更多密码强度检查，如包含大小写字母、数字、特殊字符等
        return true;
    }
}