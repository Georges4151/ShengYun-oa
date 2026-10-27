package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FXLoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private void login() {
        // 清空之前的错误信息
        messageLabel.setText("");

        // 获取输入的用户名和密码
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // 验证输入
        if (!validateInput(username, password)) {
            return;
        }

        try {
            // 调用后端登录API
            RemoteApiClient apiClient = RemoteApiClient.getInstance();
            
            // 准备请求数据
            Map<String, String> loginData = new HashMap<>();
            loginData.put("username", username);
            loginData.put("password", password);
            
            // 发送登录请求
            LoginResponse response = apiClient.post("/api/auth/login", loginData, LoginResponse.class);
            
            // 验证响应
            if (response == null || response.getToken() == null || response.getToken().isEmpty()) {
                showError("登录失败", "服务器响应异常");
                return;
            }
            
            if (response.getUserId() == null) {
                showError("登录失败", "用户信息获取失败");
                return;
            }
            
            // 从响应中构建User对象
            User user = new User();
            user.setUserId(response.getUserId());
            user.setUsername(response.getUsername());
            user.setName(response.getName());
            user.setRole(response.getRole());
            user.setStatus("ACTIVE");
            
            // 验证用户状态
            if (!"ACTIVE".equals(user.getStatus())) {
                showError("登录失败", "用户账号已禁用");
                return;
            }
            
            // 登录成功，存储JWT令牌到RemoteApiClient
            apiClient.setJwtToken(response.getToken());
            
            // 存储用户信息和令牌到本地会话
            ClientSession session = ClientSession.getInstance();
            session.setCurrentUser(user);
            session.setJwtToken(response.getToken());
            
            System.out.println("用户登录成功，用户ID: " + user.getUserId());

            // 跳转到主界面
            navigateToMainScreen(user);

        } catch (Exception e) {
            e.printStackTrace();
            showError("登录异常", "登录过程中发生错误：" + e.getMessage());
        }
    }
    
    /**
     * 登录响应内部类
     */
    public static class LoginResponse {
        private String token;
        private Integer userId;
        private String username;
        private String name;
        private String role;
        
        public LoginResponse() {
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

    /**
     * 验证用户输入
     */
    private boolean validateInput(String username, String password) {
        if (username == null || username.isEmpty()) {
            messageLabel.setText("请输入用户名");
            usernameField.requestFocus();
            return false;
        }

        if (password == null || password.isEmpty()) {
            messageLabel.setText("请输入密码");
            passwordField.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * 显示错误信息
     */
    private void showError(String title, String message) {
        messageLabel.setText(message);

        // 显示错误对话框
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void cancel() {
        // 关闭窗口
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    /**
     * 跳转到现代化主界面
     */
    private void navigateToMainScreen(User user) throws IOException {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-modern.fxml"));
        
        // 手动创建控制器并注入RemoteApiClient
        loader.setControllerFactory(controllerClass -> {
            try {
                Object controller = controllerClass.getDeclaredConstructor().newInstance();
                
                // 如果控制器有setApiClient方法，则注入RemoteApiClient
                try {
                    java.lang.reflect.Method setApiClient = controllerClass.getMethod("setApiClient", RemoteApiClient.class);
                    setApiClient.invoke(controller, RemoteApiClient.getInstance());
                } catch (NoSuchMethodException e) {
                    // 控制器不需要ApiClient，忽略
                }
                
                // 如果控制器有setClientSession方法，则注入ClientSession
                try {
                    java.lang.reflect.Method setClientSession = controllerClass.getMethod("setClientSession", ClientSession.class);
                    setClientSession.invoke(controller, ClientSession.getInstance());
                } catch (NoSuchMethodException e) {
                    // 控制器不需要ClientSession，忽略
                }
                
                return controller;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create controller: " + controllerClass.getName(), e);
            }
        });
        
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("盛韵建筑账目管理系统 - 主界面");
        stage.show();
    }
}