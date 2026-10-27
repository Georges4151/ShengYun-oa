package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.PaymentAccount;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;

public class FXPaymentAddController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    private FXPaymentController paymentController;
    private PaymentAccount account;
    private boolean isEditMode = false;

    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private TextField balanceField;
    @FXML
    private TextArea descriptionArea;

    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    @FXML
    public void initialize() {
        // 初始化账户类型下拉框
        typeComboBox.getItems().addAll(
                "现金", "银行", "微信", "支付宝", "其他");
        typeComboBox.getSelectionModel().selectFirst();

        // 设置按钮事件
        saveButton.setOnAction(event -> handleSave());
        cancelButton.setOnAction(event -> handleCancel());
    }

    /**
     * 设置支付控制器
     * 
     * @param controller 支付控制器
     */
    public void setPaymentController(FXPaymentController controller) {
        this.paymentController = controller;
    }

    /**
     * 设置账户信息（编辑模式）
     * 
     * @param account 账户信息
     */
    public void setAccount(PaymentAccount account) {
        this.account = account;
        this.isEditMode = true;
        populateForm();
    }

    /**
     * 填充表单数据
     */
    private void populateForm() {
        if (account != null) {
            nameField.setText(account.getName());

            // 设置账户类型
            String typeText = account.getTypeText();
            int index = typeComboBox.getItems().indexOf(typeText);
            if (index >= 0) {
                typeComboBox.getSelectionModel().select(index);
            }

            balanceField.setText(account.getBalance().toString());
            descriptionArea.setText(account.getDescription());
        }
    }

    /**
     * 处理保存
     */
    private void handleSave() {
        if (!validateForm()) {
            return;
        }

        try {
            if (apiClient == null) {
                apiClient = RemoteApiClient.getInstance();
            }

            if (isEditMode) {
                // 更新账户
                updateAccount();
            } else {
                // 添加账户
                addAccount();
            }

            // 关闭窗口
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "保存账户失败: " + e.getMessage());
        }
    }

    /**
     * 验证表单
     * 
     * @return 是否验证通过
     */
    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入账户名称");
            return false;
        }

        if (balanceField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入初始余额");
            return false;
        }

        try {
            new BigDecimal(balanceField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入有效的初始余额");
            return false;
        }

        return true;
    }

    /**
     * 添加账户（通过HTTP API）
     */
    private void addAccount() throws IOException {
        PaymentAccount newAccount = new PaymentAccount();
        newAccount.setName(nameField.getText().trim());
        newAccount.setType(getTypeValue(typeComboBox.getValue()));
        newAccount.setBalance(new BigDecimal(balanceField.getText().trim()));
        newAccount.setDescription(descriptionArea.getText().trim());

        PaymentAccount result = apiClient.post("/api/payment/accounts", newAccount, PaymentAccount.class);
        if (result != null) {
            showAlert(Alert.AlertType.INFORMATION, "成功", "账户添加成功");
        } else {
            showAlert(Alert.AlertType.ERROR, "错误", "账户添加失败");
        }
    }

    /**
     * 更新账户（通过HTTP API）
     */
    private void updateAccount() throws IOException {
        account.setName(nameField.getText().trim());
        account.setType(getTypeValue(typeComboBox.getValue()));
        account.setBalance(new BigDecimal(balanceField.getText().trim()));
        account.setDescription(descriptionArea.getText().trim());

        PaymentAccount result = apiClient.put("/api/payment/accounts/" + account.getId(), account, PaymentAccount.class);
        if (result != null) {
            showAlert(Alert.AlertType.INFORMATION, "成功", "账户更新成功");
        } else {
            showAlert(Alert.AlertType.ERROR, "错误", "账户更新失败");
        }
    }

    /**
     * 根据显示文本获取类型值
     * 
     * @param typeText 类型显示文本
     * @return 类型值
     */
    private String getTypeValue(String typeText) {
        switch (typeText) {
            case "现金":
                return "cash";
            case "银行":
                return "bank";
            case "微信":
                return "wechat";
            case "支付宝":
                return "alipay";
            case "其他":
                return "other";
            default:
                return "other";
        }
    }

    /**
     * 处理取消
     */
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * 显示提示信息
     * 
     * @param type    提示类型
     * @param title   标题
     * @param content 内容
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}