package com.example.android_client.model;

public class LoginResponse {
    private boolean success;
    private String message;
    private LoginData data;

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

    public LoginData getData() {
        return data;
    }

    public void setData(LoginData data) {
        this.data = data;
    }

    public String getToken() {
        return data != null ? data.getToken() : null;
    }

    public Integer getUserId() {
        return data != null ? data.getUserId() : null;
    }

    public String getUsername() {
        return data != null ? data.getUsername() : null;
    }

    public String getName() {
        return data != null ? data.getName() : null;
    }

    public String getRole() {
        return data != null ? data.getRole() : null;
    }

    public String getAuthorizationHeader() {
        String token = getToken();
        return token != null ? "Bearer " + token : null;
    }

    public static class LoginData {
        private String token;
        private Integer userId;
        private String username;
        private String name;
        private String role;

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
}
