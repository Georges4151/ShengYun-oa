package com.ceams;

import com.ceams.ui.CeamsUI;
import javafx.application.Application;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.ceams.dao")
@ComponentScan(basePackages = "com.ceams")
@EnableScheduling
public class CeamsApplication {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        // 检查并启动MySQL服务
        checkAndStartMysqlService();

        // 检查并修复数据库结构
        checkAndFixDatabaseSchema();

        // 启动Spring Boot（服务器模式，不启动JavaFX）
        context = SpringApplication.run(CeamsApplication.class, args);
    }

    private static void checkAndFixDatabaseSchema() {
        try {
            java.sql.Connection connection = java.sql.DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/ceams?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8",
                    "root",
                    "123456");

            // 检查 stamp_positions 列的数据类型
            java.sql.ResultSet rs = connection.getMetaData().getColumns(
                    "ceams", null, "stamp_approvals", "stamp_positions");

            boolean needsFix = true;
            if (rs.next()) {
                String columnType = rs.getString("TYPE_NAME");
                if ("LONGTEXT".equalsIgnoreCase(columnType)) {
                    needsFix = false;
                    System.out.println("stamp_positions 列已经是 LONGTEXT 类型，无需修复");
                }
            }
            rs.close();

            if (needsFix) {
                System.out.println("检测到 stamp_positions 列需要修复，正在修改为 LONGTEXT 类型...");
                java.sql.Statement stmt = connection.createStatement();
                stmt.execute("ALTER TABLE stamp_approvals MODIFY COLUMN stamp_positions LONGTEXT COMMENT '印章位置信息JSON'");
                stmt.close();
                System.out.println("stamp_positions 列修复成功！");
            }

            connection.close();
        } catch (Exception e) {
            System.out.println("检查或修复数据库结构时出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkAndStartMysqlService() {
        try {
            // 加载MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 检查MySQL是否可连接
            java.sql.Connection connection = java.sql.DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/ceams?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8",
                    "root",
                    "123456");
            connection.close();
            System.out.println("MySQL服务已启动并可连接");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL驱动未找到：" + e.getMessage());
        } catch (java.sql.SQLException e) {
            System.out.println("MySQL服务未启动，尝试启动...");
            // 尝试启动MySQL服务
            try {
                Process process = Runtime.getRuntime()
                        .exec("cmd /c start /B ./mysql/bin/mysqld --defaults-file=./mysql/my.ini");
                process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                System.out.println("MySQL服务启动成功");
            } catch (Exception ex) {
                System.out.println("启动MySQL服务失败：" + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public static ConfigurableApplicationContext getContext() {
        return context;
    }
}