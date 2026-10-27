package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Notification;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class NotificationEditorController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private ComboBox<String> priorityComboBox;
    @FXML
    private ComboBox<String> contentTypeComboBox;
    @FXML
    private TextArea contentArea;
    @FXML
    private TextField linkUrlField;
    @FXML
    private DatePicker validStartDatePicker;
    @FXML
    private ComboBox<String> validStartTimeCombo;
    @FXML
    private DatePicker validEndDatePicker;
    @FXML
    private ComboBox<String> validEndTimeCombo;

    private Notification notification;
    private NotificationManagementController managementController;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList("系统通知", "业务通知", "营销通知"));
        priorityComboBox.setItems(FXCollections.observableArrayList("高优先级", "中优先级", "低优先级"));
        contentTypeComboBox.setItems(FXCollections.observableArrayList("纯文本", "HTML格式", "Markdown格式"));

        initTimeComboBoxes();
    }

    private void initTimeComboBoxes() {
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String time = String.format("%02d:%02d", hour, minute);
                validStartTimeCombo.getItems().add(time);
                validEndTimeCombo.getItems().add(time);
            }
        }
        validStartTimeCombo.setValue("00:00");
        validEndTimeCombo.setValue("23:30");
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
        if (notification != null) {
            loadNotificationData();
        }
    }

    public void setManagementController(NotificationManagementController managementController) {
        this.managementController = managementController;
    }

    private void loadNotificationData() {
        titleField.setText(notification.getTitle());
        typeComboBox.setValue(getTypeDisplayName(notification.getNotificationType()));
        priorityComboBox.setValue(getPriorityDisplayName(notification.getPriority()));
        contentTypeComboBox.setValue(getContentTypeDisplayName(notification.getContentType()));
        contentArea.setText(notification.getContent());
        linkUrlField.setText(notification.getLinkUrl());

        if (notification.getValidStartTime() != null) {
            validStartDatePicker.setValue(notification.getValidStartTime().toLocalDate());
            validStartTimeCombo.setValue(
                    notification.getValidStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (notification.getValidEndTime() != null) {
            validEndDatePicker.setValue(notification.getValidEndTime().toLocalDate());
            validEndTimeCombo.setValue(
                    notification.getValidEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    @FXML
    private void save() {
        try {
            if (!validateForm()) {
                return;
            }

            if (notification == null) {
                notification = new Notification();
            }

            notification.setTitle(titleField.getText());
            notification.setNotificationType(getTypeValue(typeComboBox.getValue()));
            notification.setPriority(getPriorityValue(priorityComboBox.getValue()));
            notification.setContentType(getContentTypeValue(contentTypeComboBox.getValue()));
            notification.setContent(contentArea.getText());
            notification.setLinkUrl(linkUrlField.getText());

            if (validStartDatePicker.getValue() != null) {
                LocalTime time = LocalTime.parse(validStartTimeCombo.getValue(), DateTimeFormatter.ofPattern("HH:mm"));
                notification.setValidStartTime(LocalDateTime.of(validStartDatePicker.getValue(), time));
            }
            if (validEndDatePicker.getValue() != null) {
                LocalTime time = LocalTime.parse(validEndTimeCombo.getValue(), DateTimeFormatter.ofPattern("HH:mm"));
                notification.setValidEndTime(LocalDateTime.of(validEndDatePicker.getValue(), time));
            }

            if (notification.getId() == null) {
                Notification result = apiClient.post("/api/notifications", notification, Notification.class);
                showAlert("成功", "通知创建成功", Alert.AlertType.INFORMATION);
            } else {
                apiClient.put("/api/notifications/" + notification.getId(), notification, Void.class);
                showAlert("成功", "通知更新成功", Alert.AlertType.INFORMATION);
            }

            if (managementController != null) {
                managementController.refresh();
            }

            cancel();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "保存失败：" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void cancel() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private boolean validateForm() {
        if (titleField.getText().isEmpty()) {
            showAlert("错误", "请填写标题", Alert.AlertType.WARNING);
            return false;
        }
        if (contentArea.getText().isEmpty()) {
            showAlert("错误", "请填写内容", Alert.AlertType.WARNING);
            return false;
        }
        if (typeComboBox.getValue() == null) {
            showAlert("错误", "请选择类型", Alert.AlertType.WARNING);
            return false;
        }
        if (priorityComboBox.getValue() == null) {
            showAlert("错误", "请选择优先级", Alert.AlertType.WARNING);
            return false;
        }
        if (contentTypeComboBox.getValue() == null) {
            showAlert("错误", "请选择内容类型", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    private String getTypeDisplayName(String type) {
        if (type == null)
            return null;
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

    private String getTypeValue(String displayName) {
        if (displayName == null)
            return null;
        switch (displayName) {
            case "系统通知":
                return "SYSTEM";
            case "业务通知":
                return "BUSINESS";
            case "营销通知":
                return "MARKETING";
            default:
                return displayName;
        }
    }

    private String getPriorityDisplayName(String priority) {
        if (priority == null)
            return null;
        switch (priority) {
            case "HIGH":
                return "高优先级";
            case "MEDIUM":
                return "中优先级";
            case "LOW":
                return "低优先级";
            default:
                return priority;
        }
    }

    private String getPriorityValue(String displayName) {
        if (displayName == null)
            return null;
        switch (displayName) {
            case "高优先级":
                return "HIGH";
            case "中优先级":
                return "MEDIUM";
            case "低优先级":
                return "LOW";
            default:
                return displayName;
        }
    }

    private String getContentTypeDisplayName(String contentType) {
        if (contentType == null)
            return null;
        switch (contentType) {
            case "TEXT":
                return "纯文本";
            case "HTML":
                return "HTML格式";
            case "MARKDOWN":
                return "Markdown格式";
            default:
                return contentType;
        }
    }

    private String getContentTypeValue(String displayName) {
        if (displayName == null)
            return null;
        switch (displayName) {
            case "纯文本":
                return "TEXT";
            case "HTML格式":
                return "HTML";
            case "Markdown格式":
                return "MARKDOWN";
            default:
                return displayName;
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}