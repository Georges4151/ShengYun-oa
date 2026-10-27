package com.ceams.ui.controller;

import com.ceams.client.RemoteApiClient;
import com.ceams.client.ClientSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FXSystemSettingsController implements Initializable {

    private RemoteApiClient remoteApiClient;

    private ClientSession clientSession;

    /**
     * 设置远程API客户端
     * 
     * @param remoteApiClient 远程API客户端
     */
    public void setRemoteApiClient(RemoteApiClient remoteApiClient) {
        this.remoteApiClient = remoteApiClient;
    }

    /**
     * 设置客户端会话
     * 
     * @param clientSession 客户端会话
     */
    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TabPane settingsTabPane;

    // 基本设置组件
    @FXML
    private ComboBox<String> languageComboBox;
    @FXML
    private ComboBox<String> fontSizeComboBox;
    @FXML
    private CheckBox notificationEnabledCheckBox;
    @FXML
    private CheckBox notificationSoundCheckBox;

    // 摄像头设置组件
    @FXML
    private ComboBox<String> webcamComboBox;
    @FXML
    private Label defaultWebcamLabel;
    @FXML
    private Button setDefaultWebcamButton;
    @FXML
    private Button refreshWebcamButton;
    @FXML
    private Button testWebcamButton;

    // 备份设置组件
    @FXML
    private CheckBox autoBackupCheckBox;
    @FXML
    private Spinner<Integer> backupIntervalSpinner;
    @FXML
    private TextField backupPathTextField;
    @FXML
    private Button browseBackupPathButton;
    @FXML
    private Spinner<Integer> backupKeepDaysSpinner;
    @FXML
    private CheckBox backupCompressionCheckBox;
    @FXML
    private Button backupNowButton;
    @FXML
    private Button restoreBackupButton;

    // 底部按钮
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    // 分页设置组件
    @FXML
    private Spinner<Integer> pageSizeSpinner;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadConfigs();
        loadWebcams();
        bindEvents();
    }

    /**
     * 从远程API获取配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    private String getConfigValue(String key, String defaultValue) {
        if (remoteApiClient == null) {
            return defaultValue;
        }
        try {
            String value = remoteApiClient.get("/api/configs/" + key, String.class);
            return (value != null && !value.isEmpty()) ? value : defaultValue;
        } catch (IOException e) {
            return defaultValue;
        }
    }

    /**
     * 从远程API获取布尔配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 布尔配置值
     */
    private boolean getBooleanConfigValue(String key, boolean defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * 从远程API获取整数配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 整数配置值
     */
    private int getIntConfigValue(String key, int defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 通过远程API保存配置
     *
     * @param key   配置键
     * @param value 配置值
     */
    private void setConfigValue(String key, String value) {
        if (remoteApiClient == null) {
            return;
        }
        try {
            Map<String, String> body = new HashMap<>();
            body.put("key", key);
            body.put("value", value);
            remoteApiClient.post("/api/configs", body, Void.class);
        } catch (IOException e) {
            showErrorMessage("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 加载配置
     */
    private void loadConfigs() {
        // 基本设置
        languageComboBox.setValue(getConfigValue("ui.language", "中文"));
        fontSizeComboBox.setValue(getConfigValue("ui.font.size", "中 (14px)"));
        notificationEnabledCheckBox.setSelected(getBooleanConfigValue("notification.enabled", true));
        notificationSoundCheckBox.setSelected(getBooleanConfigValue("notification.sound", true));

        // 摄像头设置
        String defaultWebcam = getConfigValue("camera.default", "");
        defaultWebcamLabel.setText(defaultWebcam.isEmpty() ? "未设置" : defaultWebcam);

        // 备份设置 - 按天为单位
        autoBackupCheckBox.setSelected(getBooleanConfigValue("backup.auto.enabled", false));
        backupIntervalSpinner.getValueFactory().setValue(getIntConfigValue("backup.interval.days", 7));
        backupPathTextField.setText(getConfigValue("backup.path", "backup/"));
        backupKeepDaysSpinner.getValueFactory().setValue(getIntConfigValue("backup.keep.days", 30));
        backupCompressionCheckBox.setSelected(getBooleanConfigValue("backup.compression.enabled", true));

        // 分页设置
        pageSizeSpinner.getValueFactory().setValue(getIntConfigValue("pagination.page.size", 10));
    }

    /**
     * 加载摄像头列表
     */
    private void loadWebcams() {
        if (remoteApiClient == null) {
            return;
        }
        try {
            List<String> webcams = remoteApiClient.get("/api/webcams", List.class);
            webcamComboBox.setItems(FXCollections.observableArrayList(webcams));

            if (!webcams.isEmpty()) {
                webcamComboBox.getSelectionModel().selectFirst();
            }
        } catch (IOException e) {
            showErrorMessage("加载摄像头列表失败: " + e.getMessage());
        }
    }

    /**
     * 绑定事件
     */
    private void bindEvents() {
        // 基本设置事件

        // 摄像头设置事件
        refreshWebcamButton.setOnAction(e -> loadWebcams());

        setDefaultWebcamButton.setOnAction(e -> {
            String selectedWebcam = webcamComboBox.getValue();
            if (selectedWebcam != null && !selectedWebcam.isEmpty()) {
                setConfigValue("camera.default", selectedWebcam);
                defaultWebcamLabel.setText(selectedWebcam);
                showSuccessMessage("默认摄像头已设置");
            }
        });

        testWebcamButton.setOnAction(e -> {
            String selectedWebcam = webcamComboBox.getValue();
            if (selectedWebcam != null && !selectedWebcam.isEmpty()) {
                if (remoteApiClient == null) {
                    showErrorMessage("远程API客户端未初始化");
                    return;
                }
                try {
                    Map<String, String> body = new HashMap<>();
                    body.put("webcamName", selectedWebcam);
                    Boolean success = remoteApiClient.post("/api/webcams/test", body, Boolean.class);
                    if (success != null && success) {
                        showSuccessMessage("摄像头测试成功");
                    } else {
                        showErrorMessage("摄像头测试失败");
                    }
                } catch (IOException ex) {
                    showErrorMessage("摄像头测试失败: " + ex.getMessage());
                }
            } else {
                showErrorMessage("请先选择摄像头");
            }
        });

        // 备份设置事件
        autoBackupCheckBox.setOnAction(e -> {
            boolean enabled = autoBackupCheckBox.isSelected();
            backupIntervalSpinner.setDisable(!enabled);
        });

        browseBackupPathButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择备份路径");
            File selectedDirectory = directoryChooser.showDialog(settingsTabPane.getScene().getWindow());
            if (selectedDirectory != null) {
                backupPathTextField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        backupNowButton.setOnAction(e -> {
            // 触发立即备份
            Platform.runLater(() -> {
                try {
                    if (remoteApiClient == null) {
                        showErrorMessage("远程API客户端未初始化");
                        return;
                    }

                    // 显示备份中提示
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("备份中");
                    alert.setHeaderText(null);
                    alert.setContentText("正在执行备份操作，请稍候...");
                    alert.show();

                    // 执行备份操作
                    String backupPath = remoteApiClient.post("/api/backup/now", null, String.class);

                    // 关闭备份中提示
                    alert.close();

                    // 显示备份成功消息
                    showSuccessMessage("备份成功！备份文件保存到：\n" + backupPath);
                } catch (Exception ex) {
                    // 显示错误消息
                    showErrorMessage("备份失败：" + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        });

        restoreBackupButton.setOnAction(e -> {
            // 触发恢复备份
            Platform.runLater(() -> {
                try {
                    if (remoteApiClient == null) {
                        showErrorMessage("远程API客户端未初始化");
                        return;
                    }

                    // 显示备份文件选择对话框
                    File backupFile = chooseBackupFile();
                    if (backupFile == null) {
                        return; // 用户取消选择
                    }

                    // 显示恢复确认对话框
                    if (!confirmRestore()) {
                        return; // 用户取消恢复
                    }

                    // 显示恢复中提示
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("恢复中");
                    alert.setHeaderText(null);
                    alert.setContentText("正在执行恢复操作，请稍候...");
                    alert.show();

                    // 执行恢复操作
                    Map<String, String> body = new HashMap<>();
                    body.put("backupPath", backupFile.getAbsolutePath());
                    remoteApiClient.post("/api/backup/restore", body, Void.class);

                    // 关闭恢复中提示
                    alert.close();

                    // 显示恢复成功消息
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("恢复成功！建议重启应用程序以确保数据生效。");
                    successAlert.showAndWait();
                } catch (Exception ex) {
                    // 显示错误消息
                    showErrorMessage("恢复失败：" + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        });

        // 底部按钮事件
        saveButton.setOnAction(e -> saveSettings());
        cancelButton.setOnAction(e -> cancelSettings());
    }

    /**
     * 保存设置
     */
    @FXML
    private void saveSettings() {
        if (remoteApiClient == null) {
            showErrorMessage("远程API客户端未初始化");
            return;
        }

        try {
            // 构建所有配置项
            Map<String, String> configs = new HashMap<>();
            configs.put("ui.language", languageComboBox.getValue());
            configs.put("ui.font.size", fontSizeComboBox.getValue());
            configs.put("notification.enabled", String.valueOf(notificationEnabledCheckBox.isSelected()));
            configs.put("notification.sound", String.valueOf(notificationSoundCheckBox.isSelected()));
            configs.put("backup.auto.enabled", String.valueOf(autoBackupCheckBox.isSelected()));
            configs.put("backup.interval.days", String.valueOf(backupIntervalSpinner.getValue()));
            configs.put("backup.path", backupPathTextField.getText());
            configs.put("backup.keep.days", String.valueOf(backupKeepDaysSpinner.getValue()));
            configs.put("backup.compression.enabled", String.valueOf(backupCompressionCheckBox.isSelected()));
            configs.put("pagination.page.size", String.valueOf(pageSizeSpinner.getValue()));

            // 批量保存配置
            remoteApiClient.post("/api/configs", configs, Void.class);

            // 显示成功消息
            showSuccessMessage("设置已保存");

            // 提示用户重启应用以应用新的设置
            Alert restartAlert = new Alert(Alert.AlertType.INFORMATION);
            restartAlert.setTitle("提示");
            restartAlert.setHeaderText(null);
            restartAlert.setContentText("字号设置已保存，需要重启应用程序才能生效。");
            restartAlert.showAndWait();
        } catch (IOException e) {
            showErrorMessage("保存设置失败: " + e.getMessage());
        }
    }

    /**
     * 取消设置
     */
    @FXML
    private void cancelSettings() {
        // 关闭设置窗口
        settingsTabPane.getScene().getWindow().hide();
    }

    /**
     * 显示成功消息
     */
    private void showSuccessMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("成功");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 显示错误消息
     */
    private void showErrorMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 选择备份文件
     * 
     * @return 选择的备份文件，用户取消则返回null
     */
    private File chooseBackupFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择备份文件");

        // 设置初始目录为备份路径
        String backupPath = getConfigValue("backup.path", "backup/");
        File backupDir = new File(backupPath);
        if (backupDir.exists() && backupDir.isDirectory()) {
            fileChooser.setInitialDirectory(backupDir);
        }

        // 设置文件过滤器，只显示SQL和ZIP文件
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("备份文件", "*.sql", "*.zip"),
                new FileChooser.ExtensionFilter("SQL文件", "*.sql"),
                new FileChooser.ExtensionFilter("ZIP文件", "*.zip"));

        // 显示文件选择对话框
        return fileChooser.showOpenDialog(settingsTabPane.getScene().getWindow());
    }

    /**
     * 显示恢复确认对话框
     * 
     * @return 用户确认返回true，取消返回false
     */
    private boolean confirmRestore() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("恢复确认");
        alert.setHeaderText("警告：恢复操作将覆盖现有数据！");
        alert.setContentText("确定要继续恢复备份吗？此操作不可撤销，将覆盖当前数据库中的所有数据。");

        ButtonType confirmButton = new ButtonType("确定恢复");
        ButtonType cancelButton = new ButtonType("取消", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        // 显示对话框并获取用户选择
        return alert.showAndWait().filter(response -> response == confirmButton).isPresent();
    }
}