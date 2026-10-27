package com.ceams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CEAMS 建筑企业账目管理系统 - 后端启动入口。
 *
 * <p>模块化单体：领域包结构 {@code com.ceams.module.{domain}}，公共基础设施 {@code com.ceams.common}。
 * 三端（Web / Electron 桌面 / 微信小程序）统一走 {@code /api/v1} 契约。</p>
 */
@SpringBootApplication
public class CeamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CeamsApplication.class, args);
    }
}