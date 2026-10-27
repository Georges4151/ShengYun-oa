package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.exception.BusinessException;
import com.ceams.model.entity.Account;
import com.ceams.model.entity.PaymentAccount;
import com.ceams.model.entity.PaymentTransaction;
import com.ceams.model.entity.Project;
import com.ceams.ui.shortcut.SaveControllerManager;
import com.ceams.ui.shortcut.Saveable;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FXAccountAddController implements Saveable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private ComboBox<ProjectOption> projectComboBox;

    @FXML
    private ComboBox<TypeOption> typeComboBox;

    @FXML
    private ComboBox<PaymentAccountOption> paymentAccountComboBox;

    @FXML
    private TextField categoryField;

    @FXML
    private TextField amountField;

    @FXML
    private DatePicker transactionDatePicker;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField receiptIdField;

    private Account editAccount;
    private boolean hasUnsavedChanges = false;

    /**
     * 类型选项类，用于显示中文但保存英文值
     */
    public static class TypeOption {
        private String displayName;
        private String value;

        public TypeOption(String displayName, String value) {
            this.displayName = displayName;
            this.value = value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * 项目选项类，用于显示项目名称但保存项目ID
     */
    public static class ProjectOption {
        private Integer projectId;
        private String projectName;

        public ProjectOption(Integer projectId, String projectName) {
            this.projectId = projectId;
            this.projectName = projectName;
        }

        public Integer getProjectId() {
            return projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        @Override
        public String toString() {
            return projectName;
        }
    }

    /**
     * 支付账户选项类，用于显示账户名称但保存账户ID
     */
    public static class PaymentAccountOption {
        private Integer accountId;
        private String accountName;
        private BigDecimal balance;

        public PaymentAccountOption(Integer accountId, String accountName, BigDecimal balance) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.balance = balance;
        }

        public Integer getAccountId() {
            return accountId;
        }

        public String getAccountName() {
            return accountName;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        @Override
        public String toString() {
            return accountName + " (余额: " + balance + ")";
        }
    }

    /**
     * 初始化方法，设置下拉框的选项
     */
    public void initialize() {
        typeComboBox.getItems().addAll(
                new TypeOption("收入", "income"));
        typeComboBox.getSelectionModel().selectFirst();

        loadProjects();

        loadPaymentAccounts();

        // 注册到保存控制器管理器
        SaveControllerManager.getInstance().registerController("account_add", this);
        SaveControllerManager.getInstance().setCurrentActiveModule("account_add");
    }

    @Override
    public void save() {
        saveAccount();
    }

    @Override
    public String getModuleName() {
        return "account_add";
    }

    @Override
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    public void markAsChanged() {
        this.hasUnsavedChanges = true;
    }

    /**
     * 设置要编辑的账目
     * 
     * @param account 要编辑的账目对象
     */
    public void setEditAccount(Account account) {
        this.editAccount = account;
        if (account != null) {
            // 填充表单数据
            fillFormWithAccountData(account);
        }
    }

    /**
     * 用账目数据填充表单
     * 
     * @param account 账目对象
     */
    private void fillFormWithAccountData(Account account) {
        // 设置项目选择
        for (ProjectOption option : projectComboBox.getItems()) {
            if (option.getProjectId().equals(account.getProjectId())) {
                projectComboBox.getSelectionModel().select(option);
                break;
            }
        }

        // 设置支付账户选择
        for (PaymentAccountOption option : paymentAccountComboBox.getItems()) {
            if (option.getAccountId().equals(account.getPaymentAccountId())) {
                paymentAccountComboBox.getSelectionModel().select(option);
                break;
            }
        }

        // 设置类型选择
        for (TypeOption option : typeComboBox.getItems()) {
            if (option.getValue().equals(account.getType())) {
                typeComboBox.getSelectionModel().select(option);
                break;
            }
        }

        // 设置金额
        if (account.getAmount() != null) {
            amountField.setText(account.getAmount().toString());
        }

        // 设置日期
        if (account.getTransactionDate() != null) {
            java.time.LocalDate localDate = account.getTransactionDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            transactionDatePicker.setValue(localDate);
        }

        // 设置分类ID
        if (account.getCategoryId() != null) {
            categoryField.setText(account.getCategoryId().toString());
        }

        // 设置描述
        if (account.getDescription() != null) {
            descriptionField.setText(account.getDescription());
        }

        // 设置票据ID
        if (account.getReceiptId() != null) {
            receiptIdField.setText(account.getReceiptId().toString());
        }
    }

    /**
     * 加载项目列表到下拉框（通过HTTP API）
     */
    private void loadProjects() {
        System.out.println("开始加载项目列表");

        if (apiClient == null) {
            System.out.println("apiClient尚未注入，使用默认单例");
            apiClient = RemoteApiClient.getInstance();
        }

        try {
            ProjectListResponse response = apiClient.get("/api/projects", ProjectListResponse.class);
            List<Project> projects = response != null ? response.getProjects() : new ArrayList<>();
            System.out.println("获取到的项目数量: " + projects.size());

            List<ProjectOption> projectOptions = new ArrayList<>();

            for (Project project : projects) {
                System.out.println("项目: ID=" + project.getId() + ", 名称=" + project.getProjectName());
                projectOptions.add(new ProjectOption(project.getId(), project.getProjectName()));
            }

            projectComboBox.getItems().setAll(projectOptions);
            // 默认选择第一个项目（如果有）
            if (!projectOptions.isEmpty()) {
                projectComboBox.getSelectionModel().selectFirst();
                System.out.println("默认选择项目: " + projectComboBox.getValue().getProjectName());
            }
            System.out.println("项目列表加载完成");
        } catch (IOException e) {
            System.err.println("加载项目列表失败: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("加载失败", "无法从服务器加载项目列表：" + e.getMessage());
        }
    }

    /**
     * 项目列表响应内部类
     */
    private static class ProjectListResponse {
        private List<Project> projects;

        public List<Project> getProjects() {
            return projects;
        }

        public void setProjects(List<Project> projects) {
            this.projects = projects;
        }
    }

    /**
     * 加载支付账户列表到下拉框（通过HTTP API）
     */
    private void loadPaymentAccounts() {
        System.out.println("开始加载支付账户列表");

        if (apiClient == null) {
            System.out.println("apiClient尚未注入，使用默认单例");
            apiClient = RemoteApiClient.getInstance();
        }

        try {
            PaymentAccountListResponse response = apiClient.get("/api/payment/accounts", PaymentAccountListResponse.class);
            List<PaymentAccount> paymentAccounts = response != null ? response.getAccounts() : new ArrayList<>();
            System.out.println("获取到的支付账户数量: " + paymentAccounts.size());

            List<PaymentAccountOption> paymentAccountOptions = new ArrayList<>();

            for (PaymentAccount account : paymentAccounts) {
                System.out.println(
                        "支付账户: ID=" + account.getId() + ", 名称=" + account.getName() + ", 余额=" + account.getBalance());
                paymentAccountOptions.add(new PaymentAccountOption(
                        account.getId(),
                        account.getName(),
                        account.getBalance()));
            }

            paymentAccountComboBox.getItems().setAll(paymentAccountOptions);
            // 默认选择第一个账户（如果有）
            if (!paymentAccountOptions.isEmpty()) {
                paymentAccountComboBox.getSelectionModel().selectFirst();
                System.out.println("默认选择支付账户: " + paymentAccountComboBox.getValue().getAccountName());
            }
            System.out.println("支付账户列表加载完成");
        } catch (IOException e) {
            System.err.println("加载支付账户列表失败: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("加载失败", "无法从服务器加载支付账户列表：" + e.getMessage());
        }
    }

    /**
     * 支付账户列表响应内部类
     */
    private static class PaymentAccountListResponse {
        private List<PaymentAccount> accounts;

        public List<PaymentAccount> getAccounts() {
            return accounts;
        }

        public void setAccounts(List<PaymentAccount> accounts) {
            this.accounts = accounts;
        }
    }

    @FXML
    private void saveAccount() {
        try {
            System.out.println("开始保存账目...");

            // 验证必填字段
            if (projectComboBox.getValue() == null) {
                System.out.println("项目不能为空");
                return;
            }

            if (paymentAccountComboBox.getValue() == null) {
                System.out.println("交易账户不能为空");
                return;
            }

            if (typeComboBox.getValue() == null) {
                System.out.println("类型不能为空");
                return;
            }

            if (amountField.getText() == null || amountField.getText().isEmpty()) {
                System.out.println("金额不能为空");
                return;
            }

            // 创建或获取账目对象
            Account account;
            boolean isEditMode = (editAccount != null);

            if (isEditMode) {
                // 编辑模式，使用现有对象
                account = editAccount;
                System.out.println("编辑模式，账目ID: " + account.getId());
            } else {
                // 添加模式，创建新对象
                account = new Account();
                System.out.println("添加模式，创建新账目");
            }

            // 获取项目ID
            ProjectOption selectedProject = projectComboBox.getValue();
            account.setProjectId(selectedProject.getProjectId());
            System.out.println("项目ID: " + selectedProject.getProjectId());

            // 获取支付账户ID
            PaymentAccountOption selectedPaymentAccount = paymentAccountComboBox.getValue();
            account.setPaymentAccountId(selectedPaymentAccount.getAccountId());
            System.out.println("支付账户ID: " + selectedPaymentAccount.getAccountId());

            // 获取日期值
            if (transactionDatePicker.getValue() != null) {
                Date transactionDate = Date
                        .from(transactionDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
                account.setTransactionDate(transactionDate);
                System.out.println("交易日期: " + transactionDate);
            }

            BigDecimal amount = new BigDecimal(amountField.getText());
            account.setAmount(amount);
            System.out.println("金额: " + amountField.getText());

            // 获取类型的实际值（英文）
            String typeValue = ((TypeOption) typeComboBox.getValue()).getValue();
            account.setType(typeValue);
            System.out.println("类型: " + typeValue);

            // 处理分类ID
            if (categoryField.getText() != null && !categoryField.getText().isEmpty()) {
                try {
                    account.setCategoryId(Integer.parseInt(categoryField.getText()));
                    System.out.println("分类ID: " + categoryField.getText());
                } catch (NumberFormatException e) {
                    System.out.println("分类ID必须是数字");
                }
            }

            account.setDescription(descriptionField.getText());
            System.out.println("描述: " + descriptionField.getText());

            // 处理票据ID
            if (receiptIdField.getText() != null && !receiptIdField.getText().isEmpty()) {
                try {
                    account.setReceiptId(Integer.parseInt(receiptIdField.getText()));
                    System.out.println("票据ID: " + receiptIdField.getText());
                } catch (NumberFormatException e) {
                    System.out.println("票据ID必须是数字");
                }
            }

            if (apiClient == null) {
                System.out.println("apiClient尚未注入，使用默认单例");
                apiClient = RemoteApiClient.getInstance();
            }

            if (isEditMode) {
                // 编辑模式，调用PUT API更新账目
                Account updatedAccount = apiClient.put("/api/accounts/" + account.getId(), account, Account.class);
                System.out.println("更新结果: " + (updatedAccount != null));
            } else {
                // 添加模式，调用POST API新增账目
                Account savedAccount = apiClient.post("/api/accounts", account, Account.class);
                System.out.println("保存结果: " + (savedAccount != null));

                if (savedAccount != null) {
                    // 保存支付交易记录
                    PaymentTransaction transaction = new PaymentTransaction();
                    transaction.setAccountId(selectedPaymentAccount.getAccountId());
                    transaction.setReceiptId(account.getReceiptId());
                    transaction.setAmount(amount);
                    transaction.setType(typeValue);
                    transaction.setTransactionDate(
                            account.getTransactionDate() != null ? account.getTransactionDate() : new Date());
                    transaction.setDescription(account.getDescription());

                    PaymentTransaction savedTransaction = apiClient.post("/api/payment/transactions", transaction, PaymentTransaction.class);
                    System.out.println("保存支付交易记录结果: " + (savedTransaction != null));
                }
            }

            Stage stage = (Stage) projectComboBox.getScene().getWindow();
            stage.close();
        } catch (BusinessException e) {
            System.out.println("业务异常: " + e.getMessage());
            showErrorAlert("操作失败", e.getMessage());
        } catch (IOException e) {
            System.out.println("保存账目失败: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("错误", "保存账目失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("保存账目失败: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("错误", "保存账目失败: " + e.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void cancelEdit() {
        // 关闭当前窗口
        Stage stage = (Stage) projectComboBox.getScene().getWindow();
        stage.close();
    }
}