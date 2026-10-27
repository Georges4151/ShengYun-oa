package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Notification;
import com.ceams.model.entity.UserNotification;
import com.ceams.model.entity.UserNotificationPreference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 通知中心控制器（远程版本）
 * 通过HTTP API与后端通信，管理用户通知
 * 
 * @author Soko Coder
 * @version 1.0
 */
public class FXNotificationController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(FXNotificationController.class);
    
    // ==================== 依赖注入 ====================
    
    private RemoteApiClient apiClient;
    private ClientSession clientSession;
    
    /**
     * 设置远程API客户端
     * 
     * @param apiClient RemoteApiClient实例
     */
    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    /**
     * 设置客户端会话
     * 
     * @param clientSession ClientSession实例
     */
    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }
    
    // ==================== UI组件 ====================
    
    @FXML
    private Label unreadCountLabel;
    
    @FXML
    private CheckBox selectAllCheckBox;
    
    @FXML
    private VBox notificationListContainer;
    
    // ==================== 数据 ====================
    
    private List<UserNotification> notifications = new ArrayList<>();
    private List<CheckBox> notificationCheckBoxes = new ArrayList<>();
    
    // ==================== 初始化 ====================
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadNotifications();
    }
    
    /**
     * 刷新数据（由外部调用）
     */
    public void refreshData() {
        Platform.runLater(() -> {
            loadNotifications();
        });
    }
    
    // ==================== 数据加载 ====================
    
    /**
     * 加载通知列表（通过HTTP GET /api/notifications/user）
     */
    private void loadNotifications() {
        if (notificationListContainer == null) {
            logger.debug("notificationListContainer尚未初始化，跳过通知列表加载");
            return;
        }
        
        if (apiClient == null) {
            logger.debug("apiClient尚未注入，使用默认单例");
            apiClient = RemoteApiClient.getInstance();
        }
        
        try {
            // 调用后端API获取用户通知列表
            UserNotification[] responseArray = apiClient.get(
                "/api/notifications/user",
                UserNotification[].class
            );
            
            if (responseArray != null) {
                notifications = new ArrayList<>();
                for (UserNotification un : responseArray) {
                    notifications.add(un);
                }
            } else {
                notifications = new ArrayList<>();
            }
            
            updateUnreadCount();
            renderNotifications();
            
        } catch (IOException e) {
            logger.error("加载通知列表失败: {}", e.getMessage(), e);
            showEmptyState("加载通知失败");
            showErrorAlert("加载失败", "无法从服务器加载通知数据：" + e.getMessage());
        }
    }
    
    /**
     * 更新未读通知数量（通过HTTP GET /api/notifications/user/unread-count）
     */
    private void updateUnreadCount() {
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }
        
        try {
            Integer count = apiClient.get(
                "/api/notifications/user/unread-count",
                Integer.class
            );
            
            if (count != null && unreadCountLabel != null) {
                unreadCountLabel.setText(count + " 条未读");
            }
        } catch (IOException e) {
            logger.error("获取未读数量失败: {}", e.getMessage(), e);
        }
    }
    
    // ==================== UI渲染 ====================
    
    /**
     * 渲染通知列表
     */
    private void renderNotifications() {
        if (notificationListContainer == null) {
            return;
        }
        
        notificationListContainer.getChildren().clear();
        notificationCheckBoxes.clear();
        
        if (notifications == null || notifications.isEmpty()) {
            showEmptyState("暂无通知");
            return;
        }
        
        for (UserNotification userNotification : notifications) {
            HBox notificationItem = createNotificationItem(userNotification);
            notificationListContainer.getChildren().add(notificationItem);
        }
    }
    
    /**
     * 创建单个通知项UI
     * 
     * @param userNotification 用户通知对象
     * @return HBox通知项组件
     */
    private HBox createNotificationItem(UserNotification userNotification) {
        HBox item = new HBox(16);
        item.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); " +
                "-fx-cursor: hand;");
        
        Notification notification = userNotification.getNotification();
        if (notification == null) {
            return item;
        }
        
        // 复选框
        CheckBox checkBox = new CheckBox();
        checkBox.setUserData(userNotification.getNotificationId());
        notificationCheckBoxes.add(checkBox);
        item.getChildren().add(checkBox);
        
        // 内容区域
        VBox contentBox = new VBox(8);
        contentBox.setStyle("-fx-alignment: center-left;");
        HBox.setHgrow(contentBox, javafx.scene.layout.Priority.ALWAYS);
        
        HBox headerBox = new HBox(12);
        headerBox.setStyle("-fx-alignment: center-left;");
        
        // 标题
        Label titleLabel = new Label(notification.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2D3748;");
        titleLabel.setWrapText(true);
        
        // 未读标记
        if (!userNotification.getIsRead()) {
            Label unreadBadge = new Label("未读");
            unreadBadge.setStyle("-fx-background-color: #E53E3E; -fx-text-fill: white; " +
                    "-fx-padding: 2 8; -fx-background-radius: 4px; -fx-font-size: 11px;");
            headerBox.getChildren().add(unreadBadge);
        }
        
        // 优先级标记
        Label priorityBadge = createPriorityBadge(notification.getPriority());
        headerBox.getChildren().add(priorityBadge);
        
        // 类型标记
        Label typeLabel = new Label(getTypeText(notification.getNotificationType()));
        typeLabel.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #4A5568; " +
                "-fx-padding: 2 8; -fx-background-radius: 4px; -fx-font-size: 11px;");
        headerBox.getChildren().add(typeLabel);
        
        contentBox.getChildren().add(headerBox);
        contentBox.getChildren().add(titleLabel);
        
        // 内容
        if (notification.getContent() != null && !notification.getContent().isEmpty()) {
            Label contentLabel = new Label(notification.getContent());
            contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A5568;");
            contentLabel.setWrapText(true);
            contentBox.getChildren().add(contentLabel);
        }
        
        // 发布时间
        if (notification.getPublishTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            Label timeLabel = new Label(notification.getPublishTime().format(formatter));
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");
            contentBox.getChildren().add(timeLabel);
        }
        
        item.getChildren().add(contentBox);
        
        // 操作按钮区域
        VBox actionBox = new VBox(8);
        actionBox.setStyle("-fx-alignment: center;");
        
        // 标为已读按钮（仅未读时显示）
        if (!userNotification.getIsRead()) {
            Button readButton = new Button("标为已读");
            readButton.setStyle("-fx-background-color: #4299E1; -fx-text-fill: white; " +
                    "-fx-padding: 6 12; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-cursor: hand;");
            readButton.setOnAction(e -> markAsRead(userNotification.getNotificationId()));
            actionBox.getChildren().add(readButton);
        }
        
        // 删除按钮
        Button deleteButton = new Button("删除");
        deleteButton.setStyle("-fx-background-color: #E53E3E; -fx-text-fill: white; " +
                "-fx-padding: 6 12; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-cursor: hand;");
        deleteButton.setOnAction(e -> deleteNotification(userNotification.getNotificationId()));
        actionBox.getChildren().add(deleteButton);
        
        item.getChildren().add(actionBox);
        
        // 点击整个项标记为已读
        item.setOnMouseClicked(e -> {
            if (e.getTarget() != checkBox && !userNotification.getIsRead()) {
                markAsRead(userNotification.getNotificationId());
            }
        });
        
        return item;
    }
    
    /**
     * 创建优先级标记
     * 
     * @param priority 优先级字符串
     * @return Label标记组件
     */
    private Label createPriorityBadge(String priority) {
        Label badge = new Label(getPriorityText(priority));
        String color;
        switch (priority != null ? priority : "LOW") {
            case "HIGH":
                color = "#E53E3E";
                break;
            case "MEDIUM":
                color = "#ED8936";
                break;
            case "LOW":
            default:
                color = "#48BB78";
                break;
        }
        badge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-padding: 2 8; -fx-background-radius: 4px; -fx-font-size: 11px;");
        return badge;
    }
    
    /**
     * 获取优先级文本
     * 
     * @param priority 优先级字符串
     * @return 中文优先级文本
     */
    private String getPriorityText(String priority) {
        if (priority == null)
            return "低";
        switch (priority) {
            case "HIGH":
                return "高";
            case "MEDIUM":
                return "中";
            case "LOW":
            default:
                return "低";
        }
    }
    
    /**
     * 获取类型文本
     * 
     * @param type 类型字符串
     * @return 中文类型文本
     */
    private String getTypeText(String type) {
        if (type == null)
            return "系统通知";
        switch (type) {
            case "SYSTEM":
                return "系统通知";
            case "BUSINESS":
                return "业务通知";
            case "MARKETING":
                return "营销通知";
            default:
                return type;
        }
    }
    
    /**
     * 显示空状态
     * 
     * @param message 提示信息
     */
    private void showEmptyState(String message) {
        if (notificationListContainer == null) {
            return;
        }
        Label emptyLabel = new Label(message);
        emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14px; -fx-padding: 40 0;");
        notificationListContainer.getChildren().add(emptyLabel);
    }
    
    // ==================== 事件处理 ====================
    
    /**
     * 全选/取消全选
     */
    @FXML
    private void toggleSelectAll() {
        boolean selected = selectAllCheckBox.isSelected();
        for (CheckBox checkBox : notificationCheckBoxes) {
            checkBox.setSelected(selected);
        }
    }
    
    /**
     * 标记所有为已读（通过HTTP POST /api/notifications/user/read-all）
     */
    @FXML
    private void markAllAsRead() {
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }
        
        try {
            apiClient.post("/api/notifications/user/read-all", null, Void.class);
            loadNotifications();
        } catch (IOException e) {
            logger.error("标记全部已读失败: {}", e.getMessage(), e);
            showErrorAlert("操作失败", "标记全部已读失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量标记为已读（通过HTTP POST /api/notifications/user/batch-read）
     */
    @FXML
    private void batchMarkAsRead() {
        List<Integer> selectedIds = getSelectedNotificationIds();
        if (selectedIds.isEmpty()) {
            return;
        }
        
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }
        
        try {
            Map<String, List<Integer>> requestBody = new HashMap<>();
            requestBody.put("notificationIds", selectedIds);
            
            apiClient.post("/api/notifications/user/batch-read", requestBody, Void.class);
            loadNotifications();
        } catch (IOException e) {
            logger.error("批量标记已读失败: {}", e.getMessage(), e);
            showErrorAlert("操作失败", "批量标记已读失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量删除（通过HTTP POST /api/notifications/user/batch-delete）
     */
    @FXML
    private void batchDelete() {
        List<Integer> selectedIds = getSelectedNotificationIds();
        if (selectedIds.isEmpty()) {
            return;
        }
        
        // 确认对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("批量删除通知");
        alert.setContentText("确定要删除选中的 " + selectedIds.size() + " 条通知吗？");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (apiClient == null) {
                apiClient = RemoteApiClient.getInstance();
            }
            
            try {
                Map<String, List<Integer>> requestBody = new HashMap<>();
                requestBody.put("notificationIds", selectedIds);
                
                apiClient.post("/api/notifications/user/batch-delete", requestBody, Void.class);
                loadNotifications();
            } catch (IOException e) {
                logger.error("批量删除失败: {}", e.getMessage(), e);
                showErrorAlert("删除失败", "批量删除失败：" + e.getMessage());
            }
        }
    }
    
    /**
     * 标记单个通知为已读（通过HTTP POST /api/notifications/user/read）
     * 
     * @param notificationId 通知ID
     */
    private void markAsRead(Integer notificationId) {
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }
        
        try {
            Map<String, Integer> requestBody = new HashMap<>();
            requestBody.put("notificationId", notificationId);
            
            apiClient.post("/api/notifications/user/read", requestBody, Void.class);
            loadNotifications();
        } catch (IOException e) {
            logger.error("标记已读失败: {}", e.getMessage(), e);
            showErrorAlert("操作失败", "标记已读失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除单个通知（通过HTTP DELETE /api/notifications/user）
     * 
     * @param notificationId 通知ID
     */
    private void deleteNotification(Integer notificationId) {
        // 确认对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除通知");
        alert.setContentText("确定要删除该通知吗？");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (apiClient == null) {
                apiClient = RemoteApiClient.getInstance();
            }
            
            try {
                Map<String, Integer> requestBody = new HashMap<>();
                requestBody.put("notificationId", notificationId);
                
                // 后端使用POST方法删除通知
                apiClient.post("/api/notifications/user/delete", requestBody, Void.class);
                loadNotifications();
            } catch (IOException e) {
                logger.error("删除通知失败: {}", e.getMessage(), e);
                showErrorAlert("删除失败", "删除通知失败：" + e.getMessage());
            }
        }
    }
    
    /**
     * 获取选中的通知ID列表
     * 
     * @return 通知ID列表
     */
    private List<Integer> getSelectedNotificationIds() {
        List<Integer> ids = new ArrayList<>();
        for (CheckBox checkBox : notificationCheckBoxes) {
            if (checkBox.isSelected()) {
                ids.add((Integer) checkBox.getUserData());
            }
        }
        return ids;
    }
    
    /**
     * 打开通知偏好设置窗口
     */
    @FXML
    private void openPreferences() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/notification/preferences.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("通知偏好设置");
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.setWidth(700);
            stage.setHeight(600);
            stage.showAndWait();
        } catch (Exception e) {
            logger.error("打开偏好设置窗口失败: {}", e.getMessage(), e);
            showErrorAlert("打开窗口失败", "无法打开偏好设置窗口：" + e.getMessage());
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 显示错误提示对话框
     * 
     * @param title 标题
     * @param message 错误信息
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * 创建控制器实例并注入依赖
     * 用于FXML loader的controllerFactory
     * 
     * @param controllerClass 控制器类
     * @return 控制器实例
     */
    private Object createController(Class<?> controllerClass) {
        try {
            Object controller = controllerClass.getDeclaredConstructor().newInstance();
            
            // 注入RemoteApiClient
            try {
                java.lang.reflect.Method setApiClient = controllerClass.getMethod("setApiClient", RemoteApiClient.class);
                setApiClient.invoke(controller, apiClient != null ? apiClient : RemoteApiClient.getInstance());
            } catch (NoSuchMethodException e) {
                // 控制器不需要ApiClient，忽略
            }
            
            // 注入ClientSession
            try {
                java.lang.reflect.Method setClientSession = controllerClass.getMethod("setClientSession", ClientSession.class);
                setClientSession.invoke(controller, clientSession != null ? clientSession : ClientSession.getInstance());
            } catch (NoSuchMethodException e) {
                // 控制器不需要ClientSession，忽略
            }
            
            return controller;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create controller: " + controllerClass.getName(), e);
        }
    }
}
