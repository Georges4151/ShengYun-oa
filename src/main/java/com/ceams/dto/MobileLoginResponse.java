package com.ceams.dto;

public class MobileLoginResponse {
    private boolean success;
    private String message;
    private String token;
    private Integer userId;
    private String username;
    private String name;
    private String role;

    public MobileLoginResponse() {
    }

    public MobileLoginResponse(boolean success, String message, String token, Integer userId, String username,
            String name, String role) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.role = role;
    }

    public static MobileLoginResponse success(String token, Integer userId, String username, String name, String role) {
        return new MobileLoginResponse(true, "登录成功", token, userId, username, name, role);
    }

    public static MobileLoginResponse error(String message) {
        return new MobileLoginResponse(false, message, null, null, null, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
