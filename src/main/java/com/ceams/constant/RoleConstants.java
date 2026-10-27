package com.ceams.constant;

/**
 * 角色常量类
 * 统一管理系统中的角色定义
 */
public class RoleConstants {

    // 普通员工
    public static final String EMPLOYEE = "EMPLOYEE";

    // 财务
    public static final String FINANCE = "FINANCE";

    // 项目经理
    public static final String PROJECT_MANAGER = "PROJECT_MANAGER";

    // 经理
    public static final String MANAGER = "MANAGER";

    // 管理员（保留）
    public static final String ADMIN = "ADMIN";

    private RoleConstants() {
        // 私有构造方法，防止实例化
    }
}