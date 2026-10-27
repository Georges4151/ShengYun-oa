package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.PaymentAccount;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class FXPaymentController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TableView<PaymentAccount> accountTable;
    @FXML
    private TableColumn<PaymentAccount, Integer> idColumn;
    @FXML
    private TableColumn<PaymentAccount, String> nameColumn;
    @FXML
    private TableColumn<PaymentAccount, String> typeColumn;
    @FXML
    private TableColumn<PaymentAccount, Number> balanceColumn;
    @FXML
    private TableColumn<PaymentAccount, String> descriptionColumn;
    @FXML
    private TableColumn<PaymentAccount, String> createTimeColumn;

    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button transactionButton;
    @FXML
    private Button reconciliationButton;

    private ObservableList<PaymentAccount> accountList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化表格列
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTypeText()));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        createTimeColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                dateFormat.format(cellData.getValue().getCreateTime())));

        // 加载账户数据
        loadAccounts();

        // 设置按钮事件
        addButton.setOnAction(event -> handleAddAccount());
        editButton.setOnAction(event -> handleEditAccount());
        deleteButton.setOnAction(event -> handleDeleteAccount());
        transactionButton.setOnAction(event -> handleTransactionQuery());
        reconciliationButton.setOnAction(event -> handleBankReconciliation());
    }

    /**
     * 加载账户数据（通过HTTP API）
     */
    private void loadAccounts() {
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }

        try {
            List<PaymentAccount> accounts = apiClient.get(
                    "/api/payment/accounts",
                    List.class
            );

            if (accounts != null) {
                accountList = FXCollections.observableArrayList(accounts);
            } else {
                accountList = FXCollections.observableArrayList();
            }
            accountTable.setItems(accountList);
        } catch (IOException e) {
            System.err.println("加载支付账户数据失败: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "加载失败", "无法从服务器加载支付账户数据：" + e.getMessage());
        }
    }

    /**
     * 公共刷新账户数据方法，允许其他控制器调用
     */
    public void refreshAccounts() {
        loadAccounts();
    }

    /**
     * 处理添加账户
     */
    private void handleAddAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment/add.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();

            FXPaymentAddController controller = loader.getController();
            controller.setPaymentController(this);

            Stage stage = new Stage();
            stage.setTitle("添加账户");
            WindowUtils.setStageIcon(stage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(addButton.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // 重新加载数据
            loadAccounts();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "加载添加账户界面失败");
        }
    }

    /**
     * 处理编辑账户
     */
    private void handleEditAccount() {
        PaymentAccount selectedAccount = accountTable.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择要编辑的账户");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment/add.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();

            FXPaymentAddController controller = loader.getController();
            controller.setPaymentController(this);
            controller.setAccount(selectedAccount);

            Stage stage = new Stage();
            stage.setTitle("编辑账户");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(editButton.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // 重新加载数据
            loadAccounts();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "加载编辑账户界面失败");
        }
    }

    /**
     * 处理删除账户（通过HTTP DELETE API）
     */
    private void handleDeleteAccount() {
        PaymentAccount selectedAccount = accountTable.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择要删除的账户");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除账户");
        alert.setContentText("确定要删除账户 " + selectedAccount.getName() + " 吗？");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (apiClient == null) {
                        apiClient = RemoteApiClient.getInstance();
                    }
                    apiClient.delete("/api/payment/accounts/" + selectedAccount.getId(), Void.class);
                    showAlert(Alert.AlertType.INFORMATION, "成功", "账户删除成功");
                    loadAccounts();
                } catch (IOException e) {
                    System.err.println("删除支付账户失败: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "错误", "账户删除失败：" + e.getMessage());
                }
            }
        });
    }

    /**
     * 处理收支查询
     */
    private void handleTransactionQuery() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment/transaction.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("收支查询");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(transactionButton.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "加载收支查询界面失败");
        }
    }

    /**
     * 处理银行对账
     */
    private void handleBankReconciliation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment/bank_reconciliation.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("银行流水对账");
            WindowUtils.setStageIcon(stage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(reconciliationButton.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "加载银行对账界面失败");
        }
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

    /**
     * 创建控制器实例并注入依赖
     * 用于FXML loader的controllerFactory
     */
    private Object createController(Class<?> controllerClass) {
        try {
            Object controller = controllerClass.getDeclaredConstructor().newInstance();

            // 如果控制器有setApiClient方法，则注入RemoteApiClient
            try {
                java.lang.reflect.Method setApiClient = controllerClass.getMethod("setApiClient", RemoteApiClient.class);
                setApiClient.invoke(controller, apiClient != null ? apiClient : RemoteApiClient.getInstance());
            } catch (NoSuchMethodException e) {
                // 控制器不需要ApiClient，忽略
            }

            // 如果控制器有setClientSession方法，则注入ClientSession
            try {
                java.lang.reflect.Method setClientSession = controllerClass.getMethod("setClientSession", ClientSession.class);
                setClientSession.invoke(controller, clientSession != null ? clientSession : ClientSession.getInstance());
            } catch (NoSuchMethodException e) {
                // 控制器不需要ClientSession，忽略
            }

            return controller;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create controller: " + controllerClass.getName(), e);
        }
    }
}
