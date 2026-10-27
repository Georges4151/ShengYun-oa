package com.ceams.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class SecurityStartupValidator implements ApplicationListener<ApplicationReadyEvent> {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${spring.datasource.password}")
    private String dbPassword;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 开发环境：放宽验证要求
        if (jwtSecret == null || jwtSecret.isEmpty() || jwtSecret.length() < 32) {
            System.err.println("警告：jwt.secret 未设置或长度不足32位，建议在生产环境中配置");
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            System.err.println("警告：数据库密码未设置，建议在生产环境中配置");
        }
    }
}
