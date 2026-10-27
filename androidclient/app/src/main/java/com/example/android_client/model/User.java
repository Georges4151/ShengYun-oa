package com.example.android_client.model;

/**
 * 用户实体类
 * 对应后端User实体
 */
public class User {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String department;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * 获取角色显示文本
     */
    public String getRoleDisplay() {
        switch (role) {
            case "ADMIN":
                return "管理员";
            case "MANAGER":
                return "经理";
            case "PROJECT_MANAGER":
                return "项目经理";
            case "ACCOUNTANT":
                return "会计";
            case "EMPLOYEE":
                return "员工";
            default:
                return role;
        }
    }

    /**
     * 检查用户是否有审批权限
     */
    public boolean hasApprovalPermission() {
        return "MANAGER".equals(role) || "PROJECT_MANAGER".equals(role) || "ADMIN".equals(role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
