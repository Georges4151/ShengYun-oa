package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.StampApproval;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ResourceBundle;

public class FXMyStampApprovalController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TableView<StampApproval> approvalTable;

    @FXML
    private TableColumn<StampApproval, String> contractNameColumn;

    @FXML
    private TableColumn<StampApproval, String> sealNameColumn;

    @FXML
    private TableColumn<StampApproval, String> projectNameColumn;

    @FXML
    private TableColumn<StampApproval, String> applyTimeColumn;

    @FXML
    private TableColumn<StampApproval, String> statusColumn;

    @FXML
    private TableColumn<StampApproval, String> approverNameColumn;

    @FXML
    private TableColumn<StampApproval, String> approvalTimeColumn;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label detailContractName;

    @FXML
    private Label detailSealName;

    @FXML
    private Label detailProjectName;

    @FXML
    private Label detailApplyTime;

    @FXML
    private Label detailStatus;

    @FXML
    private Label detailApproverName;

    @FXML
    private Label detailApprovalTime;

    @FXML
    private Label detailRejectReason;

    @FXML
    private Button exportButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button closeButton;

    private ObservableList<StampApproval> approvalList;
    private StampApproval selectedApproval;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTable();
        loadMyApprovals();

        approvalTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showApprovalDetails(newValue));
    }

    private void initTable() {
        contractNameColumn.setCellValueFactory(new PropertyValueFactory<>("contractName"));
        sealNameColumn.setCellValueFactory(new PropertyValueFactory<>("sealName"));
        projectNameColumn.setCellValueFactory(new PropertyValueFactory<>("projectName"));

        applyTimeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getApplyTime() != null) {
                return new SimpleStringProperty(dateFormat.format(cellData.getValue().getApplyTime()));
            }
            return new SimpleStringProperty("-");
        });

        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatusText()));

        approverNameColumn.setCellValueFactory(new PropertyValueFactory<>("approverName"));

        approvalTimeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getApprovalTime() != null) {
                return new SimpleStringProperty(dateFormat.format(cellData.getValue().getApprovalTime()));
            }
            return new SimpleStringProperty("-");
        });
    }

    private void loadMyApprovals() {
        try {
            Integer userId = getCurrentUserId();
            String url = "/api/electronic-seal/applications/my?userId=" + userId;
            StampApproval[] approvals = apiClient.get(url, StampApproval[].class);
            approvalList = FXCollections.observableArrayList(Arrays.asList(approvals));
            approvalTable.setItems(approvalList);
            totalCountLabel.setText("共 " + approvalList.size() + " 条审批记录");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "加载我的审批记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showApprovalDetails(StampApproval approval) {
        if (approval != null) {
            selectedApproval = approval;
            detailContractName.setText(approval.getContractName());
            detailSealName.setText(approval.getSealName());
            detailProjectName.setText(approval.getProjectName());

            if (approval.getApplyTime() != null) {
                detailApplyTime.setText(dateFormat.format(approval.getApplyTime()));
            } else {
                detailApplyTime.setText("-");
            }

            detailStatus.setText(approval.getStatusText());
            detailStatus.setStyle(approval.getStatusStyle() + "-fx-font-weight: bold;");

            detailApproverName.setText(approval.getApproverName() != null ? approval.getApproverName() : "-");

            if (approval.getApprovalTime() != null) {
                detailApprovalTime.setText(dateFormat.format(approval.getApprovalTime()));
            } else {
                detailApprovalTime.setText("-");
            }

            detailRejectReason.setText(approval.getRejectReason() != null ? approval.getRejectReason() : "-");

            if ("approved".equals(approval.getStatus())) {
                exportButton.setDisable(false);
            } else {
                exportButton.setDisable(true);
            }
        } else {
            clearDetails();
        }
    }

    private void clearDetails() {
        selectedApproval = null;
        detailContractName.setText("-");
        detailSealName.setText("-");
        detailProjectName.setText("-");
        detailApplyTime.setText("-");
        detailStatus.setText("-");
        detailStatus.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        detailApproverName.setText("-");
        detailApprovalTime.setText("-");
        detailRejectReason.setText("-");
        exportButton.setDisable(true);
    }

    @FXML
    private void exportContract() {
        if (selectedApproval == null) {
            showAlert(Alert.AlertType.WARNING, "警告", "请先选择要导出的审批记录");
            return;
        }

        if (!"approved".equals(selectedApproval.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "警告", "只有审批通过的合同才能导出");
            return;
        }

        System.out.println("=== 导出合同调试信息 ===");
        System.out.println("审批ID: " + selectedApproval.getId());
        System.out.println("合同名称: " + selectedApproval.getContractName());
        System.out.println("状态: " + selectedApproval.getStatus());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存盖章后的合同");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF文件", "*.pdf"));
        fileChooser.setInitialFileName(selectedApproval.getContractName() + "_盖章.pdf");

        Stage stage = (Stage) exportButton.getScene().getWindow();
        File targetFile = fileChooser.showSaveDialog(stage);

        if (targetFile != null) {
            try {
                // 调用后端API导出合同
                String url = "/api/electronic-seal/applications/" + selectedApproval.getId() + "/export";
                File downloadedFile = apiClient.downloadFile(url, targetFile);

                showAlert(Alert.AlertType.INFORMATION, "成功",
                        "合同导出成功！\n保存位置: " + downloadedFile.getAbsolutePath());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "错误", "导出合同失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void refreshApprovals() {
        loadMyApprovals();
        clearDetails();
        showAlert(Alert.AlertType.INFORMATION, "提示", "审批记录已刷新");
    }

    @FXML
    private void close() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private Integer getCurrentUserId() {
        if (clientSession != null && clientSession.getCurrentUser() != null) {
            return clientSession.getCurrentUser().getUserId();
        }
        return 1;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
