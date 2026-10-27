package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Notification;
import com.ceams.model.entity.UserNotification;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationCenterController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private Label unreadCountLabel;

    @FXML
    private CheckBox selectAllCheckBox;

    @FXML
    private VBox notificationListContainer;

    private List<UserNotification> notifications = new ArrayList<>();
    private List<CheckBox> notificationCheckBoxes = new ArrayList<>();

    @FXML
    public void initialize() {
        loadNotifications();
    }

    private void loadNotifications() {
        try {
            UserNotification[] arr = apiClient.get("/api/notifications/user", UserNotification[].class);
            notifications = new ArrayList<>();
            if (arr != null) {
                notifications.addAll(Arrays.asList(arr));
            }
            updateUnreadCount();
            renderNotifications();
        } catch (Exception e) {
            e.printStackTrace();
            showEmptyState("加载通知失败");
        }
    }

    private void updateUnreadCount() {
        try {
            Integer count = apiClient.get("/api/notifications/user/unread-count", Integer.class);
            unreadCountLabel.setText((count != null ? count : 0) + " 条未读");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderNotifications() {
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

    private HBox createNotificationItem(UserNotification userNotification) {
        HBox item = new HBox(16);
        item.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); " +
                "-fx-cursor: hand;");

        Notification notification = userNotification.getNotification();
        if (notification == null) {
            return item;
        }

        CheckBox checkBox = new CheckBox();
        checkBox.setUserData(userNotification.getNotificationId());
        notificationCheckBoxes.add(checkBox);
        item.getChildren().add(checkBox);

        VBox contentBox = new VBox(8);
        contentBox.setStyle("-fx-alignment: center-left;");
        HBox.setHgrow(contentBox, javafx.scene.layout.Priority.ALWAYS);

        HBox headerBox = new HBox(12);
        headerBox.setStyle("-fx-alignment: center-left;");

        Label titleLabel = new Label(notification.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2D3748;");
        titleLabel.setWrapText(true);

        if (!userNotification.getIsRead()) {
            Label unreadBadge = new Label("未读");
            unreadBadge.setStyle("-fx-background-color: #E53E3E; -fx-text-fill: white; " +
                    "-fx-padding: 2 8; -fx-background-radius: 4px; -fx-font-size: 11px;");
            headerBox.getChildren().add(unreadBadge);
        }

        Label priorityBadge = createPriorityBadge(notification.getPriority());
        headerBox.getChildren().add(priorityBadge);

        Label typeLabel = new Label(getTypeText(notification.getNotificationType()));
        typeLabel.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #4A5568; " +
                "-fx-padding: 2 8; -fx-background-radius: 4px; -fx-font-size: 11px;");
        headerBox.getChildren().add(typeLabel);

        contentBox.getChildren().add(headerBox);
        contentBox.getChildren().add(titleLabel);

        if (notification.getContent() != null && !notification.getContent().isEmpty()) {
            Label contentLabel = new Label(notification.getContent());
            contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A5568;");
            contentLabel.setWrapText(true);
            contentBox.getChildren().add(contentLabel);
        }

        if (notification.getPublishTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            Label timeLabel = new Label(notification.getPublishTime().format(formatter));
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");
            contentBox.getChildren().add(timeLabel);
        }

        item.getChildren().add(contentBox);

        VBox actionBox = new VBox(8);
        actionBox.setStyle("-fx-alignment: center;");

        if (!userNotification.getIsRead()) {
            Button readButton = new Button("标为已读");
            readButton.setStyle("-fx-background-color: #4299E1; -fx-text-fill: white; " +
                    "-fx-padding: 6 12; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-cursor: hand;");
            readButton.setOnAction(e -> markAsRead(userNotification.getNotificationId()));
            actionBox.getChildren().add(readButton);
        }

        Button deleteButton = new Button("删除");
        deleteButton.setStyle("-fx-background-color: #E53E3E; -fx-text-fill: white; " +
                "-fx-padding: 6 12; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-cursor: hand;");
        deleteButton.setOnAction(e -> deleteNotification(userNotification.getNotificationId()));
        actionBox.getChildren().add(deleteButton);

        item.getChildren().add(actionBox);

        item.setOnMouseClicked(e -> {
            if (e.getTarget() != checkBox && !userNotification.getIsRead()) {
                markAsRead(userNotification.getNotificationId());
            }
        });

        return item;
    }

    private Label createPriorityBadge(String priority) {
        Label badge = new Label(getPriorityText(priority));
        String color;
        switch (priority) {
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

    private void showEmptyState(String message) {
        Label emptyLabel = new Label(message);
        emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14px; -fx-padding: 40 0;");
        notificationListContainer.getChildren().add(emptyLabel);
    }

    @FXML
    private void toggleSelectAll() {
        boolean selected = selectAllCheckBox.isSelected();
        for (CheckBox checkBox : notificationCheckBoxes) {
            checkBox.setSelected(selected);
        }
    }

    @FXML
    private void markAllAsRead() {
        try {
            apiClient.postForm("/api/notifications/user/read-all", null, Void.class);
            loadNotifications();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void batchMarkAsRead() {
        try {
            List<Integer> selectedIds = getSelectedNotificationIds();
            if (selectedIds.isEmpty()) {
                return;
            }
            Map<String, List<Integer>> body = new HashMap<>();
            body.put("notificationIds", selectedIds);
            apiClient.post("/api/notifications/user/batch-read", body, Void.class);
            loadNotifications();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void batchDelete() {
        try {
            List<Integer> selectedIds = getSelectedNotificationIds();
            if (selectedIds.isEmpty()) {
                return;
            }
            Map<String, List<Integer>> body = new HashMap<>();
            body.put("notificationIds", selectedIds);
            apiClient.post("/api/notifications/user/batch-delete", body, Void.class);
            loadNotifications();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markAsRead(Integer notificationId) {
        try {
            Map<String, Integer> body = new HashMap<>();
            body.put("notificationId", notificationId);
            apiClient.post("/api/notifications/user/read", body, Void.class);
            loadNotifications();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteNotification(Integer notificationId) {
        try {
            Map<String, Integer> body = new HashMap<>();
            body.put("notificationId", notificationId);
            apiClient.post("/api/notifications/user/delete", body, Void.class);
            loadNotifications();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getSelectedNotificationIds() {
        List<Integer> ids = new ArrayList<>();
        for (CheckBox checkBox : notificationCheckBoxes) {
            if (checkBox.isSelected()) {
                ids.add((Integer) checkBox.getUserData());
            }
        }
        return ids;
    }

    @FXML
    private void openPreferences() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/notification/preferences.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            try {
                java.lang.reflect.Method setApiClient = controller.getClass().getMethod("setApiClient", RemoteApiClient.class);
                setApiClient.invoke(controller, apiClient);
            } catch (NoSuchMethodException ignored) {}
            try {
                java.lang.reflect.Method setClientSession = controller.getClass().getMethod("setClientSession", ClientSession.class);
                setClientSession.invoke(controller, clientSession);
            } catch (NoSuchMethodException ignored) {}

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("通知偏好设置");
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.setWidth(700);
            stage.setHeight(600);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}