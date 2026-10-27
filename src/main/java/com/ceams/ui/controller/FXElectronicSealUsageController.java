package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.ElectronicSeal;
import com.ceams.model.entity.ElectronicSealUsage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class FXElectronicSealUsageController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private ComboBox<ElectronicSeal> sealComboBox;

    @FXML
    private TextArea purposeTextArea;

    @FXML
    private TextField documentTypeField;

    @FXML
    private TextField documentPathField;

    @FXML
    private Button useSealButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TableView<ElectronicSealUsage> usageTable;

    @FXML
    private TableColumn<ElectronicSealUsage, String> sealNameColumn;

    @FXML
    private TableColumn<ElectronicSealUsage, String> purposeColumn;

    @FXML
    private TableColumn<ElectronicSealUsage, String> documentTypeColumn;

    @FXML
    private TableColumn<ElectronicSealUsage, String> usageTimeColumn;

    private ObservableList<ElectronicSeal> sealList;
    private ObservableList<ElectronicSealUsage> usageList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化电子章下拉框
        loadSeals();

        // 初始化表格
        initTable();

        // 加载我的使用记录
        loadMyUsages();

        // 设置按钮事件
        useSealButton.setOnAction(event -> useSeal());
        cancelButton.setOnAction(event -> closeWindow());
    }

    private void loadSeals() {
        try {
            ElectronicSeal[] seals = apiClient.get("/api/electronic-seal/seals", ElectronicSeal[].class);
            sealList = FXCollections.observableArrayList();
            if (seals != null) {
                sealList.addAll(Arrays.asList(seals));
            }
            sealComboBox.setItems(sealList);

            // 设置显示的字段
            sealComboBox.setCellFactory(param -> new ListCell<ElectronicSeal>() {
                @Override
                protected void updateItem(ElectronicSeal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getSealName());
                    }
                }
            });

            sealComboBox.setButtonCell(new ListCell<ElectronicSeal>() {
                @Override
                protected void updateItem(ElectronicSeal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getSealName());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTable() {
        sealNameColumn.setCellValueFactory(new PropertyValueFactory<>("sealName"));
        purposeColumn.setCellValueFactory(new PropertyValueFactory<>("purpose"));
        documentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("documentType"));
        usageTimeColumn.setCellValueFactory(new PropertyValueFactory<>("usageTime"));
    }

    private void loadMyUsages() {
        try {
            ElectronicSealUsage[] usages = apiClient.get("/api/electronic-seal/usage/my", ElectronicSealUsage[].class);
            usageList = FXCollections.observableArrayList();
            if (usages != null) {
                usageList.addAll(Arrays.asList(usages));
            }
            usageTable.setItems(usageList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void useSeal() {
        ElectronicSeal selectedSeal = sealComboBox.getValue();
        String purpose = purposeTextArea.getText();
        String documentType = documentTypeField.getText();
        String documentPath = documentPathField.getText();

        if (selectedSeal == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择电子章");
            return;
        }

        if (purpose == null || purpose.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请输入使用目的");
            return;
        }

        if (documentType == null || documentType.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请输入文档类型");
            return;
        }

        if (documentPath == null || documentPath.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请输入文档路径");
            return;
        }

        // 检查是否有使用权限
        try {
            Boolean canUse = apiClient.get("/api/electronic-seal/can-use/" + selectedSeal.getId(), Boolean.class);
            if (canUse == null || !canUse) {
                showAlert(Alert.AlertType.ERROR, "错误", "您没有使用此电子章的权限，请先提交申请并获得批准");
                return;
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "检查权限失败: " + e.getMessage());
            return;
        }

        ElectronicSealUsage usage = new ElectronicSealUsage();
        usage.setSealId(selectedSeal.getId());
        usage.setSealName(selectedSeal.getSealName());
        usage.setPurpose(purpose);
        usage.setDocumentType(documentType);
        usage.setDocumentPath(documentPath);
        usage.setIpAddress("127.0.0.1"); // 实际应用中应获取真实IP

        try {
            apiClient.post("/api/electronic-seal/usage", usage, Void.class);
            showAlert(Alert.AlertType.INFORMATION, "成功", "电子章使用成功");
            loadMyUsages();
            clearForm();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "使用电子章失败: " + e.getMessage());
        }
    }

    private void clearForm() {
        sealComboBox.setValue(null);
        purposeTextArea.clear();
        documentTypeField.clear();
        documentPathField.clear();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}