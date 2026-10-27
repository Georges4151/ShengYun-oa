package com.ceams.ui.controller;

import com.ceams.client.RemoteApiClient;
import com.ceams.client.ClientSession;
import com.ceams.model.entity.BankStatement;
import com.ceams.model.entity.PaymentAccount;
import com.ceams.model.entity.ReconciliationResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FXBankReconciliationController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private AnchorPane bankReconciliationPane;

    @FXML
    private ComboBox<PaymentAccount> accountComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Button uploadButton;

    @FXML
    private Button reconcileButton;

    @FXML
    private Button exportButton;

    @FXML
    private TableView<BankStatement> bankStatementTable;

    @FXML
    private TableColumn<BankStatement, Date> bsDateColumn;

    @FXML
    private TableColumn<BankStatement, String> bsAmountColumn;

    @FXML
    private TableColumn<BankStatement, String> bsTypeColumn;

    @FXML
    private TableColumn<BankStatement, String> bsDescriptionColumn;

    @FXML
    private TableColumn<BankStatement, String> bsStatusColumn;

    @FXML
    private TableView<ReconciliationResult> reconciliationTable;

    @FXML
    private TableColumn<ReconciliationResult, String> rrStatusColumn;

    @FXML
    private TableColumn<ReconciliationResult, String> rrDiscrepancyColumn;

    @FXML
    private TableColumn<ReconciliationResult, String> rrBankAmountColumn;

    @FXML
    private TableColumn<ReconciliationResult, String> rrSystemAmountColumn;

    @FXML
    private TableColumn<ReconciliationResult, Date> rrBankDateColumn;

    @FXML
    private TableColumn<ReconciliationResult, Date> rrSystemDateColumn;

    @FXML
    private TextArea analysisTextArea;

    @FXML
    private ComboBox<String> bankFormatComboBox;

    private ObservableList<BankStatement> bankStatementList = FXCollections.observableArrayList();
    private ObservableList<ReconciliationResult> reconciliationResultList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeComponents();
        loadAccounts();
        loadBankFormats();
    }

    private void initializeComponents() {
        bsDateColumn.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        bsAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        bsTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        bsDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        bsStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        rrStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        rrDiscrepancyColumn.setCellValueFactory(new PropertyValueFactory<>("discrepancyType"));
        rrBankAmountColumn.setCellValueFactory(new PropertyValueFactory<>("bankAmount"));
        rrSystemAmountColumn.setCellValueFactory(new PropertyValueFactory<>("systemAmount"));
        rrBankDateColumn.setCellValueFactory(new PropertyValueFactory<>("bankDate"));
        rrSystemDateColumn.setCellValueFactory(new PropertyValueFactory<>("systemDate"));

        bankStatementTable.setItems(bankStatementList);
        reconciliationTable.setItems(reconciliationResultList);
    }

    private void loadAccounts() {
        try {
            PaymentAccount[] accounts = apiClient.get("/api/payment/accounts", PaymentAccount[].class);
            accountComboBox.setItems(FXCollections.observableArrayList(Arrays.asList(accounts)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBankFormats() {
        List<String> formats = List.of("ICBC", "ABC", "CCB", "BOC", "Generic");
        bankFormatComboBox.setItems(FXCollections.observableArrayList(formats));
        bankFormatComboBox.setValue("Generic");
    }

    @FXML
    private void handleUploadButtonAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择银行流水文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV文件", "*.csv"),
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"));

        File file = fileChooser.showOpenDialog(bankReconciliationPane.getScene().getWindow());
        if (file != null) {
            PaymentAccount selectedAccount = accountComboBox.getValue();
            if (selectedAccount == null) {
                showAlert(Alert.AlertType.ERROR, "错误", "请选择账户");
                return;
            }

            try {
                Map<String, String> params = new HashMap<>();
                params.put("accountId", String.valueOf(selectedAccount.getId()));
                params.put("format", bankFormatComboBox.getValue());

                Map<String, Object> result = apiClient.uploadFile(
                        "/api/payment/bank-statements/upload",
                        file, "file", params, Map.class);

                if (result != null && result.get("count") != null) {
                    showAlert(Alert.AlertType.INFORMATION, "成功", "成功导入 " + result.get("count") + " 条银行流水记录");
                    loadBankStatements(selectedAccount.getId());
                } else {
                    showAlert(Alert.AlertType.WARNING, "警告", "未解析到银行流水记录");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "错误", "导入银行流水失败: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleReconcileButtonAction(ActionEvent event) {
        PaymentAccount selectedAccount = accountComboBox.getValue();
        if (selectedAccount == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择账户");
            return;
        }

        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择日期范围");
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("accountId", selectedAccount.getId());
            body.put("startDate", startDatePicker.getValue().toString());
            body.put("endDate", endDatePicker.getValue().toString());

            @SuppressWarnings("unchecked")
            Map<String, Object> reconciliationResult = apiClient.post("/api/payment/reconciliation",
                    body, Map.class);

            displayReconciliationResults(reconciliationResult);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "执行对账失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        PaymentAccount selectedAccount = accountComboBox.getValue();
        if (selectedAccount == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择账户");
            return;
        }

        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择日期范围");
            return;
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("accountId", String.valueOf(selectedAccount.getId()));
            params.put("startDate", startDatePicker.getValue().toString());
            params.put("endDate", endDatePicker.getValue().toString());

            byte[] exportData = apiClient.get("/api/payment/reconciliation/export", params, byte[].class);

            if (exportData != null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("保存对账结果");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
                fileChooser.setInitialFileName(
                        "bank_reconciliation_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx");

                File file = fileChooser.showSaveDialog(bankReconciliationPane.getScene().getWindow());
                if (file != null) {
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                        fos.write(exportData);
                    }
                    showAlert(Alert.AlertType.INFORMATION, "成功", "对账结果已导出");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "导出对账结果失败: " + e.getMessage());
        }
    }

    private void loadBankStatements(Integer accountId) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("accountId", String.valueOf(accountId));
            BankStatement[] statements = apiClient.get("/api/payment/bank-statements", params, BankStatement[].class);
            bankStatementList.setAll(Arrays.asList(statements));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void displayReconciliationResults(Map<String, Object> result) {
        Object bankStatementsObj = result.get("bankStatements");
        if (bankStatementsObj instanceof List<?>) {
            List<?> list = (List<?>) bankStatementsObj;
            List<BankStatement> bankStatements = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof BankStatement) {
                    bankStatements.add((BankStatement) item);
                }
            }
            bankStatementList.setAll(bankStatements);
        }

        Object reconciliationResultsObj = result.get("reconciliationResults");
        if (reconciliationResultsObj instanceof List<?>) {
            List<?> list = (List<?>) reconciliationResultsObj;
            List<ReconciliationResult> reconciliationResults = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof ReconciliationResult) {
                    reconciliationResults.add((ReconciliationResult) item);
                }
            }
            reconciliationResultList.setAll(reconciliationResults);
        }

        Object analysisObj = result.get("analysis");
        if (analysisObj instanceof Map<?, ?>) {
            displayAnalysis((Map<String, Object>) analysisObj);
        }
    }

    private void displayAnalysis(Map<String, Object> analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("对账分析结果\n");
        sb.append("====================\n");
        sb.append("总记录数: " + analysis.get("totalRecords") + "\n");
        sb.append("已匹配: " + analysis.get("matchedRecords") + "\n");
        sb.append("未匹配: " + analysis.get("unmatchedRecords") + "\n");
        sb.append("部分匹配: " + analysis.get("partiallyMatchedRecords") + "\n");
        sb.append("\n差异分析:\n");
        sb.append("银行缺失: " + analysis.get("missingInBank") + "\n");
        sb.append("系统缺失: " + analysis.get("missingInSystem") + "\n");
        sb.append("金额差异: " + analysis.get("amountMismatch") + "\n");
        sb.append("日期差异: " + analysis.get("dateMismatch") + "\n");
        sb.append("描述差异: " + analysis.get("descriptionMismatch") + "\n");
        sb.append("\n金额分析:\n");
        sb.append("银行总金额: " + analysis.get("totalBankAmount") + "\n");
        sb.append("系统总金额: " + analysis.get("totalSystemAmount") + "\n");
        sb.append("金额差异: " + analysis.get("amountDifference") + "\n");

        analysisTextArea.setText(sb.toString());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleAccountSelection(ActionEvent event) {
        PaymentAccount selectedAccount = accountComboBox.getValue();
        if (selectedAccount != null) {
            loadBankStatements(selectedAccount.getId());
        }
    }

    @FXML
    private void handleManualMatch(ActionEvent event) {
        BankStatement selectedStatement = bankStatementTable.getSelectionModel().getSelectedItem();
        if (selectedStatement == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择银行流水记录");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "提示", "手动匹配功能开发中");
    }

    @FXML
    private void handleMarkAsResolved(ActionEvent event) {
        ReconciliationResult selectedResult = reconciliationTable.getSelectionModel().getSelectedItem();
        if (selectedResult == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择对账结果记录");
            return;
        }

        try {
            Map<String, String> body = new HashMap<>();
            body.put("resolution", "手动处理");
            apiClient.put("/api/payment/reconciliation/" + selectedResult.getId() + "/resolve", body, Void.class);
            showAlert(Alert.AlertType.INFORMATION, "成功", "已标记差异为已处理");

            PaymentAccount selectedAccount = accountComboBox.getValue();
            if (selectedAccount != null && startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                Map<String, Object> reconBody = new HashMap<>();
                reconBody.put("accountId", selectedAccount.getId());
                reconBody.put("startDate", startDatePicker.getValue().toString());
                reconBody.put("endDate", endDatePicker.getValue().toString());
                @SuppressWarnings("unchecked")
                Map<String, Object> result = apiClient.post("/api/payment/reconciliation", reconBody, Map.class);
                displayReconciliationResults(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "操作失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateTransaction(ActionEvent event) {
        BankStatement selectedStatement = bankStatementTable.getSelectionModel().getSelectedItem();
        if (selectedStatement == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择银行流水记录");
            return;
        }

        try {
            apiClient.post("/api/payment/reconciliation/" + selectedStatement.getId() + "/create-transaction",
                    null, Void.class);
            showAlert(Alert.AlertType.INFORMATION, "成功", "已从银行流水创建系统交易");
            selectedStatement.setStatus("reconciled");
            bankStatementTable.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "操作失败: " + e.getMessage());
        }
    }
}