package com.ceams.ui.controller;

import com.ceams.client.RemoteApiClient;
import com.ceams.client.ClientSession;
import com.ceams.model.Pagination;
import com.ceams.model.entity.PaymentAccount;
import com.ceams.model.entity.PaymentTransaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FXPaymentTransactionController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private ComboBox<PaymentAccount> accountComboBox;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private TextField descriptionTextField;

    @FXML
    private TableView<PaymentTransaction> transactionTable;
    @FXML
    private TableColumn<PaymentTransaction, Integer> idColumn;
    @FXML
    private TableColumn<PaymentTransaction, String> accountNameColumn;
    @FXML
    private TableColumn<PaymentTransaction, String> typeColumn;
    @FXML
    private TableColumn<PaymentTransaction, Number> amountColumn;
    @FXML
    private TableColumn<PaymentTransaction, String> transactionDateColumn;
    @FXML
    private TableColumn<PaymentTransaction, String> descriptionColumn;
    @FXML
    private TableColumn<PaymentTransaction, Integer> receiptIdColumn;

    @FXML
    private Button queryButton;
    @FXML
    private Button resetButton;
    @FXML
    private Button summaryButton;
    @FXML
    private Button exportButton;
    @FXML
    private ComboBox<Integer> pageSizeComboBox;
    @FXML
    private Button firstPageButton;
    @FXML
    private Button prevPageButton;
    @FXML
    private Label currentPageLabel;
    @FXML
    private Button nextPageButton;
    @FXML
    private Button lastPageButton;

    private ObservableList<PaymentTransaction> transactionList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Pagination pagination;
    private int currentPage = 1;
    private int totalRecords = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        System.out.println("FXPaymentTransactionController.initialize() 开始执行");

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        accountNameColumn.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setCellFactory(col -> new TableCell<PaymentTransaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item.equals("income")) {
                        setText("收入");
                    } else if (item.equals("expense")) {
                        setText("支出");
                    } else {
                        setText(item);
                    }
                }
            }
        });
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        transactionDateColumn.setCellValueFactory(cellData -> {
            Date date = cellData.getValue().getTransactionDate();
            return new javafx.beans.property.SimpleStringProperty(date != null ? dateFormat.format(date) : "");
        });
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        receiptIdColumn.setCellValueFactory(new PropertyValueFactory<>("receiptId"));

        loadAccounts();
        loadTransactionTypes();

        ObservableList<Integer> pageSizes = FXCollections.observableArrayList(10, 20, 50, 100);
        pageSizeComboBox.setItems(pageSizes);
        pageSizeComboBox.setValue(10);

        startDatePicker.setValue(null);
        endDatePicker.setValue(null);

        pagination = new Pagination(currentPage, pageSizeComboBox.getValue());

        queryButton.setOnAction(event -> handleQuery());
        resetButton.setOnAction(event -> handleReset());
        summaryButton.setOnAction(event -> handleSummary());
        exportButton.setOnAction(event -> handleExport());

        firstPageButton.setOnAction(event -> goToFirstPage());
        prevPageButton.setOnAction(event -> goToPrevPage());
        nextPageButton.setOnAction(event -> goToNextPage());
        lastPageButton.setOnAction(event -> goToLastPage());

        pageSizeComboBox.setOnAction(event -> {
            pagination.setPageSize(pageSizeComboBox.getValue());
            currentPage = 1;
            handleQuery();
        });

        handleQuery();
        updatePaginationButtons();
    }

    private void loadTransactionTypes() {
        ObservableList<String> types = FXCollections.observableArrayList();
        types.add("全部");
        types.add("收入");
        types.add("支出");
        typeComboBox.setItems(types);
        typeComboBox.setValue("全部");
    }

    private void loadAccounts() {
        try {
            PaymentAccount[] accounts = apiClient.get("/api/payment/accounts", PaymentAccount[].class);
            List<PaymentAccount> accountList = Arrays.asList(accounts);
            ObservableList<PaymentAccount> accountItems = FXCollections.observableArrayList(accountList);
            accountComboBox.setItems(accountItems);
            accountComboBox.setCellFactory(param -> new ListCell<PaymentAccount>() {
                @Override
                protected void updateItem(PaymentAccount item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
            accountComboBox.setButtonCell(new ListCell<PaymentAccount>() {
                @Override
                protected void updateItem(PaymentAccount item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("全部账户");
                    } else {
                        setText(item.getName());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleQuery() {
        try {
            System.out.println("开始查询交易记录");

            PaymentAccount selectedAccount = accountComboBox.getValue();
            Integer accountId = selectedAccount != null ? selectedAccount.getId() : null;

            String type = typeComboBox.getValue();
            if (type != null) {
                if (type.equals("全部")) {
                    type = null;
                } else if (type.equals("收入")) {
                    type = "income";
                } else if (type.equals("支出")) {
                    type = "expense";
                }
            }

            String description = descriptionTextField.getText();
            if (description != null && description.trim().isEmpty()) {
                description = null;
            }

            // Build query params
            Map<String, String> params = new HashMap<>();
            params.put("pageNum", String.valueOf(currentPage));
            params.put("pageSize", String.valueOf(pageSizeComboBox.getValue()));
            if (accountId != null)
                params.put("accountId", String.valueOf(accountId));
            if (type != null)
                params.put("type", type);
            if (description != null)
                params.put("description", description);
            if (startDatePicker.getValue() != null)
                params.put("startDate", startDatePicker.getValue().toString());
            if (endDatePicker.getValue() != null)
                params.put("endDate", endDatePicker.getValue().toString());

            // Call API
            PaymentTransaction[] transactions = apiClient.get("/api/payment/transactions", params,
                    PaymentTransaction[].class);
            List<PaymentTransaction> list = Arrays.asList(transactions);

            transactionList = FXCollections.observableArrayList(list);
            transactionTable.setItems(transactionList);

            totalRecords = list.size();
            totalPages = (int) Math.ceil((double) totalRecords / pagination.getPageSize());
            if (totalPages == 0)
                totalPages = 1;
            currentPageLabel.setText(currentPage + " / " + totalPages);

            updatePaginationButtons();
            transactionTable.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "查询交易记录失败: " + e.getMessage());
        }
    }

    private void updatePaginationButtons() {
        firstPageButton.setDisable(currentPage == 1);
        prevPageButton.setDisable(currentPage == 1);
        nextPageButton.setDisable(currentPage >= totalPages);
        lastPageButton.setDisable(currentPage >= totalPages);
    }

    private void goToFirstPage() {
        currentPage = 1;
        handleQuery();
    }

    private void goToPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            handleQuery();
        }
    }

    private void goToNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            handleQuery();
        }
    }

    private void goToLastPage() {
        currentPage = totalPages;
        handleQuery();
    }

    private void handleReset() {
        accountComboBox.getSelectionModel().clearSelection();
        typeComboBox.setValue("全部");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        descriptionTextField.clear();
        currentPage = 1;
        pagination.setPageNum(currentPage);
        handleQuery();
    }

    private void handleExport() {
        try {
            System.out.println("开始导出收支交易记录");

            PaymentAccount selectedAccount = accountComboBox.getValue();
            Integer accountId = selectedAccount != null ? selectedAccount.getId() : null;

            String type = typeComboBox.getValue();
            if (type != null) {
                if (type.equals("全部")) {
                    type = null;
                } else if (type.equals("收入")) {
                    type = "income";
                } else if (type.equals("支出")) {
                    type = "expense";
                }
            }

            String description = descriptionTextField.getText();
            if (description != null && description.trim().isEmpty()) {
                description = null;
            }

            Map<String, String> params = new HashMap<>();
            if (accountId != null)
                params.put("accountId", String.valueOf(accountId));
            if (type != null)
                params.put("type", type);
            if (startDatePicker.getValue() != null)
                params.put("startDate", startDatePicker.getValue().toString());
            if (endDatePicker.getValue() != null)
                params.put("endDate", endDatePicker.getValue().toString());
            if (description != null)
                params.put("description", description);

            byte[] reportBytes = apiClient.get("/api/reports/export/transaction/excel", params, byte[].class);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存收支交易记录Excel");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel文件 (*.xlsx)", "*.xlsx"));
            fileChooser.setInitialFileName("收支交易记录_" + System.currentTimeMillis() + ".xlsx");
            Stage stage = (Stage) exportButton.getScene().getWindow();
            java.io.File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(reportBytes);
                }
                showAlert(Alert.AlertType.INFORMATION, "成功", "收支交易记录导出成功！\n文件路径: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "导出收支交易记录失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSummary() {
        try {
            PaymentAccount selectedAccount = accountComboBox.getValue();
            Integer accountId = selectedAccount != null ? selectedAccount.getId() : null;

            Map<String, String> params = new HashMap<>();
            if (accountId != null)
                params.put("accountId", String.valueOf(accountId));
            if (startDatePicker.getValue() != null)
                params.put("startDate", startDatePicker.getValue().toString());
            if (endDatePicker.getValue() != null)
                params.put("endDate", endDatePicker.getValue().toString());

            Map<String, Object> summary = apiClient.get("/api/payment/transactions/summary", params, Map.class);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("汇总信息");
            alert.setHeaderText(selectedAccount != null ? "账户: " + selectedAccount.getName() : "所有账户");

            BigDecimal totalIncome = summary.get("total_income") != null
                    ? new BigDecimal(String.valueOf(summary.get("total_income")))
                    : BigDecimal.ZERO;
            BigDecimal totalExpense = summary.get("total_expense") != null
                    ? new BigDecimal(String.valueOf(summary.get("total_expense")))
                    : BigDecimal.ZERO;
            BigDecimal netAmount = summary.get("net_amount") != null
                    ? new BigDecimal(String.valueOf(summary.get("net_amount")))
                    : BigDecimal.ZERO;

            String content = "收入: " + totalIncome + "\n" +
                    "支出: " + totalExpense + "\n" +
                    "净收入: " + netAmount;

            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "获取汇总信息失败: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}