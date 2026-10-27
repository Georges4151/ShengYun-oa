package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.StampApproval;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FXStampApprovalController implements Initializable {

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
    private TableColumn<StampApproval, String> sealCodeColumn;

    @FXML
    private TableColumn<StampApproval, String> projectNameColumn;

    @FXML
    private TableColumn<StampApproval, String> applicantNameColumn;

    @FXML
    private TableColumn<StampApproval, String> applyTimeColumn;

    @FXML
    private TableColumn<StampApproval, String> statusColumn;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label detailContractName;

    @FXML
    private Label detailSealName;

    @FXML
    private Label detailSealCode;

    @FXML
    private Label detailSealPermission;

    @FXML
    private Label detailProjectName;

    @FXML
    private Label detailApplicantName;

    @FXML
    private Label detailApplyTime;

    @FXML
    private Button previewButton;

    @FXML
    private Button approveButton;

    @FXML
    private Button rejectButton;

    @FXML
    private Button closeButton;

    @FXML
    private VBox rejectReasonBox;

    @FXML
    private TextArea rejectReasonTextArea;

    private ObservableList<StampApproval> approvalList;
    private StampApproval selectedApproval;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTable();
        loadPendingApprovals();
        checkApprovalPermission();

        approvalTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showApprovalDetails(newValue));
    }

    private void checkApprovalPermission() {
        boolean hasPermission = hasApprovalPermission();

        approveButton.setVisible(hasPermission);
        approveButton.setManaged(hasPermission);
        rejectButton.setVisible(hasPermission);
        rejectButton.setManaged(hasPermission);

        if (!hasPermission) {
            approveButton.setDisable(true);
            rejectButton.setDisable(true);
        }
    }

    private boolean hasApprovalPermission() {
        if (clientSession == null || clientSession.getCurrentUser() == null) {
            return false;
        }
        String role = clientSession.getCurrentUser().getRole();
        if (role == null) {
            return false;
        }
        String roleLower = role.toLowerCase();
        return "manager".equals(roleLower) || "admin".equals(roleLower);
    }

    private void initTable() {
        contractNameColumn.setCellValueFactory(new PropertyValueFactory<>("contractName"));
        sealNameColumn.setCellValueFactory(new PropertyValueFactory<>("sealName"));
        sealCodeColumn.setCellValueFactory(new PropertyValueFactory<>("sealCode"));
        projectNameColumn.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        applicantNameColumn.setCellValueFactory(new PropertyValueFactory<>("applicantName"));

        applyTimeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getApplyTime() != null) {
                return new SimpleStringProperty(dateFormat.format(cellData.getValue().getApplyTime()));
            }
            return new SimpleStringProperty("-");
        });

        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatusText()));
    }

    private void loadPendingApprovals() {
        try {
            StampApproval[] approvals = apiClient.get("/api/electronic-seal/applications/pending", StampApproval[].class);
            approvalList = FXCollections.observableArrayList(Arrays.asList(approvals));
            approvalTable.setItems(approvalList);
            totalCountLabel.setText("共 " + approvalList.size() + " 条待审批记录");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "加载待审批记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showApprovalDetails(StampApproval approval) {
        if (approval != null) {
            selectedApproval = approval;
            detailContractName.setText(approval.getContractName());
            detailSealName.setText(approval.getSealName());
            detailSealCode.setText(approval.getSealCode() != null ? approval.getSealCode() : "-");
            detailSealPermission.setText(approval.getSealPermission() != null ? approval.getSealPermission() : "-");
            detailProjectName.setText(approval.getProjectName());
            detailApplicantName.setText(approval.getApplicantName());

            if (approval.getApplyTime() != null) {
                detailApplyTime.setText(dateFormat.format(approval.getApplyTime()));
            } else {
                detailApplyTime.setText("-");
            }

            approveButton.setDisable(false);
            rejectButton.setDisable(false);
            previewButton.setDisable(false);
        } else {
            clearDetails();
        }
    }

    private void clearDetails() {
        selectedApproval = null;
        detailContractName.setText("-");
        detailSealName.setText("-");
        detailSealCode.setText("-");
        detailSealPermission.setText("-");
        detailProjectName.setText("-");
        detailApplicantName.setText("-");
        detailApplyTime.setText("-");
        approveButton.setDisable(true);
        rejectButton.setDisable(true);
        previewButton.setDisable(true);
    }

    @FXML
    private void previewContract() {
        if (selectedApproval == null) {
            showAlert(Alert.AlertType.WARNING, "警告", "请先选择要预览的审批记录");
            return;
        }

        File file = null;
        String status = selectedApproval.getStatus();
        String titlePrefix = "";

        try {
            if ("approved".equals(status)) {
                // 优先从stamped_contracts目录查找已盖章的文件
                String contractPath = null;
                String originalPath = selectedApproval.getOriginalContractPath();

                if (originalPath != null && !originalPath.isEmpty()) {
                    File originalFile = new File(originalPath);
                    String fileName = originalFile.getName();
                    String stampedPath = "./stamped_contracts/" + fileName;
                    File stampedFile = new File(stampedPath);

                    if (stampedFile.exists()) {
                        contractPath = stampedPath;
                        System.out.println("找到已盖章文件: " + contractPath);
                    }
                }

                // 如果没找到盖章文件，尝试使用contractPath
                if (contractPath == null || contractPath.isEmpty()) {
                    contractPath = selectedApproval.getContractPath();
                }

                // 如果还是没有，使用原始文件
                if (contractPath == null || contractPath.isEmpty()) {
                    contractPath = originalPath;
                }

                if (contractPath == null || contractPath.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "错误", "合同文件路径为空");
                    return;
                }
                file = new File(contractPath);
                file = addWatermarkToFile(file, selectedApproval.getId());
                if (file == null) {
                    file = new File(contractPath);
                }

                titlePrefix = "正式盖章合同";
            } else if ("rejected".equals(status)) {
                String contractPath = selectedApproval.getOriginalContractPath();
                if (contractPath == null || contractPath.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "错误", "原始合同文件路径为空");
                    return;
                }
                file = new File(contractPath);
                titlePrefix = "原始合同（已驳回）";
            } else {
                // 尝试从后端下载预览文件
                try {
                    String downloadPath = "/api/electronic-seal/applications/" + selectedApproval.getId() + "/preview";
                    String tempDir = "./temp_previews";
                    new File(tempDir).mkdirs();
                    String previewFilePath = tempDir + "/preview_" + selectedApproval.getId() + "_"
                            + System.currentTimeMillis() + ".pdf";
                    file = new File(previewFilePath);
                    file = apiClient.downloadFile(downloadPath, file);
                    titlePrefix = "盖章预览";
                } catch (Exception e) {
                    System.err.println("下载预览文件失败: " + e.getMessage());
                    String contractPath = selectedApproval.getOriginalContractPath();
                    if (contractPath == null || contractPath.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "错误", "无法生成预览文件，原始合同路径为空");
                        return;
                    }
                    file = new File(contractPath);
                    titlePrefix = "原始合同";
                }

                final File previewFile = file;
                file = addWatermarkToFile(previewFile, selectedApproval.getId());
                if (file == null) {
                    file = previewFile;
                }
            }

            if (!file.exists()) {
                showAlert(Alert.AlertType.ERROR, "错误", "合同文件不存在: " + file.getAbsolutePath());
                return;
            }

            URL fxmlUrl = getClass().getResource("/fxml/electronic-seal/contract-preview.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "错误", "无法找到预览界面配置文件");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            FXContractPreviewController controller = loader.getController();
            controller.setApiClient(apiClient);
            controller.setClientSession(clientSession);
            controller.setContractFile(file, selectedApproval.getContractName());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(titlePrefix + " - " + selectedApproval.getContractName());
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root, 900, 700));
            stage.setResizable(true);
            stage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "无法预览合同文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File addWatermarkToFile(File sourceFile, Integer approvalId) {
        try {
            String username = "未知用户";
            if (clientSession != null && clientSession.getCurrentUser() != null) {
                username = clientSession.getCurrentUser().getName();
                if (username == null || username.isEmpty()) {
                    username = clientSession.getCurrentUser().getUsername();
                }
            }

            String tempDir = "./temp_previews";
            new File(tempDir).mkdirs();
            String watermarkedFilePath = tempDir + "/preview_watermarked_" + approvalId + "_"
                    + System.currentTimeMillis() + ".pdf";

            // 通过远程API添加水印
            Map<String, String> params = new HashMap<>();
            params.put("username", username);
            File watermarkedFile = apiClient.uploadFile(
                    "/api/electronic-seal/watermark/add",
                    sourceFile,
                    "file",
                    params,
                    File.class
            );

            if (watermarkedFile != null && watermarkedFile.exists()) {
                System.out.println("已添加水印: " + watermarkedFile.getAbsolutePath());
                return watermarkedFile;
            } else {
                System.out.println("水印添加失败，将使用原始文件");
                return null;
            }
        } catch (Exception e) {
            System.err.println("添加水印失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void approveStamp() {
        if (!hasApprovalPermission()) {
            showAlert(Alert.AlertType.ERROR, "权限不足", "您没有审批权限，只有经理或管理员才能审批！");
            return;
        }

        if (selectedApproval == null) {
            showAlert(Alert.AlertType.WARNING, "警告", "请先选择要审批的记录");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认审批");
        confirmAlert.setHeaderText("确认通过该盖章申请？");
        confirmAlert.setContentText("合同名称：" + selectedApproval.getContractName() +
                "\n电子章：" + selectedApproval.getSealName() +
                "\n申请人：" + selectedApproval.getApplicantName());

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            try {
                String url = "/api/electronic-seal/applications/" + selectedApproval.getId() + "/approve";
                Map<String, Object> body = new HashMap<>();
                body.put("approverId", clientSession.getCurrentUser().getUserId());
                body.put("approverName", clientSession.getCurrentUser().getName());
                apiClient.post(url, body, Void.class);

                showAlert(Alert.AlertType.INFORMATION, "成功", "审批通过！");

                loadPendingApprovals();
                clearDetails();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "错误", "审批失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void rejectStamp() {
        if (!hasApprovalPermission()) {
            showAlert(Alert.AlertType.ERROR, "权限不足", "您没有审批权限，只有经理或管理员才能审批！");
            return;
        }

        if (selectedApproval == null) {
            showAlert(Alert.AlertType.WARNING, "警告", "请先选择要审批的记录");
            return;
        }

        rejectReasonBox.setVisible(true);
        rejectReasonBox.setManaged(true);
        rejectReasonTextArea.clear();
    }

    @FXML
    private void confirmReject() {
        String rejectReason = rejectReasonTextArea.getText().trim();
        if (rejectReason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "警告", "请输入驳回原因");
            return;
        }

        try {
            String url = "/api/electronic-seal/applications/" + selectedApproval.getId() + "/reject";
            Map<String, Object> body = new HashMap<>();
            body.put("approverId", clientSession.getCurrentUser().getUserId());
            body.put("approverName", clientSession.getCurrentUser().getName());
            body.put("rejectReason", rejectReason);
            apiClient.post(url, body, Void.class);

            showAlert(Alert.AlertType.INFORMATION, "成功", "已驳回该申请");

            cancelReject();
            loadPendingApprovals();
            clearDetails();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "驳回失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void cancelReject() {
        rejectReasonBox.setVisible(false);
        rejectReasonBox.setManaged(false);
        rejectReasonTextArea.clear();
    }

    @FXML
    private void close() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
