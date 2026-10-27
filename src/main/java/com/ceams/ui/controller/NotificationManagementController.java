package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Notification;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class NotificationManagementController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<String> typeFilter;
    @FXML
    private VBox notificationListContainer;
    @FXML
    private Button prevPageBtn;
    @FXML
    private Button nextPageBtn;
    @FXML
    private Label pageInfoLabel;
    @FXML
    private Label totalCountLabel;

    private List<Notification> notifications;
    private int currentPage = 1;
    private int pageSize = 10;
    private String currentStatus = "全部状态";
    private String currentType = "全部类型";
    private String currentSearch = "";

    @FXML
    public void initialize() {
        initFilters();
        loadNotifications();
    }

    private void initFilters() {
        statusFilter.setItems(FXCollections.observableArrayList("全部状态", "草稿", "已发布", "已归档"));
        statusFilter.setValue("全部状态");
        typeFilter.setItems(FXCollections.observableArrayList("全部类型", "系统通知", "业务通知", "营销通知"));
        typeFilter.setValue("全部类型");
    }

    private void loadNotifications() {
        try {
            Notification[] arr = apiClient.get("/api/notifications", Notification[].class);
            notifications = new java.util.ArrayList<>();
            if (arr != null) {
                notifications.addAll(Arrays.asList(arr));
            }
            filterAndRenderNotifications();
        } catch (Exception e) {
            e.printStackTrace();
            showEmptyState("加载通知失败");
        }
    }

    private void filterAndRenderNotifications() {
        List<Notification> filtered = notifications;

        if (!currentStatus.equals("全部状态")) {
            String statusValue = currentStatus.equals("草稿") ? "DRAFT"
                    : currentStatus.equals("已发布") ? "PUBLISHED"
                            : currentStatus.equals("已归档") ? "ARCHIVED" : currentStatus;
            final String status = statusValue;
            filtered = filtered.stream()
                    .filter(n -> n.getStatus().equals(status))
                    .toList();
        }

        if (!currentType.equals("全部类型")) {
            String typeValue = currentType.equals("系统通知") ? "SYSTEM"
                    : currentType.equals("业务通知") ? "BUSINESS" : currentType.equals("营销通知") ? "MARKETING" : currentType;
            final String type = typeValue;
            filtered = filtered.stream()
                    .filter(n -> n.getNotificationType().equals(type))
                    .toList();
        }

        if (!currentSearch.isEmpty()) {
            String search = currentSearch.toLowerCase();
            filtered = filtered.stream()
                    .filter(n -> n.getTitle().toLowerCase().contains(search) ||
                            n.getContent().toLowerCase().contains(search))
                    .toList();
        }

        totalCountLabel.setText("共 " + filtered.size() + " 条通知");
        renderNotifications(filtered);
        updatePagination(filtered.size());
    }

    private void renderNotifications(List<Notification> filtered) {
        notificationListContainer.getChildren().clear();

        if (filtered.isEmpty()) {
            showEmptyState("暂无通知");
            return;
        }

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, filtered.size());
        List<Notification> pageNotifications = filtered.subList(start, end);

        for (Notification notification : pageNotifications) {
            HBox item = createNotificationItem(notification);
            notificationListContainer.getChildren().add(item);
        }
    }

    private HBox createNotificationItem(Notification notification) {
        HBox item = new HBox(20);
        item.setStyle(
                "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2); -fx-cursor: hand;");

        VBox leftBox = new VBox(12);
        leftBox.setStyle("-fx-flex-grow: 1;");

        HBox headerBox = new HBox(12);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label(notification.getTitle());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2D3748;");

        HBox statusBadge = new HBox();
        statusBadge.setStyle("-fx-padding: 4 12; -fx-background-radius: 16px;");
        Label statusLabel = new Label(notification.getStatus().equals("DRAFT") ? "草稿"
                : notification.getStatus().equals("PUBLISHED") ? "已发布" : "已归档");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        if (notification.getStatus().equals("DRAFT")) {
            statusBadge.setStyle(statusBadge.getStyle() + "-fx-background-color: #FEF3C7;");
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-text-fill: #92400E;");
        } else if (notification.getStatus().equals("PUBLISHED")) {
            statusBadge.setStyle(statusBadge.getStyle() + "-fx-background-color: #D1FAE5;");
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-text-fill: #065F46;");
        } else {
            statusBadge.setStyle(statusBadge.getStyle() + "-fx-background-color: #FEE2E2;");
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-text-fill: #991B1B;");
        }
        statusBadge.getChildren().add(statusLabel);

        HBox typeBadge = new HBox();
        typeBadge.setStyle("-fx-padding: 4 12; -fx-background-radius: 16px; -fx-background-color: #E0E7FF;");
        Label typeLabel = new Label(notification.getNotificationType().equals("SYSTEM") ? "系统"
                : notification.getNotificationType().equals("BUSINESS") ? "业务" : "营销");
        typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3730A3;");
        typeBadge.getChildren().add(typeLabel);

        HBox priorityBadge = new HBox();
        priorityBadge.setStyle("-fx-padding: 4 12; -fx-background-radius: 16px;");
        Label priorityLabel = new Label(notification.getPriority().equals("HIGH") ? "高优先级"
                : notification.getPriority().equals("MEDIUM") ? "中优先级" : "低优先级");
        priorityLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        if (notification.getPriority().equals("HIGH")) {
            priorityBadge.setStyle(priorityBadge.getStyle() + "-fx-background-color: #FEE2E2;");
            priorityLabel.setStyle(priorityLabel.getStyle() + "-fx-text-fill: #DC2626;");
        } else if (notification.getPriority().equals("MEDIUM")) {
            priorityBadge.setStyle(priorityBadge.getStyle() + "-fx-background-color: #FEF3C7;");
            priorityLabel.setStyle(priorityLabel.getStyle() + "-fx-text-fill: #D97706;");
        } else {
            priorityBadge.setStyle(priorityBadge.getStyle() + "-fx-background-color: #DBEAFE;");
            priorityLabel.setStyle(priorityLabel.getStyle() + "-fx-text-fill: #2563EB;");
        }
        priorityBadge.getChildren().add(priorityLabel);

        headerBox.getChildren().addAll(titleLabel, statusBadge, typeBadge, priorityBadge);

        HBox metaBox = new HBox(24);
        metaBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");
        metaBox.getChildren().addAll(
                new Label("\uD83D\uDC64 " + (notification.getSenderName() != null ? notification.getSenderName() : "系统")),
                new Label("\uD83D\uDCC5 " + formatDateTime(notification.getCreatedAt())),
                new Label("\u23F0 " + (notification.getValidEndTime() != null
                        ? "有效期至 " + formatDateTime(notification.getValidEndTime())
                        : "永久有效")));

        Label contentLabel = new Label(notification.getContent());
        contentLabel.setStyle("-fx-text-fill: #4A5568; -fx-wrap-text: true; -fx-font-size: 14px;");
        contentLabel.setMaxWidth(800);

        leftBox.getChildren().addAll(headerBox, metaBox, contentLabel);

        VBox actionBox = new VBox(10);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER);

        if (notification.getStatus().equals("DRAFT")) {
            Button editBtn = new Button("\u270F\uFE0F 编辑");
            editBtn.setOnAction(e -> editNotification(notification));
            editBtn.setStyle(
                    "-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-font-size: 13px; -fx-cursor: hand;");

            Button publishBtn = new Button("\uD83D\uDE80 发布");
            publishBtn.setOnAction(e -> publishNotification(notification));
            publishBtn.setStyle(
                    "-fx-background-color: #10B981; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-font-size: 13px; -fx-cursor: hand;");

            Button deleteBtn = new Button("\uD83D\uDDD1\uFE0F 删除");
            deleteBtn.setOnAction(e -> deleteNotification(notification));
            deleteBtn.setStyle(
                    "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-font-size: 13px; -fx-cursor: hand;");

            actionBox.getChildren().addAll(editBtn, publishBtn, deleteBtn);
        } else if (notification.getStatus().equals("PUBLISHED")) {
            Button archiveBtn = new Button("\uD83D\uDCE6 归档");
            archiveBtn.setOnAction(e -> archiveNotification(notification));
            archiveBtn.setStyle(
                    "-fx-background-color: #6B7280; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-font-size: 13px; -fx-cursor: hand;");
            actionBox.getChildren().add(archiveBtn);
        }

        item.getChildren().addAll(leftBox, actionBox);
        return item;
    }

    @FXML
    private void createNotification() {
        openNotificationEditor(null);
    }

    private void editNotification(Notification notification) {
        openNotificationEditor(notification);
    }

    private void publishNotification(Notification notification) {
        try {
            apiClient.postForm("/api/notifications/" + notification.getId() + "/publish", null, Void.class);
            showAlert("成功", "通知发布成功", Alert.AlertType.INFORMATION);
            loadNotifications();
        } catch (Exception e) {
            showAlert("错误", "发布失败：" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void archiveNotification(Notification notification) {
        try {
            apiClient.postForm("/api/notifications/" + notification.getId() + "/archive", null, Void.class);
            showAlert("成功", "通知归档成功", Alert.AlertType.INFORMATION);
            loadNotifications();
        } catch (Exception e) {
            showAlert("错误", "归档失败：" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void deleteNotification(Notification notification) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确定要删除这条通知吗？");
        alert.setContentText("删除后无法恢复。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    apiClient.delete("/api/notifications/" + notification.getId(), Void.class);
                    showAlert("成功", "通知删除成功", Alert.AlertType.INFORMATION);
                    loadNotifications();
                } catch (Exception e) {
                    showAlert("错误", "删除失败：" + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void openNotificationEditor(Notification notification) {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/fxml/notification/editor.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            NotificationEditorController controller = loader.getController();
            controller.setApiClient(apiClient);
            controller.setClientSession(clientSession);
            controller.setNotification(notification);
            controller.setManagementController(this);

            Stage stage = new Stage();
            stage.setTitle(notification == null ? "创建通知" : "编辑通知");
            stage.setScene(new Scene(root, 900, 700));
            stage.initModality(Modality.APPLICATION_MODAL);
            WindowUtils.setStageIcon(stage);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "打开编辑器失败", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void filterNotifications() {
        currentStatus = statusFilter.getValue();
        currentType = typeFilter.getValue();
        currentSearch = searchField.getText();
        currentPage = 1;
        filterAndRenderNotifications();
    }

    @FXML
    private void refreshNotifications() {
        loadNotifications();
    }

    @FXML
    private void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            filterAndRenderNotifications();
        }
    }

    @FXML
    private void nextPage() {
        int totalPages = (notifications.size() + pageSize - 1) / pageSize;
        if (currentPage < totalPages) {
            currentPage++;
            filterAndRenderNotifications();
        }
    }

    private void updatePagination(int total) {
        int totalPages = (total + pageSize - 1) / pageSize;
        pageInfoLabel.setText("第 " + currentPage + " 页 / 共 " + totalPages + " 页");
        prevPageBtn.setDisable(currentPage == 1);
        nextPageBtn.setDisable(currentPage >= totalPages);
    }

    private void showEmptyState(String message) {
        notificationListContainer.getChildren().clear();
        VBox emptyBox = new VBox(16);
        emptyBox.setAlignment(javafx.geometry.Pos.CENTER);
        emptyBox.setStyle("-fx-padding: 60 0;");

        Label iconLabel = new Label("\uD83D\uDCED");
        iconLabel.setStyle("-fx-font-size: 48px;");

        Label emptyLabel = new Label(message);
        emptyLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 16px;");

        emptyBox.getChildren().addAll(iconLabel, emptyLabel);
        notificationListContainer.getChildren().add(emptyBox);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return "-";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }

    public void refresh() {
        loadNotifications();
    }
}