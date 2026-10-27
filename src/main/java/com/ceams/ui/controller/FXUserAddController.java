package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.User;
import com.ceams.util.PasswordUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

public class FXUserAddController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setRemoteApiClient(RemoteApiClient remoteApiClient) {
        this.apiClient = remoteApiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private Label titleLabel;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    private User user;
    private boolean isEditMode = false;

    // 角色和状态的映射关系
    private final String[][] ROLE_MAPPING = { { "普通员工", "EMPLOYEE" }, { "财务", "FINANCE" },
            { "项目经理", "PROJECT_MANAGER" }, { "经理", "MANAGER" }, { "管理员", "ADMIN" } };
    private final String[][] STATUS_MAPPING = { { "启用", "ACTIVE" }, { "禁用", "INACTIVE" } };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 清空并重新初始化角色和状态下拉框（显示中文）
        roleComboBox.getItems().clear();
        for (String[] role : ROLE_MAPPING) {
            roleComboBox.getItems().add(role[0]);
        }
        for (String[] status : STATUS_MAPPING) {
            statusComboBox.getItems().add(status[0]);
        }

        // 默认选择
        roleComboBox.setValue("普通员工");
        statusComboBox.setValue("启用");
    }

    /**
     * 设置要编辑的用户
     */
    public void setUser(User user) {
        this.user = user;
        this.isEditMode = true;
        titleLabel.setText("编辑用户");

        // 填充表单数据
        usernameField.setText(user.getUsername());
        nameField.setText(user.getName());

        // 将英文角色转换为中文显示
        roleComboBox.setValue(getChineseRole(user.getRole()));
        // 将英文状态转换为中文显示
        statusComboBox.setValue(getChineseStatus(user.getStatus()));

        // 编辑模式下，密码字段可选
        passwordField.setPromptText("留空则不修改密码");
    }

    /**
     * 保存用户
     */
    @FXML
    private void save() {
        // 表单验证
        if (!validateForm()) {
            return;
        }

        try {
            if (isEditMode) {
                // 编辑用户
                updateUser();
            } else {
                // 添加用户
                addUser();
            }

            // 关闭窗口
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            e.printStackTrace();
            showError("保存失败", "保存用户时发生错误: " + e.getMessage());
        }
    }

    /**
     * 添加用户（通过HTTP API）
     */
    private void addUser() throws IOException {
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }

        User newUser = new User();
        newUser.setUsername(usernameField.getText());
        newUser.setPassword(PasswordUtil.encode(passwordField.getText()));
        newUser.setName(nameField.getText());

        // 将中文角色转换为英文存储
        newUser.setRole(getEnglishRole(roleComboBox.getValue()));
        // 将中文状态转换为英文存储
        newUser.setStatus(getEnglishStatus(statusComboBox.getValue()));

        newUser.setCreateTime(new Date());
        newUser.setUpdateTime(new Date());

        apiClient.post("/api/users", newUser, User.class);
    }

    /**
     * 更新用户（通过HTTP API）
     */
    private void updateUser() throws IOException {
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }

        user.setUsername(usernameField.getText());

        // 如果密码不为空，则更新密码
        String password = passwordField.getText();
        if (password != null && !password.isEmpty()) {
            user.setPassword(PasswordUtil.encode(password));
        }

        user.setName(nameField.getText());

        // 将中文角色转换为英文存储
        user.setRole(getEnglishRole(roleComboBox.getValue()));
        // 将中文状态转换为英文存储
        user.setStatus(getEnglishStatus(statusComboBox.getValue()));

        user.setUpdateTime(new Date());

        apiClient.put("/api/users/" + user.getUserId(), user, User.class);
    }

    /**
     * 将英文角色转换为中文
     */
    private String getChineseRole(String englishRole) {
        for (String[] role : ROLE_MAPPING) {
            if (role[1].equals(englishRole)) {
                return role[0];
            }
        }
        return englishRole;
    }

    /**
     * 将中文角色转换为英文
     */
    private String getEnglishRole(String chineseRole) {
        for (String[] role : ROLE_MAPPING) {
            if (role[0].equals(chineseRole)) {
                return role[1];
            }
        }
        return chineseRole;
    }

    /**
     * 将英文状态转换为中文
     */
    private String getChineseStatus(String englishStatus) {
        for (String[] status : STATUS_MAPPING) {
            if (status[1].equals(englishStatus)) {
                return status[0];
            }
        }
        return englishStatus;
    }

    /**
     * 将中文状态转换为英文
     */
    private String getEnglishStatus(String chineseStatus) {
        for (String[] status : STATUS_MAPPING) {
            if (status[0].equals(chineseStatus)) {
                return status[1];
            }
        }
        return chineseStatus;
    }

    /**
     * 表单验证
     */
    private boolean validateForm() {
        // 用户名验证
        String username = usernameField.getText();
        if (username == null || username.isEmpty()) {
            showError("验证失败", "用户名不能为空");
            return false;
        }

        // 密码验证
        String password = passwordField.getText();
        if (!isEditMode && (password == null || password.isEmpty())) {
            showError("验证失败", "密码不能为空");
            return false;
        }

        // 姓名验证
        String name = nameField.getText();
        if (name == null || name.isEmpty()) {
            showError("验证失败", "姓名不能为空");
            return false;
        }

        return true;
    }

    /**
     * 显示错误信息
     */
    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 取消操作
     */
    @FXML
    private void cancel() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}