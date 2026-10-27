package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.ElectronicSealApplication;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.ResourceBundle;

public class FXElectronicSealApprovalController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TableView<ElectronicSealApplication> applicationTable;

    @FXML
    private TableColumn<ElectronicSealApplication, String> applicantNameColumn;

    @FXML
    private TableColumn<ElectronicSealApplication, String> sealNameColumn;

    @FXML
    private TableColumn<ElectronicSealApplication, String> purposeColumn;

    @FXML
    private TableColumn<ElectronicSealApplication, String> applyTimeColumn;

    @FXML
    private TextArea commentTextArea;

    @FXML
    private Button approveButton;

    @FXML
    private Button rejectButton;

    @FXML
    private Button cancelButton;

    private ObservableList<ElectronicSealApplication> applicationList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化表格
        initTable();

        // 加载待审批的申请
        loadPendingApplications();

        // 设置按钮事件
        approveButton.setOnAction(event -> approveApplication());
        rejectButton.setOnAction(event -> rejectApplication());
        cancelButton.setOnAction(event -> closeWindow());
    }

    private void initTable() {
        applicantNameColumn.setCellValueFactory(new PropertyValueFactory<>("applicantName"));
        sealNameColumn.setCellValueFactory(new PropertyValueFactory<>("sealName"));
        purposeColumn.setCellValueFactory(new PropertyValueFactory<>("purpose"));
        applyTimeColumn.setCellValueFactory(new PropertyValueFactory<>("applyTime"));
    }

    private void loadPendingApplications() {
        try {
            ElectronicSealApplication[] applications = apiClient.get("/api/electronic-seal/applications/pending", ElectronicSealApplication[].class);
            applicationList = FXCollections.observableArrayList();
            if (applications != null) {
                applicationList.addAll(Arrays.asList(applications));
            }
            applicationTable.setItems(applicationList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void approveApplication() {
        ElectronicSealApplication selectedApplication = applicationTable.getSelectionModel().getSelectedItem();
        if (selectedApplication == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择要审批的申请");
            return;
        }

        String comment = commentTextArea.getText();

        try {
            String encodedComment = URLEncoder.encode(comment != null ? comment : "", "UTF-8");
            apiClient.put("/api/electronic-seal/applications/" + selectedApplication.getId() + "/approve?comment=" + encodedComment, null, Void.class);
            showAlert(Alert.AlertType.INFORMATION, "成功", "申请已批准");
            loadPendingApplications();
            clearForm();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "审批失败: " + e.getMessage());
        }
    }

    private void rejectApplication() {
        ElectronicSealApplication selectedApplication = applicationTable.getSelectionModel().getSelectedItem();
        if (selectedApplication == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择要审批的申请");
            return;
        }

        String comment = commentTextArea.getText();

        try {
            String encodedComment = URLEncoder.encode(comment != null ? comment : "", "UTF-8");
            apiClient.put("/api/electronic-seal/applications/" + selectedApplication.getId() + "/reject?comment=" + encodedComment, null, Void.class);
            showAlert(Alert.AlertType.INFORMATION, "成功", "申请已拒绝");
            loadPendingApplications();
            clearForm();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "审批失败: " + e.getMessage());
        }
    }

    private void clearForm() {
        commentTextArea.clear();
        applicationTable.getSelectionModel().clearSelection();
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