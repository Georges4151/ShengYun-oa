package com.ceams.ui.controller;

import com.ceams.client.RemoteApiClient;
import com.ceams.client.ClientSession;
import com.ceams.model.entity.Receipt;
import com.ceams.model.entity.ReceiptVerification;
import com.ceams.model.entity.PaymentAccount;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class FXVerifyPaymentController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    private Receipt receipt;

    @FXML
    private TextField receiptIdField;

    @FXML
    private TextField reporterNameField;

    @FXML
    private TextField projectNameField;

    @FXML
    private TextField amountField;

    @FXML
    private TextField paidAmountField;

    @FXML
    private TextField remainingAmountField;

    @FXML
    private TextField verificationAmountField;

    @FXML
    private ComboBox<PaymentAccount> paymentAccountComboBox;

    @FXML
    private TextField verifiedByField;

    @FXML
    private DatePicker verificationTimePicker;

    @FXML
    private TextArea remarkTextArea;

    @FXML
    private TableView<ReceiptVerification> verificationTable;

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (verificationTimePicker != null) {
            verificationTimePicker.setValue(java.time.LocalDate.now());
        }
    }

    public void initData() {
        loadPaymentAccounts();
        if (verificationTable != null) {
            initTable();
        }
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
        fillReceiptInfo();
        if (verificationTable != null) {
            initTable();
        }
        loadVerificationRecords();
    }

    private void fillReceiptInfo() {
        if (receipt != null) {
            receiptIdField.setText(String.valueOf(receipt.getId()));
            reporterNameField.setText(receipt.getReporterName() != null ? receipt.getReporterName() : "");
            projectNameField.setText(receipt.getProjectName() != null ? receipt.getProjectName() : "");
            amountField.setText(receipt.getAmount() != null ? receipt.getAmount().toString() : "0.00");
            paidAmountField.setText(receipt.getPaidAmount() == null ? "0.00" : receipt.getPaidAmount().toString());
            remainingAmountField
                    .setText(receipt.getRemainingAmount() != null ? receipt.getRemainingAmount().toString() : "0.00");
        }
    }

    private void loadPaymentAccounts() {
        try {
            PaymentAccount[] paymentAccounts = apiClient.get("/api/payment/accounts", PaymentAccount[].class);
            List<PaymentAccount> accountList = Arrays.asList(paymentAccounts);
            ObservableList<PaymentAccount> data = FXCollections.observableArrayList(accountList);
            paymentAccountComboBox.setItems(data);

            paymentAccountComboBox.setCellFactory(param -> new ListCell<PaymentAccount>() {
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

            paymentAccountComboBox.setButtonCell(new ListCell<PaymentAccount>() {
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

            if (!accountList.isEmpty()) {
                paymentAccountComboBox.setValue(accountList.get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("加载支付账户失败: " + e.getMessage());
        }
    }

    private void initTable() {
        ObservableList<TableColumn<ReceiptVerification, ?>> columns = verificationTable.getColumns();

        System.out.println("表格列数量: " + columns.size());
        if (columns.size() > 0) {
            columns.get(0).setCellValueFactory(new PropertyValueFactory<>("amount"));
        }
        if (columns.size() > 1) {
            columns.get(1).setCellValueFactory(new PropertyValueFactory<>("verificationTime"));
            TableColumn<?, ?> column = columns.get(1);
            if (column instanceof TableColumn<?, ?>) {
                @SuppressWarnings("unchecked")
                TableColumn<ReceiptVerification, Date> dateColumn = (TableColumn<ReceiptVerification, Date>) column;
                dateColumn.setCellFactory(col -> {
                    return new TableCell<ReceiptVerification, Date>() {
                        @Override
                        protected void updateItem(Date item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(dateFormat.format(item));
                            }
                        }
                    };
                });
            }
        }
        if (columns.size() > 2) {
            columns.get(2).setCellValueFactory(new PropertyValueFactory<>("paymentMethodText"));
        }
        if (columns.size() > 3) {
            columns.get(3).setCellValueFactory(new PropertyValueFactory<>("paymentAccountName"));
        }
        if (columns.size() > 4) {
            columns.get(4).setCellValueFactory(new PropertyValueFactory<>("verifiedByName"));
        }
        if (columns.size() > 5) {
            columns.get(5).setCellValueFactory(new PropertyValueFactory<>("remark"));
        }
        System.out.println("表格初始化完成");
    }

    private void loadVerificationRecords() {
        if (receipt != null && receipt.getId() != null && verificationTable != null) {
            try {
                System.out.println("开始加载核销记录，票据ID: " + receipt.getId());
                ReceiptVerification[] verifications = apiClient.get(
                        "/api/receipts/" + receipt.getId() + "/verifications",
                        ReceiptVerification[].class);
                List<ReceiptVerification> list = Arrays.asList(verifications);
                System.out.println("获取到的核销记录数量: " + list.size());

                for (ReceiptVerification v : list) {
                    System.out.println(
                            "核销记录: ID=" + v.getId() + ", 金额=" + v.getAmount() + ", 支付方式=" + v.getPaymentMethod());
                }

                final ObservableList<ReceiptVerification> data = FXCollections.observableArrayList(list);

                javafx.application.Platform.runLater(() -> {
                    verificationTable.setItems(data);
                    verificationTable.refresh();
                    System.out.println("表格当前items数量: " + verificationTable.getItems().size());
                    System.out.println("核销记录加载完成");
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("加载核销记录失败: " + e.getMessage());
            }
        }
    }

    @FXML
    private void verifyPayment() {
        try {
            if (receipt == null) {
                showAlert(Alert.AlertType.ERROR, "错误", "未选择票据");
                return;
            }

            String amountText = verificationAmountField.getText();
            if (amountText == null || amountText.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "错误", "请输入核销金额");
                return;
            }

            BigDecimal verificationAmount = new BigDecimal(amountText);
            if (verificationAmount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert(Alert.AlertType.ERROR, "错误", "核销金额必须大于0");
                return;
            }

            BigDecimal remainingAmount = receipt.getRemainingAmount();
            if (verificationAmount.compareTo(remainingAmount) > 0) {
                showAlert(Alert.AlertType.ERROR, "错误", "核销金额不能超过剩余金额");
                return;
            }

            PaymentAccount paymentAccount = paymentAccountComboBox.getValue();
            if (paymentAccount == null) {
                showAlert(Alert.AlertType.ERROR, "错误", "请选择支付账户");
                return;
            }

            Integer verifiedBy = null;
            if (clientSession != null && clientSession.getCurrentUser() != null) {
                verifiedBy = clientSession.getCurrentUser().getUserId();
            }
            if (verifiedBy == null) {
                showAlert(Alert.AlertType.ERROR, "错误", "未获取到登录用户信息，请重新登录");
                return;
            }
            verifiedByField.setText(verifiedBy.toString());

            String remark = remarkTextArea.getText();

            Map<String, Object> body = new HashMap<>();
            body.put("paymentAccountId", paymentAccount.getId());
            body.put("amount", verificationAmount);
            body.put("verifiedBy", verifiedBy);
            body.put("remark", remark);

            Map<String, Object> result = apiClient.post("/api/receipts/" + receipt.getId() + "/verify",
                    body, Map.class);

            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                showAlert(Alert.AlertType.INFORMATION, "成功", "票据核销成功");
                receipt = apiClient.get("/api/receipts/" + receipt.getId(), Receipt.class);
                fillReceiptInfo();
                loadVerificationRecords();
                loadPaymentAccounts();
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "错误", "票据核销失败");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "请输入有效的数字");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "票据核销失败: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        Stage stage = (Stage) receiptIdField.getScene().getWindow();
        stage.close();
    }

    private void clearForm() {
        verificationAmountField.clear();
        if (!paymentAccountComboBox.getItems().isEmpty()) {
            paymentAccountComboBox.setValue(paymentAccountComboBox.getItems().get(0));
        }
        verifiedByField.clear();
        verificationTimePicker.setValue(java.time.LocalDate.now());
        remarkTextArea.clear();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}