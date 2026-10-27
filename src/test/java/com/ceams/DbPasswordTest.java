package com.ceams;

import com.ceams.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 数据库密码测试程序
 * 直接连接到数据库，检查现有用户的密码格式，并测试密码匹配逻辑
 */
public class DbPasswordTest {

    // 数据库连接信息，从application.properties中复制
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ceams?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";

    public static void main(String[] args) {
        System.out.println("=== 数据库密码测试程序 ===");

        try {
            // 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("数据库驱动加载成功");

            // 建立连接
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("数据库连接成功");

            // 查询所有用户
            String sql = "SELECT user_id, username, password_hash, name, role FROM users";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n=== 数据库中的用户信息 ===");
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String username = rs.getString("username");
                String passwordHash = rs.getString("password_hash");
                String name = rs.getString("name");
                String role = rs.getString("role");

                System.out.println("\n用户ID: " + userId);
                System.out.println("用户名: " + username);
                System.out.println("密码哈希: " + passwordHash);
                System.out.println("姓名: " + name);
                System.out.println("角色: " + role);

                // 测试密码匹配，假设密码是 '123456'
                testPasswordMatch(username, passwordHash, "123456");
            }

            // 关闭资源
            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n=== 测试程序结束 ===");
    }

    private static void testPasswordMatch(String username, String passwordHash, String rawPassword) {
        System.out.println("\n--- 测试密码匹配: username=" + username + ", rawPassword=" + rawPassword + " ---");
        System.out.println("存储的密码哈希: " + passwordHash);

        if (passwordHash == null || passwordHash.isEmpty()) {
            System.out.println("结果: 密码哈希为空");
            return;
        }

        // 检查密码哈希格式
        if (passwordHash.startsWith("$2a$")) {
            System.out.println("密码哈希格式: BCrypt (正确)");
        } else {
            System.out.println("密码哈希格式: 未知 (错误)");
        }

        // 测试密码匹配
        boolean matches = PasswordUtil.matches(rawPassword, passwordHash);
        System.out.println("密码匹配结果: " + matches);

        if (matches) {
            System.out.println("结果: 密码匹配成功");
        } else {
            System.out.println("结果: 密码匹配失败");
        }

        // 测试生成新的密码哈希并匹配
        String newHash = PasswordUtil.encode(rawPassword);
        System.out.println("新生成的密码哈希: " + newHash);
        boolean newMatches = PasswordUtil.matches(rawPassword, newHash);
        System.out.println("新密码哈希匹配结果: " + newMatches);
    }
}
