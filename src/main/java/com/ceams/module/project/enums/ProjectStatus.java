package com.ceams.module.project.enums;

/**
 * 项目状态（biz_project.status）。
 *
 * <p>枚举 name 即 varchar 代码值，与契约 ProjectStatus 一致。
 */
public enum ProjectStatus {
    PLANNING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CLOSED
}
