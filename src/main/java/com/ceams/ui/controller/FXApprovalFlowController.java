package com.ceams.ui.controller;

import com.ceams.CeamsApplication;
import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.ApprovalRecord;
import com.ceams.model.entity.Receipt;
import com.ceams.model.entity.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FXApprovalFlowController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private ComboBox<String> approvalLevelComboBox;

    @FXML
    private TableView<Receipt> receiptTable;

    @FXML
    private TextField receiptIdField;

    @FXML
    private TextField reporterNameField;

    @FXML
    private TextField projectNameField;

    @FXML
    private TextField receiptDateField;

    @FXML
    private TextField amountField;

    @FXML
    private TextField typeField;

    @FXML
    private TextField descriptionField;

    @FXML
    private Label financeStatusLabel;

    @FXML
    private Label financeInfoLabel;

    @FXML
    private Label projectManagerStatusLabel;

    @FXML
    private Label projectManagerInfoLabel;

    @FXML
    private Label managerStatusLabel;

    @FXML
    private Label managerInfoLabel;

    @FXML
    private ComboBox<String> actionComboBox;

    @FXML
    private Label currentLevelLabel;

    @FXML
    private TextArea commentTextArea;

    @FXML
    private TableView<ApprovalRecord> historyTable;

    private Receipt selectedReceipt;
    private ObservableList<Receipt> receiptList = FXCollections.observableArrayList();
    private ObservableList<ApprovalRecord> historyList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        approvalLevelComboBox.setItems(FXCollections.observableArrayList(
                "全部待审", "财务审核", "项目经理审核", "经理审核"));
        approvalLevelComboBox.setValue("全部待审");

        actionComboBox.setItems(FXCollections.observableArrayList(
                "通过", "拒绝"));
        actionComboBox.setValue("通过");

        loadPendingReceipts();
        initHistoryTable();
    }

    private void initHistoryTable() {
        historyTable.setItems(historyList);
    }

    @FXML
    private void onApprovalLevelChanged() {
        loadPendingReceipts();
    }

    private void loadPendingReceipts() {
        try {
            String selectedLevel = approvalLevelComboBox.getValue();
            Receipt[] receiptsArray;

            if ("全部待审".equals(selectedLevel)) {
                receiptsArray = apiClient.get("/api/receipts/pending-approvals", Receipt[].class);
            } else {
                int level = 0;
                if ("财务审核".equals(selectedLevel)) {
                    level = 0;
                } else if ("项目经理审核".equals(selectedLevel)) {
                    level = 1;
                } else if ("经理审核".equals(selectedLevel)) {
                    level = 2;
                }
                Map<String, String> params = new HashMap<>();
                params.put("level", String.valueOf(level));
                receiptsArray = apiClient.get("/api/receipts/pending-approvals", params, Receipt[].class);
            }

            List<Receipt> receipts = Arrays.asList(receiptsArray);

            receiptList.clear();
            for (Receipt r : receipts) {
                Integer currentLevel = r.getCurrentApprovalLevel();
                if (currentLevel == null) {
                    currentLevel = 0;
                }
                if (currentLevel < 3) {
                    receiptList.add(r);
                }
            }
            receiptTable.setItems(receiptList);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "加载待审核票据失败: " + e.getMessage());
        }
    }

    @FXML
    private void onReceiptSelected() {
        selectedReceipt = receiptTable.getSelectionModel().getSelectedItem();
        if (selectedReceipt != null) {
            displayReceiptDetails();
            loadApprovalHistory();
        }
    }

    private void displayReceiptDetails() {
        if (selectedReceipt == null) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        if (receiptIdField != null) {
            receiptIdField.setText(String.valueOf(selectedReceipt.getId()));
        }
        if (reporterNameField != null) {
            reporterNameField
                    .setText(selectedReceipt.getReporterName() != null ? selectedReceipt.getReporterName() : "");
        }
        if (projectNameField != null) {
            projectNameField.setText(selectedReceipt.getProjectName() != null ? selectedReceipt.getProjectName() : "");
        }
        if (receiptDateField != null) {
            receiptDateField.setText(
                    selectedReceipt.getReceiptDate() != null ? sdf.format(selectedReceipt.getReceiptDate()) : "");
        }
        if (amountField != null) {
            amountField.setText(selectedReceipt.getAmount() != null ? selectedReceipt.getAmount().toString() : "");
        }
        if (typeField != null) {
            typeField.setText(selectedReceipt.getType() != null ? selectedReceipt.getType() : "");
        }
        if (descriptionField != null) {
            descriptionField.setText(selectedReceipt.getDescription() != null ? selectedReceipt.getDescription() : "");
        }

        Integer currentLevel = selectedReceipt.getCurrentApprovalLevel();
        if (currentLevel == null) {
            currentLevel = 0;
        }

        if (financeStatusLabel != null) {
            financeStatusLabel.setText(
                    selectedReceipt.getFinanceStatusText() != null ? selectedReceipt.getFinanceStatusText() : "待审核");
        }
        if (financeInfoLabel != null) {
            financeInfoLabel.setText(
                    selectedReceipt.getFinanceVerifiedAt() != null ? sdf.format(selectedReceipt.getFinanceVerifiedAt())
                            : "");
        }

        if (projectManagerStatusLabel != null) {
            projectManagerStatusLabel.setText(selectedReceipt.getProjectManagerStatusText() != null
                    ? selectedReceipt.getProjectManagerStatusText()
                    : "待审核");
        }
        if (projectManagerInfoLabel != null) {
            projectManagerInfoLabel.setText(selectedReceipt.getProjectManagerVerifiedAt() != null
                    ? sdf.format(selectedReceipt.getProjectManagerVerifiedAt())
                    : "");
        }

        if (managerStatusLabel != null) {
            managerStatusLabel.setText(
                    selectedReceipt.getManagerStatusText() != null ? selectedReceipt.getManagerStatusText() : "待审核");
        }
        if (managerInfoLabel != null) {
            managerInfoLabel.setText(
                    selectedReceipt.getManagerVerifiedAt() != null ? sdf.format(selectedReceipt.getManagerVerifiedAt())
                            : "");
        }

        String levelText = "";
        switch (currentLevel) {
            case 0:
                levelText = "当前审核级别: 财务审核";
                break;
            case 1:
                levelText = "当前审核级别: 项目经理审核";
                break;
            case 2:
                levelText = "当前审核级别: 经理审核";
                break;
            default:
                levelText = "审核完成";
        }
        if (currentLevelLabel != null) {
            currentLevelLabel.setText(levelText);
        }
    }

    private void loadApprovalHistory() {
        if (selectedReceipt != null) {
            try {
                String url = "/api/receipts/" + selectedReceipt.getId() + "/approval-history";
                ApprovalRecord[] recordsArray = apiClient.get(url, ApprovalRecord[].class);
                List<ApprovalRecord> records = Arrays.asList(recordsArray);
                historyList.clear();
                historyList.addAll(records);
                historyTable.setItems(historyList);
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("错误", "加载审批历史失败: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onApprove() {
        if (selectedReceipt == null) {
            showAlert("提示", "请先选择要审核的票据");
            return;
        }

        User currentUser = clientSession.getCurrentUser();
        if (currentUser == null) {
            showAlert("错误", "未登录，无法执行审批操作");
            return;
        }

        String action = actionComboBox.getValue();
        String comment = commentTextArea.getText();

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("approverId", currentUser.getUserId());
            requestBody.put("approverName", currentUser.getName());
            requestBody.put("comment", comment);

            String url;
            if ("通过".equals(action)) {
                url = "/api/receipts/" + selectedReceipt.getId() + "/approve";
            } else {
                url = "/api/receipts/" + selectedReceipt.getId() + "/reject";
            }

            apiClient.post(url, requestBody, Void.class);

            showAlert("成功", "审核提交成功");
            commentTextArea.clear();
            loadPendingReceipts();

            // 刷新当前票据详情
            selectedReceipt = apiClient.get("/api/receipts/" + selectedReceipt.getId(), Receipt.class);
            if (selectedReceipt != null) {
                displayReceiptDetails();
                loadApprovalHistory();
            }
        } catch (IOException e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg.contains("403")) {
                showAlert("权限不足", "您没有权限审核此票据");
            } else {
                showAlert("失败", "审核提交失败: " + errorMsg);
            }
        }
    }

    @FXML
    private void onViewImage() {
        if (selectedReceipt == null) {
            showAlert("提示", "请先选择票据");
            return;
        }

        String filePath = selectedReceipt.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            showAlert("提示", "该票据没有图片文件");
            return;
        }

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file = new File("." + filePath);
            }
            if (!file.exists()) {
                showAlert("错误", "图片文件不存在: " + filePath);
                return;
            }

            Image image = new Image(file.toURI().toString());
            Stage stage = new Stage();
            stage.setTitle("票据图片 - ID: " + selectedReceipt.getId());

            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);

            Scene scene = new Scene(new javafx.scene.layout.StackPane(imageView));
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "加载图片失败: " + e.getMessage());
        }
    }

    @FXML
    private void onViewHistory() {
        if (selectedReceipt == null) {
            showAlert("提示", "请先选择票据");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/receipt/approval_history_detail.fxml"));
            loader.setControllerFactory(com.ceams.CeamsApplication.getContext()::getBean);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("审核历史详情");
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.setWidth(800);
            stage.setHeight(600);

            FXApprovalHistoryDetailController controller = loader.getController();
            controller.setReceipt(selectedReceipt);

            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "打开审核历史窗口失败: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
