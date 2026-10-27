package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.Pagination;
import com.ceams.model.entity.Account;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class FXAccountController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TextField projectIdField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TableView<Account> accountTable;

    @FXML
    private TableColumn<Account, Integer> idColumn;

    @FXML
    private TableColumn<Account, Integer> projectIdColumn;

    @FXML
    private TableColumn<Account, String> transactionDateColumn;

    @FXML
    private TableColumn<Account, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Account, String> typeColumn;

    @FXML
    private TableColumn<Account, String> descriptionColumn;

    @FXML
    private TableColumn<Account, String> statusColumn;

    @FXML
    private TableColumn<Account, String> paymentAccountNameColumn;

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
    @FXML
    private ComboBox<Integer> pageSizeComboBox;

    private Pagination pagination;
    private int currentPage = 1;
    private int totalRecords = 0;
    private int totalPages = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化表格列
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        projectIdColumn.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        transactionDateColumn.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        // 为类型列添加自定义单元格工厂，将英文类型转换为中文显示
        typeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Account, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 将英文类型转换为中文显示
                    if ("income".equals(item)) {
                        setText("收入");
                    } else if ("expense".equals(item)) {
                        setText("支出");
                    } else {
                        setText(item);
                    }
                }
            }
        });
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentAccountNameColumn.setCellValueFactory(new PropertyValueFactory<>("paymentAccountName"));

        // 初始化分页参数
        pagination = new Pagination(currentPage, 10);

        // 初始化分页大小下拉框
        ObservableList<Integer> pageSizes = FXCollections.observableArrayList(10, 20, 50, 100);
        pageSizeComboBox.setItems(pageSizes);
        pageSizeComboBox.setValue(10);

        // 设置分页按钮事件
        firstPageButton.setOnAction(event -> goToFirstPage());
        prevPageButton.setOnAction(event -> goToPrevPage());
        nextPageButton.setOnAction(event -> goToNextPage());
        lastPageButton.setOnAction(event -> goToLastPage());

        // 设置分页大小变化事件
        pageSizeComboBox.setOnAction(event -> {
            pagination.setPageSize(pageSizeComboBox.getValue());
            currentPage = 1;
            loadAccounts();
        });

        // 加载账目数据
        loadAccounts();
    }

    /**
     * 刷新账目数据（由外部调用，替代事件监听）
     */
    public void refreshData() {
        Platform.runLater(() -> {
            loadAccounts();
        });
    }

    /**
     * 加载所有账目数据（通过HTTP API）
     */
    private void loadAccounts() {
        // 检查UI组件是否已经被初始化
        if (accountTable == null) {
            System.out.println("accountTable尚未初始化，跳过账目列表加载");
            return;
        }

        // 检查apiClient是否已注入
        if (apiClient == null) {
            System.out.println("apiClient尚未注入，使用默认单例");
            apiClient = RemoteApiClient.getInstance();
        }

        // 更新分页参数
        pagination.setPageNum(currentPage);
        pagination.setPageSize(pageSizeComboBox.getValue());

        try {
            // 构建分页查询参数
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("page", String.valueOf(currentPage));
            queryParams.put("size", String.valueOf(pagination.getPageSize()));

            // 调用后端API获取账目列表
            AccountListResponse response = apiClient.get(
                "/api/accounts",
                queryParams,
                AccountListResponse.class
            );

            if (response != null && response.getContent() != null) {
                List<Account> accounts = response.getContent();
                totalRecords = response.getTotalElements();
                totalPages = response.getTotalPages();

                ObservableList<Account> data = FXCollections.observableArrayList(accounts);
                accountTable.setItems(data);
            } else {
                // 响应为空，清空表格
                accountTable.setItems(FXCollections.observableArrayList());
                totalRecords = 0;
                totalPages = 0;
            }

            // 更新分页信息
            currentPageLabel.setText(currentPage + " / " + totalPages);

            // 刷新分页按钮状态
            updatePaginationButtons();

        } catch (IOException e) {
            System.err.println("加载账目数据失败: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("加载失败", "无法从服务器加载账目数据：" + e.getMessage());
        }
    }

    /**
     * 账目列表响应内部类（用于解析分页API响应）
     */
    private static class AccountListResponse {
        private List<Account> content;
        private int totalPages;
        private int totalElements;
        private int number;
        private int size;

        public List<Account> getContent() {
            return content;
        }

        public void setContent(List<Account> content) {
            this.content = content;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(int totalElements) {
            this.totalElements = totalElements;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }

    /**
     * 刷新分页按钮状态
     */
    private void updatePaginationButtons() {
        firstPageButton.setDisable(currentPage == 1);
        prevPageButton.setDisable(currentPage == 1);
        nextPageButton.setDisable(currentPage >= totalPages);
        lastPageButton.setDisable(currentPage >= totalPages);
    }

    /**
     * 跳转到第一页
     */
    private void goToFirstPage() {
        currentPage = 1;
        loadAccounts();
    }

    /**
     * 跳转到上一页
     */
    private void goToPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadAccounts();
        }
    }

    /**
     * 跳转到下一页
     */
    private void goToNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadAccounts();
        }
    }

    /**
     * 跳转到最后一页
     */
    private void goToLastPage() {
        currentPage = totalPages;
        loadAccounts();
    }

    /**
     * 刷新账目列表
     */
    @FXML
    private void refresh() {
        loadAccounts();
    }

    /**
     * 查询账目
     */
    @FXML
    private void search() {
        // 这里可以实现查询逻辑
        System.out.println("查询账目");
        loadAccounts();
    }

    /**
     * 添加账目
     */
    @FXML
    private void addAccount() {
        System.out.println("添加账目");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/account/add.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("添加账目");
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            // 刷新账目列表
            loadAccounts();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("打开窗口失败", "无法打开添加账目窗口：" + e.getMessage());
        }
    }

    /**
     * 编辑账目
     */
    @FXML
    private void editAccount() {
        Account selectedAccount = accountTable.getSelectionModel().getSelectedItem();
        if (selectedAccount != null) {
            System.out.println("编辑账目: " + selectedAccount.getId());
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/account/edit.fxml"));
                loader.setControllerFactory(this::createController);
                Parent root = loader.load();

                // 获取控制器并设置要编辑的账目
                FXAccountAddController controller = loader.getController();
                controller.setEditAccount(selectedAccount);

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("编辑账目");
                WindowUtils.setStageIcon(stage);
                stage.setScene(new Scene(root));
                stage.showAndWait();
                // 刷新账目列表
                loadAccounts();
            } catch (IOException e) {
                e.printStackTrace();
                showErrorAlert("打开窗口失败", "无法打开编辑账目窗口：" + e.getMessage());
            }
        } else {
            System.out.println("请选择要编辑的账目");
            showErrorAlert("提示", "请先选择要编辑的账目");
        }
    }

    /**
     * 删除账目（通过HTTP DELETE API）
     */
    @FXML
    private void deleteAccount() {
        Account selectedAccount = accountTable.getSelectionModel().getSelectedItem();
        if (selectedAccount != null) {
            // 弹出确认对话框
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText("删除账目");
            alert.setContentText("确定要删除ID为" + selectedAccount.getId() + "的账目吗？此操作不可恢复。");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.out.println("删除账目: " + selectedAccount.getId());
                try {
                    if (apiClient == null) {
                        apiClient = RemoteApiClient.getInstance();
                    }
                    apiClient.delete("/api/accounts/" + selectedAccount.getId(), Void.class);
                    System.out.println("账目删除成功");
                    // 刷新账目列表
                    loadAccounts();
                } catch (IOException e) {
                    System.err.println("删除账目失败: " + e.getMessage());
                    e.printStackTrace();
                    showErrorAlert("删除失败", "无法删除账目：" + e.getMessage());
                }
            }
        } else {
            System.out.println("请选择要删除的账目");
            showErrorAlert("提示", "请先选择要删除的账目");
        }
    }

    /**
     * 显示错误提示对话框
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
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

    /**
     * 财务报表
     */
    @FXML
    private void financialReport() {
        // 这里可以实现财务报表逻辑
        System.out.println("生成财务报表");
    }

    /**
     * 生成报表
     */
    @FXML
    private void generateReport() {
        // 这里可以实现生成报表逻辑
        System.out.println("生成财务报表");
    }

    /**
     * 查看月度统计
     */
    @FXML
    private void viewMonthlyStats() {
        // 这里可以实现查看月度统计逻辑
        System.out.println("查看月度统计");
    }

    /**
     * 导出PDF
     */
    @FXML
    private void exportPDF() {
        // 这里可以实现导出PDF逻辑
        System.out.println("导出PDF");
    }

    /**
     * 导出Excel
     */
    @FXML
    private void exportExcel() {
        // 这里可以实现导出Excel逻辑
        System.out.println("导出Excel");
    }

    /**
     * 返回
     */
    @FXML
    private void back() {
        // 关闭当前窗口
        Stage stage = (Stage) accountTable.getScene().getWindow();
        stage.close();
    }
}