package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Receipt;
import com.ceams.model.entity.ApprovalRecord;
import com.ceams.model.entity.User;
import com.ceams.ui.controller.FXVerifyPaymentController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.control.Separator;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;
import javafx.util.Callback;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Optional;
import javafx.embed.swing.SwingFXUtils;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FXReceiptController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TextField reporterIdField;

    @FXML
    private TextField projectIdField;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> statusComboBox;

    // 票据验证所需字段
    @FXML
    private TextArea ocrResultTextArea;

    @FXML
    private TextField verifierIdField;

    @FXML
    private TextField searchField;

    @FXML
    private TextField reporterNameField;

    @FXML
    private TextField projectNameField;

    @FXML
    private TextField amountField;

    @FXML
    private TextField descriptionField;

    @FXML
    private ComboBox<String> verifyStatusComboBox;

    @FXML
    private TextField verifyRemarkField;

    @FXML
    private Label verificationStatusLabel;

    @FXML
    private Label checkAmountResult;

    @FXML
    private Label checkDateResult;

    @FXML
    private Label checkTypeResult;

    @FXML
    private Label checkProjectResult;

    @FXML
    private Label checkDuplicateResult;

    @FXML
    private TableView<?> verificationHistoryTable;

    // 三级审核所需字段
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
    private Label currentApprovalLevelLabel;

    @FXML
    private ComboBox<String> approvalActionComboBox;

    @FXML
    private TextArea approvalCommentTextArea;

    @FXML
    private TableView<ApprovalRecord> approvalHistoryTable;

    // 票据详情所需字段
    @FXML
    private TextField receiptIdField;

    @FXML
    private TextField receiptUserIdField;

    @FXML
    private TextField receiptReporterNameField;

    @FXML
    private TextField receiptProjectNameField;

    @FXML
    private TextField receiptDateField;

    @FXML
    private TextField receiptAmountField;

    @FXML
    private TextField receiptTypeField;

    @FXML
    private TextField receiptDescriptionField;

    @FXML
    private TextField receiptFilePathField;

    @FXML
    private TextField receiptVerificationStatusField;

    @FXML
    private ImageView receiptImageView;

    @FXML
    private TableView<Receipt> receiptTable;

    @FXML
    private TableColumn<Receipt, Integer> idColumn;

    @FXML
    private TableColumn<Receipt, Date> receiptDateColumn;

    @FXML
    private TableColumn<Receipt, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Receipt, String> typeColumn;

    @FXML
    private TableColumn<Receipt, Integer> reporterIdColumn;

    @FXML
    private TableColumn<Receipt, Integer> projectIdColumn;

    @FXML
    private TableColumn<Receipt, String> filePathColumn;

    @FXML
    private TableColumn<Receipt, String> verificationStatusColumn;

    @FXML
    private TableColumn<Receipt, String> paymentStatusColumn;

    @FXML
    private TableColumn<Receipt, String> paymentMethodColumn;

    @FXML
    private TableColumn<Receipt, BigDecimal> paidAmountColumn;

    @FXML
    private TableColumn<Receipt, BigDecimal> remainingAmountColumn;

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

    private int currentPage = 1;
    private int totalRecords = 0;
    private int totalPages = 0;
    private int pageSize = 10;

    // 存储的筛选条件（用于保持筛选状态）
    private String savedReporterName;
    private String savedProjectName;
    private java.sql.Date savedStartDate;
    private java.sql.Date savedEndDate;
    private String savedStatus;
    private boolean hasFilterCriteria = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== initialize 被调用 ===");
        System.out.println("idColumn = " + idColumn);
        System.out.println("receiptTable = " + receiptTable);
        // 检查idColumn是否存在，避免在详情页面或验证页面加载失败
        if (idColumn != null && receiptTable != null) {
            // 初始化表格列
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

            if (receiptDateColumn != null) {
                receiptDateColumn.setCellValueFactory(new PropertyValueFactory<>("receiptDate"));
                // 设置日期列的单元格工厂，格式化显示日期
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Callback<TableColumn<Receipt, Date>, TableCell<Receipt, Date>> dateCellFactory = column -> {
                    return new TableCell<Receipt, Date>() {
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
                };
                receiptDateColumn.setCellFactory(dateCellFactory);
            }

            if (amountColumn != null) {
                amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
                // 设置金额列的单元格工厂，格式化显示金额
                Callback<TableColumn<Receipt, BigDecimal>, TableCell<Receipt, BigDecimal>> amountCellFactory = column -> {
                    return new TableCell<Receipt, BigDecimal>() {

                        @Override
                        protected void updateItem(BigDecimal item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(item.toString());
                            }
                        }
                    };
                };
                amountColumn.setCellFactory(amountCellFactory);
            }

            if (typeColumn != null) {
                typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
            }

            if (reporterIdColumn != null) {
                reporterIdColumn.setCellValueFactory(new PropertyValueFactory<>("reporterName"));
            }

            if (projectIdColumn != null) {
                projectIdColumn.setCellValueFactory(new PropertyValueFactory<>("projectName"));
            }

            if (verificationStatusColumn != null) {
                verificationStatusColumn.setCellValueFactory(new PropertyValueFactory<>("approvalProgressText"));

                javafx.scene.paint.Paint approvedColor = javafx.scene.paint.Paint.valueOf("#27ae60");
                javafx.scene.paint.Paint rejectedColor = javafx.scene.paint.Paint.valueOf("#95a5a6");
                javafx.scene.paint.Paint pendingColor = javafx.scene.paint.Paint.valueOf("#3498db");

                verificationStatusColumn.setCellFactory(col -> {
                    return new TableCell<Receipt, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setTextFill(null);
                            } else {
                                setText(item);

                                if ("审核完成".equals(item) || item.contains("通过")) {
                                    setTextFill(approvedColor);
                                    setFont(javafx.scene.text.Font.font(null, javafx.scene.text.FontWeight.BOLD, 12));
                                } else if (item.contains("拒绝") || item.contains("未通过")) {
                                    setTextFill(rejectedColor);
                                    setFont(javafx.scene.text.Font.font(null, javafx.scene.text.FontWeight.NORMAL, 12));
                                } else {
                                    setTextFill(pendingColor);
                                    setFont(javafx.scene.text.Font.font(null, javafx.scene.text.FontWeight.NORMAL, 12));
                                }
                            }
                        }
                    };
                });
            }

            // 初始化支付状态列
            if (paymentStatusColumn != null) {
                paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatusText"));
            }

            // 初始化支付方式列
            if (paymentMethodColumn != null) {
                paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethodText"));
            }

            // 初始化已支付金额列
            if (paidAmountColumn != null) {
                paidAmountColumn.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));

                // 设置金额列的单元格工厂，格式化显示金额
                Callback<TableColumn<Receipt, BigDecimal>, TableCell<Receipt, BigDecimal>> paidAmountCellFactory = column -> {
                    return new TableCell<Receipt, BigDecimal>() {
                        @Override
                        protected void updateItem(BigDecimal item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText("0.00");
                            } else {
                                setText(item.toString());
                            }
                        }
                    };
                };
                paidAmountColumn.setCellFactory(paidAmountCellFactory);
            }

            // 初始化剩余金额列
            if (remainingAmountColumn != null) {
                remainingAmountColumn.setCellValueFactory(new PropertyValueFactory<>("remainingAmount"));
                // 设置金额列的单元格工厂，格式化显示金额
                Callback<TableColumn<Receipt, BigDecimal>, TableCell<Receipt, BigDecimal>> remainingAmountCellFactory = column -> {
                    return new TableCell<Receipt, BigDecimal>() {
                        @Override
                        protected void updateItem(BigDecimal item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText("0.00");
                            } else {
                                setText(item.toString());
                            }
                        }
                    };
                };
                remainingAmountColumn.setCellFactory(remainingAmountCellFactory);
            }

            // 检查filePathColumn是否存在，避免在票据列表页面加载失败
            if (filePathColumn != null) {
                filePathColumn.setCellValueFactory(new PropertyValueFactory<>("filePath"));
            }

            // 检查分页相关组件是否存在，只在存在时初始化
            if (pageSizeComboBox != null) {
                // 初始化分页大小下拉框
                ObservableList<Integer> pageSizes = FXCollections.observableArrayList(10, 20, 50,
                        100);
                pageSizeComboBox.setItems(pageSizes);
                pageSizeComboBox.setValue(10);

                // 设置分页大小变化事件
                pageSizeComboBox.setOnAction(event -> {
                    pageSize = pageSizeComboBox.getValue();
                    currentPage = 1;

                    loadReceipts();
                });
            }

            // 检查分页按钮是否存在，只在存在时设置事件
            if (firstPageButton != null && prevPageButton != null && nextPageButton != null && lastPageButton != null) {
                // 设置分页按钮事件
                firstPageButton.setOnAction(event -> goToFirstPage());
                prevPageButton.setOnAction(event -> goToPrevPage());
                nextPageButton.setOnAction(event -> goToNextPage());
                lastPageButton.setOnAction(event -> goToLastPage());
            }

            // 加载票据数据
            loadReceipts();

            // 添加鼠标点击事件处理
            receiptTable.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    // 双击事件，调用查看详情方法
                    viewDetail();
                } else if (event.getClickCount() == 1) {
                    // 单击事件，调用查看票据方法，显示发票原件
                    viewReceipt();
                }
            });
        }

        // 初始化状态下拉框
        if (statusComboBox != null) {

            ObservableList<String> statusOptions = FXCollections.observableArrayList(
                    "待财务审核", "待项目经理审核", "待经理审核",
                    "审核完成");
            statusComboBox.setItems(statusOptions);
            statusComboBox.setValue("待财务审核"); // 默认值
        }

        // 初始化三级审核相关组件
        if (approvalActionComboBox != null) {
            ObservableList<String> approvalActions = FXCollections.observableArrayList(
                    "通过", "拒绝");
            approvalActionComboBox.setItems(approvalActions);
            approvalActionComboBox.setValue("通过");
        }

        if (approvalHistoryTable != null) {
            approvalHistoryTable.setItems(FXCollections.observableArrayList());
        }
    }

    /**
     * 加载所有票据数据（通过HTTP API）
     */
    public void loadReceipts() {
        System.out.println("=== loadReceipts 被调用 ===");

        // 如果有筛选条件，使用筛选条件查询
        if (hasFilterCriteria) {
            System.out.println("使用保存的筛选条件重新查询");
            currentPage = 1; // 筛选查询时重置到第一页
            executeQueryWithCriteria();
            return;
        }

        // 没有筛选条件时，加载全部数据（原有逻辑）
        if (receiptTable == null) {
            System.out.println("receiptTable为null，无法加载数据");
            return;
        }

        // 确保apiClient已初始化
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }

        try {
            // 构建分页查询参数
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("page", String.valueOf(currentPage));
            if (pageSizeComboBox != null) {
                pageSize = pageSizeComboBox.getValue();
            }
            queryParams.put("size", String.valueOf(pageSize));

            // 调用后端API获取票据列表
            ReceiptListResponse response = apiClient.get(
                "/api/receipts",
                queryParams,
                ReceiptListResponse.class
            );

            if (response != null && response.getContent() != null) {
                List<Receipt> receipts = response.getContent();
                totalPages = response.getTotalPages();
                totalRecords = response.getTotalElements();

                ObservableList<Receipt> data = FXCollections.observableArrayList(receipts);
                receiptTable.setItems(data);
            } else {
                receiptTable.setItems(FXCollections.observableArrayList());
                totalPages = 0;
                totalRecords = 0;
            }

            System.out.println("查询到 " + totalRecords + " 条记录");

        } catch (IOException e) {
            System.err.println("加载票据数据失败: " + e.getMessage());
            e.printStackTrace();
            showError("加载失败", "无法从服务器加载票据数据：" + e.getMessage());
        }

        // 检查分页信息标签是否存在
        if (currentPageLabel != null) {
            // 更新分页信息
            currentPageLabel.setText(currentPage + " / " + totalPages);
        }

        // 检查分页按钮是否存在
        if (firstPageButton != null && prevPageButton != null && nextPageButton != null && lastPageButton != null) {
            // 刷新分页按钮状态
            updatePaginationButtons();
        }
    }

    /**
     * 刷新分页按钮状态
     */
    private void updatePaginationButtons() {
        if (firstPageButton != null && prevPageButton != null && nextPageButton != null && lastPageButton != null) {
            firstPageButton.setDisable(currentPage == 1);
            prevPageButton.setDisable(currentPage == 1);
            nextPageButton.setDisable(currentPage >= totalPages);
            lastPageButton.setDisable(currentPage >= totalPages);
        }
    }

    /**
     * 跳转到第一页
     */
    private void goToFirstPage() {
        currentPage = 1;
        loadReceipts();
    }

    /**
     * 跳转到上一页
     */
    private void goToPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadReceipts();
        }
    }

    /**
     * 跳转到下一页
     */
    private void goToNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadReceipts();
        }
    }

    /**
     * 跳转到最后一页
     */
    private void goToLastPage() {
        currentPage = totalPages;
        loadReceipts();
    }

    /**
     * 刷新票据列表
     */
    @FXML
    private void refresh() {
        loadReceipts();
    }

    /**
     * 查询票据
     */
    @FXML
    private void search() {
        // 检查receiptTable是否存在
        if (receiptTable == null) {
            System.out.println("receiptTable为null，无法执行查询");
            return;
        }

        // 获取报票人姓名
        savedReporterName = (reporterIdField != null && !reporterIdField.getText().isEmpty())
                ? reporterIdField.getText().trim()
                : null;

        // 获取归属项目名称
        savedProjectName = (projectIdField != null && !projectIdField.getText().isEmpty())
                ? projectIdField.getText().trim()
                : null;

        // 获取开始日期
        savedStartDate = (startDatePicker != null && startDatePicker.getValue() != null)
                ? java.sql.Date.valueOf(startDatePicker.getValue())
                : null;

        // 获取结束日期
        savedEndDate = (endDatePicker != null && endDatePicker.getValue() != null)
                ? java.sql.Date.valueOf(endDatePicker.getValue())
                : null;

        // 获取状态选择值
        savedStatus = (statusComboBox != null && statusComboBox.getValue() != null
                && !statusComboBox.getValue().isEmpty())
                        ? statusComboBox.getValue()
                        : null;

        // 标记有筛选条件
        hasFilterCriteria = true;

        // 执行查询
        executeQueryWithCriteria();

        System.out.println("查询完成，筛选条件已保存");
    }

    /**
     * 使用筛选条件执行查询（通过HTTP API）
     */
    private void executeQueryWithCriteria() {
        // 确保apiClient已初始化
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }

        try {
            // 构建查询参数
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("page", String.valueOf(currentPage));
            queryParams.put("size", String.valueOf(pageSize));

            if (savedReporterName != null) {
                queryParams.put("reporterName", savedReporterName);
            }
            if (savedProjectName != null) {
                queryParams.put("projectName", savedProjectName);
            }
            if (savedStartDate != null) {
                queryParams.put("startDate", savedStartDate.toString());
            }
            if (savedEndDate != null) {
                queryParams.put("endDate", savedEndDate.toString());
            }
            if (savedStatus != null) {
                queryParams.put("status", savedStatus);
            }

            // 调用后端API查询票据
            ReceiptListResponse response = apiClient.get(
                "/api/receipts",
                queryParams,
                ReceiptListResponse.class
            );

            if (response != null && response.getContent() != null) {
                List<Receipt> filteredReceipts = response.getContent();
                ObservableList<Receipt> data = FXCollections.observableArrayList(filteredReceipts);
                receiptTable.setItems(data);
                System.out.println("筛选查询完成，共找到" + filteredReceipts.size() + "条记录");
            } else {
                receiptTable.setItems(FXCollections.observableArrayList());
                System.out.println("筛选查询完成，未找到记录");
            }

        } catch (IOException e) {
            System.err.println("筛选查询失败: " + e.getMessage());
            e.printStackTrace();
            showError("查询失败", "无法从服务器查询票据数据：" + e.getMessage());
        }
    }

    /**
     * 重置查询条件
     */
    @FXML
    private void reset() {
        // 清空 UI 控件
        if (reporterIdField != null) {
            reporterIdField.clear();
        }
        if (projectIdField != null) {
            projectIdField.clear();
        }
        if (startDatePicker != null) {
            startDatePicker.setValue(null);
        }
        if (endDatePicker != null) {
            endDatePicker.setValue(null);
        }
        if (statusComboBox != null) {
            statusComboBox.setValue(null);
        }

        // 清空存储的筛选条件
        savedReporterName = null;
        savedProjectName = null;
        savedStartDate = null;
        savedEndDate = null;
        savedStatus = null;
        hasFilterCriteria = false;

        // 重新加载所有票据数据
        loadReceipts();
    }

    /**
     * 上传票据
     */
    @FXML
    private void uploadReceipt() {
        // 打开上传票据窗口
        System.out.println("打开上传票据窗口");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/receipt/upload.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("上传票据");
            stage.setScene(new Scene(root));
            stage.setWidth(600);
            stage.setHeight(500);

            stage.showAndWait();

            loadReceipts();
        } catch (IOException e) {
            e.printStackTrace();
            showError("错误", "无法打开上传票据窗口: " + e.getMessage());
        }
    }

    @FXML
    private void createVerification() {
        System.out.println("新建验证");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/receipt/verify.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("票据验证");
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.setWidth(900);
            stage.setHeight(650);

            stage.showAndWait();

            loadReceipts();
        } catch (IOException e) {
            e.printStackTrace();
            showError("错误", "无法打开票据验证窗口: " + e.getMessage());
        }
    }

    /**
     * 验证票据
     */
    @FXML
    private void verifyReceipt() {
        // 打开验证票据窗口
        System.out.println("打开验证票据窗口");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/receipt/verify.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("验证票据");
            stage.setScene(new Scene(root));
            stage.setWidth(900);
            stage.setHeight(650);
            stage.showAndWait();
            // 刷新票据列表
            loadReceipts();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("打开验证票据窗口失败: " + e.getMessage());
        }
    }

    /**
     * 查看详情
     */
    @FXML
    private void viewDetail() {
        System.out.println("查看详情按钮被点击");
        // 这里可以实现查看详情逻辑
        Receipt selectedReceipt = receiptTable.getSelectionModel().getSelectedItem();
        if (selectedReceipt != null) {
            System.out.println("查看票据详情: " + selectedReceipt.getId());
            // 打开票据详情窗口
            try {
                System.out.println("开始加载详情窗口 FXML");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/receipt/detail.fxml"));
                loader.setControllerFactory(this::createController);
                System.out.println("加载FXML文件");
                Parent root = loader.load();
                System.out.println("创建舞台");
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("票据详情");
                stage.setScene(new Scene(root));
                stage.setWidth(700);
                stage.setHeight(600);

                // 填充详情数据
                System.out.println("获取控制器实例");
                FXReceiptController controller = loader.getController();
                System.out.println("设置票据详情数据");
                controller.setReceiptDetails(selectedReceipt);

                System.out.println("显示详情窗口");
                stage.showAndWait();
                System.out.println("详情窗口关闭");
            } catch (Exception e) {
                System.out.println("打开票据详情窗口失败，详细错误信息:");
                e.printStackTrace();
                System.out.println("异常类型: " + e.getClass().getName());
                System.out.println("异常消息: " + e.getMessage());
                System.out.println("异常堆栈:");
                e.printStackTrace(System.out);
            }
        } else {
            System.out.println("请选择要查看详情的票据");
        }
    }

    /**
     * 设置票据详情数据
     */
    public void setReceiptDetails(Receipt receipt) {
        if (receipt == null) {
            System.out.println("票据数据为null，无法设置详情");
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // 为详情页面字段设置值，添加空值检查
        if (receiptIdField != null) {
            receiptIdField.setText(String.valueOf(receipt.getId()));
        }
        if (receiptUserIdField != null) {
            receiptUserIdField.setText(String.valueOf(receipt.getUserId()));
        }
        if (receiptReporterNameField != null) {
            receiptReporterNameField.setText(receipt.getReporterName());
        }
        if (receiptProjectNameField != null) {
            receiptProjectNameField.setText(receipt.getProjectName());
        }
        if (receiptDateField != null && receipt.getReceiptDate() != null) {
            receiptDateField.setText(dateFormat.format(receipt.getReceiptDate()));
        }
        if (receiptAmountField != null && receipt.getAmount() != null) {
            receiptAmountField.setText(receipt.getAmount().toString());
        }
        if (receiptTypeField != null) {
            receiptTypeField.setText(receipt.getType());
        }
        if (receiptDescriptionField != null) {
            receiptDescriptionField.setText(receipt.getDescription() != null ? receipt.getDescription() : "");
        }
        if (receiptFilePathField != null) {
            receiptFilePathField.setText(receipt.getFilePath());
        }
        if (receiptVerificationStatusField != null) {
            receiptVerificationStatusField.setText(receipt.getVerificationStatusText());
        }

        // 显示发票原件
        try {
            if (receiptImageView != null && receipt.getFilePath() != null) {
                File imageFile = new File(receipt.getFilePath());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    receiptImageView.setImage(image);
                } else {
                    System.out.println("图片文件不存在: " + receipt.getFilePath());
                }
            }
        } catch (Exception e) {
            System.out.println("显示图片失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 关闭详情窗口
     */
    @FXML
    private void closeDetail() {
        // 关闭当前窗口
        Stage stage = (Stage) receiptIdField.getScene().getWindow();
        stage.close();
    }

    /**
     * 显示错误对话框
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示信息对话框
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 保存画布状态到历史记录
     */
    private void saveToHistory(Canvas canvas) {
        // 保存当前画布状态
        WritableImage currentState = canvas.snapshot(null, null);

        // 如果当前索引不在历史记录末尾，删除索引之后的所有记录
        if (historyIndex < history.size() - 1) {
            history = new ArrayList<>(history.subList(0, historyIndex + 1));
        }

        // 将新状态添加到历史记录
        history.add(currentState);

        // 如果历史记录超过最大限制，删除最旧的记录
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        } else {
            historyIndex++;
        }

        System.out.println("保存历史记录，当前索引: " + historyIndex + ", 历史记录大小: " + history.size());
    }

    /**
     * 从历史记录恢复画布状态
     */
    private void restoreFromHistory(Canvas canvas) {
        if (historyIndex >= 0 && historyIndex < history.size()) {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            // 清除当前画布
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            // 绘制历史状态
            gc.drawImage(history.get(historyIndex), 0, 0);
            hasDrawn = true;
        }
    }

    // 涂鸦相关的成员变量
    private boolean isDrawing = false;
    private double lastX = 0.0;
    private double lastY = 0.0;
    private boolean hasDrawn = false;

    // 工具类型相关的成员变量
    private final int BRUSH = 0;
    private final int CURSOR = 1;
    private final int ERASER = 2;
    private int currentTool = CURSOR;

    // 撤回/重做相关的成员变量
    private List<WritableImage> history = new ArrayList<>();
    private int historyIndex = -1;
    private final int MAX_HISTORY = 5;

    // 图片平移相关的成员变量
    private boolean isPanning = false;
    private double lastPanX = 0;
    private double lastPanY = 0;

    /**
     * 显示大图
     */
    @FXML
    public void showLargeImage() {
        // 显示发票大图
        System.out.println("showLargeImage方法被调用");
        try {
            System.out.println("receiptImageView: " + receiptImageView);
            // 获取当前ImageView中显示的图片
            Image currentImage = receiptImageView.getImage();
            System.out.println("currentImage: " + currentImage);
            if (currentImage != null) {
                System.out.println("图片已加载，开始显示大图");
                // 初始化涂鸦和工具状态
                isDrawing = false;
                lastX = 0;
                lastY = 0;
                hasDrawn = false;
                currentTool = CURSOR;
                isPanning = false;
                lastPanX = 0;
                lastPanY = 0;

                // 创建一个新的Stage用于显示大图
                Stage largeImageStage = new Stage();
                largeImageStage.initModality(Modality.APPLICATION_MODAL);
                largeImageStage.setTitle("发票大图");

                // 创建一个新的ImageView，设置更大的尺寸
                ImageView largeImageView = new ImageView(currentImage);
                largeImageView.setFitWidth(800);
                largeImageView.setFitHeight(600);
                largeImageView.setPreserveRatio(true);
                largeImageView.setStyle("-fx-border-color: #cccccc; -fx-padding: 10px;");

                // 创建Canvas用于涂鸦
                Canvas canvas = new Canvas(800, 600);
                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.setLineWidth(2.0);
                gc.setStroke(Color.RED);
                gc.setFill(Color.TRANSPARENT);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                // 创建一个Pane，将ImageView和Canvas叠加在一起
                StackPane imagePane = new StackPane();
                imagePane.getChildren().addAll(largeImageView, canvas);
                imagePane.setStyle("-fx-background-color: #f0f0f0;");

                // 添加鼠标滚轮缩放功能
                imagePane.setOnScroll(event -> {
                    // 检查是否按住了Ctrl键
                    if (event.isControlDown()) {
                        event.consume();

                        // 获取当前缩放比例
                        double currentScale = largeImageView.getScaleX();

                        // 计算新的缩放比例
                        double deltaY = event.getDeltaY();
                        double newScale;
                        if (deltaY > 0) {
                            // 放大
                            newScale = currentScale * 1.1;
                        } else {
                            // 缩小
                            newScale = currentScale / 1.1;
                        }

                        // 设置缩放比例范围
                        newScale = Math.max(0.1, Math.min(newScale, 5.0));

                        // 应用新的缩放比例
                        largeImageView.setScaleX(newScale);
                        largeImageView.setScaleY(newScale);
                        canvas.setScaleX(newScale);
                        canvas.setScaleY(newScale);

                        System.out.println("缩放比例: " + newScale);
                    }
                });

                // 鼠标按下事件 - 根据当前工具执行不同操作
                canvas.setOnMousePressed(event -> {
                    if (currentTool == BRUSH || currentTool == ERASER) {
                        isDrawing = true;
                        lastX = event.getX();
                        lastY = event.getY();

                        if (currentTool == ERASER) {
                            // 橡皮擦模式：准备清除画布区域
                            gc.setLineWidth(10.0);
                        } else {
                            // 画笔模式：正常绘制，红色线条，线宽2.0
                            gc.setStroke(Color.RED);
                            gc.setLineWidth(2.0);
                        }
                    } else if (currentTool == CURSOR) {
                        // 光标模式：开始平移
                        isPanning = true;
                        lastPanX = event.getSceneX();
                        lastPanY = event.getSceneY();
                    }
                });

                // 鼠标拖动事件 - 根据当前工具执行不同操作
                canvas.setOnMouseDragged(event -> {
                    if (currentTool == BRUSH || currentTool == ERASER) {
                        if (isDrawing) {
                            double currentX = event.getX();
                            double currentY = event.getY();

                            if (currentTool == ERASER) {
                                // 橡皮擦模式：清除画布上的区域，只影响画笔痕迹
                                double lineWidth = gc.getLineWidth();
                                gc.clearRect(currentX - lineWidth / 2, currentY - lineWidth / 2, lineWidth, lineWidth);
                            } else {
                                // 画笔模式：正常绘制线条
                                gc.beginPath();
                                gc.moveTo(lastX, lastY);
                                gc.lineTo(currentX, currentY);
                                gc.stroke();
                            }

                            lastX = currentX;
                            lastY = currentY;
                            hasDrawn = true;
                            // 保存当前画布状态到历史记录
                            saveToHistory(canvas);
                        }
                    } else if (currentTool == CURSOR) {
                        // 光标模式：平移图片
                        if (isPanning) {
                            double deltaX = event.getSceneX() - lastPanX;
                            double deltaY = event.getSceneY() - lastPanY;

                            largeImageView.setTranslateX(largeImageView.getTranslateX() + deltaX);
                            largeImageView.setTranslateY(largeImageView.getTranslateY() + deltaY);
                            canvas.setTranslateX(canvas.getTranslateX() + deltaX);
                            canvas.setTranslateY(canvas.getTranslateY() + deltaY);

                            lastPanX = event.getSceneX();
                            lastPanY = event.getSceneY();
                        }
                    }
                });

                // 鼠标释放事件 - 根据当前工具执行不同操作
                canvas.setOnMouseReleased(event -> {
                    if (currentTool == BRUSH || currentTool == ERASER) {
                        isDrawing = false;
                    } else if (currentTool == CURSOR) {
                        isPanning = false;
                    }
                });

                // 创建工具栏
                HBox toolbar = new HBox(10);
                toolbar.setPadding(new Insets(10));
                toolbar.setStyle(
                        "-fx-background-color: #e0e0e0; -fx-border-color: #cccccc; -fx-border-width: 1px 0 0 0;");

                // 添加工具按钮
                final Button brushButton = new Button("画笔");
                brushButton.setStyle(
                        "-fx-font-weight: normal; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");

                final Button cursorButton = new Button("光标");
                cursorButton.setStyle(
                        "-fx-font-weight: bold; -fx-background-color: #d0e8ff; -fx-border-color: #66aaff; -fx-border-width: 1px;");

                final Button eraserButton = new Button("橡皮擦");
                eraserButton.setStyle(
                        "-fx-font-weight: normal; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");

                final Button zoomInButton = new Button("放大镜");
                zoomInButton
                        .setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");

                final Button zoomOutButton = new Button("缩小镜");
                zoomOutButton
                        .setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");

                final Button undoButton = new Button("撤回");
                undoButton.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");

                final Button redoButton = new Button("重做");
                redoButton.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");

                // 画笔按钮事件
                brushButton.setOnAction(e -> {
                    currentTool = BRUSH;
                    brushButton.setStyle(
                            "-fx-font-weight: bold; -fx-background-color: #d0e8ff; -fx-border-color: #66aaff; -fx-border-width: 1px;");
                    cursorButton.setStyle(
                            "-fx-font-weight: normal; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");
                    eraserButton.setStyle(
                            "-fx-font-weight: normal; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");
                });

                // 光标按钮事件
                cursorButton.setOnAction(e -> {
                    currentTool = CURSOR;
                    cursorButton.setStyle(
                            "-fx-font-weight: bold; -fx-background-color: #d0e8ff; -fx-border-color: #66aaff; -fx-border-width: 1px;");
                    brushButton.setStyle(
                            "-fx-font-weight: normal; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");
                    eraserButton.setStyle(
                            "-fx-font-weight: normal; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");
                });

                // 橡皮擦按钮事件
                eraserButton.setOnAction(e -> {
                    currentTool = ERASER;
                    eraserButton.setStyle(
                            "-fx-font-weight: bold; -fx-background-color: #d0e8ff; -fx-border-color: #66aaff; -fx-border-width: 1px;");
                    brushButton.setStyle(
                            "-fx-font-weight: normal; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");
                    cursorButton.setStyle(
                            "-fx-font-weight: normal; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px;");
                });

                // 放大按钮事件
                zoomInButton.setOnAction(e -> {
                    // 放大图片
                    double currentScale = largeImageView.getScaleX();
                    double newScale = Math.min(currentScale * 1.2, 5.0);
                    largeImageView.setScaleX(newScale);
                    largeImageView.setScaleY(newScale);
                    canvas.setScaleX(newScale);
                    canvas.setScaleY(newScale);
                    System.out.println("放大后的比例: " + newScale);
                });

                // 缩小按钮事件
                zoomOutButton.setOnAction(e -> {
                    // 缩小图片
                    double currentScale = largeImageView.getScaleX();
                    double newScale = Math.max(currentScale / 1.2, 0.1);
                    largeImageView.setScaleX(newScale);
                    largeImageView.setScaleY(newScale);
                    canvas.setScaleX(newScale);
                    canvas.setScaleY(newScale);
                    System.out.println("缩小后的比例: " + newScale);
                });

                // 撤回按钮事件
                undoButton.setOnAction(e -> {
                    if (historyIndex > 0) {
                        historyIndex--;
                        restoreFromHistory(canvas);
                        System.out.println("撤回操作，当前历史索引: " + historyIndex);
                        redoButton.setDisable(false);
                        if (historyIndex <= 0) {
                            undoButton.setDisable(true);
                        }
                    }
                });

                // 重做按钮事件
                redoButton.setOnAction(e -> {
                    if (historyIndex < history.size() - 1) {
                        historyIndex++;
                        restoreFromHistory(canvas);
                        System.out.println("重做操作，当前历史索引: " + historyIndex);
                        undoButton.setDisable(false);
                        if (historyIndex >= history.size() - 1) {
                            redoButton.setDisable(true);
                        }
                    }
                });

                // 初始化时禁用撤回和重做按钮
                undoButton.setDisable(true);
                redoButton.setDisable(true);

                // 将按钮添加到工具栏
                toolbar.getChildren().addAll(
                        new Label("工具栏: "),
                        brushButton,
                        cursorButton,
                        eraserButton,
                        zoomInButton,
                        zoomOutButton,
                        undoButton,
                        redoButton);

                // 添加提示标签
                Label tipLabel = new Label("提示: 按住Ctrl键并滚动鼠标滚轮可以放大/缩小图片; 使用工具栏切换工具");
                tipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-padding: 10px;");

                // 设置窗口内容
                BorderPane root = new BorderPane();
                root.setCenter(imagePane);
                root.setBottom(new VBox(toolbar, tipLabel));
                root.setStyle("-fx-background-color: #ffffff;");

                // 显示窗口
                Scene scene = new Scene(root, 820, 700);
                largeImageStage.setScene(scene);

                // 窗口关闭时询问是否保存涂鸦
                largeImageStage.setOnCloseRequest(event -> {
                    if (hasDrawn) {
                        // 询问是否保存涂鸦
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("保存涂鸦");
                        alert.setHeaderText("您已经进行了涂鸦，是否保存？");
                        alert.setContentText("选择'是'将替换原有的照片，选择'否'将不保存涂鸦");

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            // 保存涂鸦后的图片
                            try {
                                System.out.println("receiptFilePathField: " + receiptFilePathField);
                                // 将StackPane渲染为图片
                                WritableImage writableImage = new WritableImage(800, 600);
                                imagePane.snapshot(null, writableImage);

                                // 获取原图片路径
                                String originalPath = receiptFilePathField.getText();
                                System.out.println("originalPath: " + originalPath);
                                File originalFile = new File(originalPath);

                                // 保存涂鸦后的图片
                                File outputFile = new File(originalPath);
                                ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", outputFile);

                                System.out.println("涂鸦后的图片已保存到: " + outputFile.getAbsolutePath());

                                // 更新原ImageView显示的图片
                                Image updatedImage = new Image(outputFile.toURI().toString());
                                receiptImageView.setImage(updatedImage);
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("保存涂鸦失败: " + e.getMessage());
                            }
                        }
                    }
                });

                largeImageStage.showAndWait();
                System.out.println("大图窗口已关闭");
            } else {
                System.out.println("当前没有显示图片");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("显示大图失败: " + e.getMessage());
        }
    }

    /**
     * 删除票据（通过HTTP DELETE API）
     */
    @FXML
    private void deleteReceipt() {
        // 这里可以实现删除逻辑
        Receipt selectedReceipt = receiptTable.getSelectionModel().getSelectedItem();
        if (selectedReceipt != null) {
            // 弹出确认对话框
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText("删除票据");
            alert.setContentText("确定要删除ID为" + selectedReceipt.getId() + "的票据吗？此操作不可恢复。");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.out.println("删除票据: " + selectedReceipt.getId());
                try {
                    // 确保apiClient已初始化
                    if (apiClient == null) {
                        apiClient = RemoteApiClient.getInstance();
                    }
                    // 调用后端API删除票据
                    apiClient.delete("/api/receipts/" + selectedReceipt.getId(), Void.class);
                    System.out.println("票据删除成功");
                    // 刷新票据列表
                    loadReceipts();
                } catch (IOException e) {
                    System.err.println("删除票据失败: " + e.getMessage());
                    e.printStackTrace();
                    showError("删除失败", "无法删除票据：" + e.getMessage());
                }
            }
        } else {
            System.out.println("请选择要删除的票据");
        }
    }

    /**
     * 核销票据
     */
    @FXML
    public void verifyPayment() {
        // 打开核销票据窗口
        System.out.println("打开核销票据窗口 - 开始");
        Receipt selectedReceipt = receiptTable.getSelectionModel().getSelectedItem();
        if (selectedReceipt != null) {
            // 从后端API获取最新的票据信息，确保审核状态是最新的
            try {
                if (apiClient == null) {
                    apiClient = RemoteApiClient.getInstance();
                }
                Receipt freshReceipt = apiClient.get("/api/receipts/" + selectedReceipt.getId(), Receipt.class);
                if (freshReceipt != null) {
                    selectedReceipt = freshReceipt;
                }
            } catch (IOException e) {
                System.err.println("获取票据详情失败: " + e.getMessage());
                e.printStackTrace();
            }

            // 检查审核流程是否全部通过
            Integer approvalLevel = selectedReceipt.getCurrentApprovalLevel();
            if (approvalLevel == null || approvalLevel < 3) {
                System.out.println("审核流程未完成，无法核销");
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("无法核销");
                alert.setHeaderText("审核流程未完成");
                alert.setContentText("只有当全部审核流程通过后才可以进行票据核销。当前审核状态: " + selectedReceipt.getApprovalProgressText());
                alert.showAndWait();
                return;
            }
            try {
                System.out.println("已选择票据: " + selectedReceipt.getId());

                // 检查FXML文件是否存在
                String fxmlPath = "/fxml/receipt/verify_payment.fxml";
                System.out.println("尝试加载FXML文件: " + fxmlPath);
                System.out.println("FXML文件是否存在: " + (getClass().getResource(fxmlPath) != null));

                // 使用普通的FXML加载方式
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.setControllerFactory(this::createController);
                System.out.println("正在加载FXML文件...");
                Parent root = loader.load();
                System.out.println("FXML文件加载成功");

                // 直接获取控制器实例
                FXVerifyPaymentController controller = loader.getController();
                System.out.println("控制器实例: " + controller);

                // 调用初始化数据方法
                controller.initData();

                // 传递票据信息到核销控制器
                controller.setReceipt(selectedReceipt);

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("核销票据");
                WindowUtils.setStageIcon(stage);
                stage.setScene(new Scene(root));

                System.out.println("显示核销窗口...");
                stage.showAndWait();
                // 刷新票据列表
                loadReceipts();
                System.out.println("票据列表已刷新");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("打开核销票据窗口失败: " + e.getMessage());
            }
        } else {
            System.out.println("请选择要核销的票据");
        }
        System.out.println("打开核销票据窗口 - 结束");
    }

    /**
     * 验证票据（通过HTTP POST API）
     */
    @FXML
    private void executeVerify() {
        // 执行票据验证
        System.out.println("开始执行票据验证");
        Receipt selectedReceipt = receiptTable.getSelectionModel().getSelectedItem();
        if (selectedReceipt == null) {
            showError("错误", "请先选择要验证的票据");
            return;
        }

        // 获取验证状态
        String status = verifyStatusComboBox != null ? verifyStatusComboBox.getValue() : null;
        if (status == null || status.isEmpty()) {
            showError("错误", "请选择验证结果（已验证/已拒绝）");
            return;
        }

        // 获取验证人ID - 从ClientSession中获取
        User currentUser = clientSession != null ? clientSession.getCurrentUser() : null;
        Integer verifierId = currentUser != null ? currentUser.getUserId() : null;
        if (verifierId == null) {
            showError("错误", "未找到登录用户信息，请重新登录");
            return;
        }

        // 根据中文状态转换为对应的英文状态
        String statusEnglish;
        switch (status) {
            case "已验证":
                statusEnglish = "verified";
                break;
            case "已拒绝":
                statusEnglish = "rejected";
                break;
            default:
                statusEnglish = "pending";
        }

        System.out.println("验证票据: " + selectedReceipt.getId() + ", 状态: " + statusEnglish + ", 验证人: " + verifierId);

        try {
            // 确保apiClient已初始化
            if (apiClient == null) {
                apiClient = RemoteApiClient.getInstance();
            }

            // 构建请求体
            Map<String, Object> verifyRequest = new HashMap<>();
            verifyRequest.put("verifierId", verifierId);
            verifyRequest.put("status", statusEnglish);

            // 调用后端API验证票据
            apiClient.post("/api/receipts/" + selectedReceipt.getId() + "/verify", verifyRequest, Void.class);

            showInfo("成功", "票据验证成功！");
            // 刷新表格
            loadReceipts();
            // 更新验证状态显示
            if (verificationStatusLabel != null) {
                verificationStatusLabel.setText(status);
                verificationStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                        ("verified".equals(statusEnglish) ? "#4CAF50" : "#F44336") +
                        "; -fx-padding: 2 8; -fx-background-color: " +
                        ("verified".equals(statusEnglish) ? "#E8F5E9" : "#FFEBEE") + "; -fx-background-radius: 3;");
            }
        } catch (IOException e) {
            System.err.println("票据验证失败: " + e.getMessage());
            e.printStackTrace();
            showError("错误", "票据验证失败，请重试：" + e.getMessage());
        }
    }

    @FXML
    private void verify() {
        // 兼容旧方法调用，直接调用 executeVerify
        executeVerify();
    }

    /**
     * 票据选择事件处理
     */
    @FXML
    private void onReceiptSelected() {
        Receipt selectedReceipt = receiptTable.getSelectionModel().getSelectedItem();
        if (selectedReceipt != null) {
            System.out.println("选择票据: " + selectedReceipt.getId());

            // 填充票据信息
            if (receiptIdField != null) {
                receiptIdField.setText(String.valueOf(selectedReceipt.getId()));
            }
            if (reporterNameField != null) {
                reporterNameField.setText(selectedReceipt.getReporterName());
            }
            if (projectNameField != null) {
                projectNameField.setText(selectedReceipt.getProjectName());
            }
            if (amountField != null && selectedReceipt.getAmount() != null) {
                amountField.setText(selectedReceipt.getAmount().toString());
            }
            if (descriptionField != null) {
                descriptionField
                        .setText(selectedReceipt.getDescription() != null ? selectedReceipt.getDescription() : "");
            }

            // 显示发票图片
            if (receiptImageView != null && selectedReceipt.getFilePath() != null) {
                try {
                    File imageFile = new File(selectedReceipt.getFilePath());
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        receiptImageView.setImage(image);
                    }
                } catch (Exception e) {
                    System.out.println("加载图片失败: " + e.getMessage());
                }
            }

            // 显示OCR结果
            if (ocrResultTextArea != null) {
                ocrResultTextArea.setText(selectedReceipt.getOcrData());
            }
        }
    }

    /**
     * 查看票据
     */
    @FXML
    private void viewReceipt() {
        // 实现查看票据逻辑
        System.out.println("查看票据");
        Receipt selectedReceipt = receiptTable.getSelectionModel().getSelectedItem();
        if (selectedReceipt != null) {
            System.out.println("查看票据: " + selectedReceipt.getId());

            // 显示OCR结果
            if (ocrResultTextArea != null) {
                ocrResultTextArea.setText(selectedReceipt.getOcrData());
            }

            // 显示发票原件
            if (receiptImageView != null) {
                try {
                    String filePath = selectedReceipt.getFilePath();
                    if (filePath != null && !filePath.isEmpty()) {
                        File imageFile = new File(filePath);
                        if (imageFile.exists()) {
                            Image image = new Image(imageFile.toURI().toString());
                            receiptImageView.setImage(image);
                        } else {
                            System.out.println("图片文件不存在: " + filePath);
                            receiptImageView.setImage(null);
                        }
                    } else {
                        System.out.println("图片文件路径为空");
                        receiptImageView.setImage(null);
                    }
                } catch (Exception e) {
                    System.out.println("显示图片失败: " + e.getMessage());
                    e.printStackTrace();
                    receiptImageView.setImage(null);
                }
            }

            // 显示三级审核进度
            updateApprovalProgress(selectedReceipt);

            // 加载审核历史
            loadApprovalHistory(selectedReceipt.getId());
        } else {
            System.out.println("请选择要查看的票据");
            // 清空显示
            if (ocrResultTextArea != null) {
                ocrResultTextArea.clear();
            }
            if (receiptImageView != null) {
                receiptImageView.setImage(null);
            }
            // 清空审核进度
            clearApprovalProgress();
        }
    }

    /**
     * 更新三级审核进度显示
     */
    private void updateApprovalProgress(Receipt receipt) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        // 财务审核
        if (financeStatusLabel != null) {
            financeStatusLabel.setText(receipt.getFinanceStatusText());
        }
        if (financeInfoLabel != null && receipt.getFinanceVerifiedAt() != null) {
            financeInfoLabel.setText(sdf.format(receipt.getFinanceVerifiedAt()));
        } else if (financeInfoLabel != null) {
            financeInfoLabel.setText("");
        }

        // 项目经理审核
        if (projectManagerStatusLabel != null) {
            projectManagerStatusLabel.setText(receipt.getProjectManagerStatusText());
        }
        if (projectManagerInfoLabel != null && receipt.getProjectManagerVerifiedAt() != null) {
            projectManagerInfoLabel.setText(sdf.format(receipt.getProjectManagerVerifiedAt()));
        } else if (projectManagerInfoLabel != null) {
            projectManagerInfoLabel.setText("");
        }

        // 经理审核
        if (managerStatusLabel != null) {
            managerStatusLabel.setText(receipt.getManagerStatusText());
        }
        if (managerInfoLabel != null && receipt.getManagerVerifiedAt() != null) {
            managerInfoLabel.setText(sdf.format(receipt.getManagerVerifiedAt()));
        } else if (managerInfoLabel != null) {
            managerInfoLabel.setText("");
        }

        // 当前审核级别
        Integer currentLevel = receipt.getCurrentApprovalLevel();
        if (currentLevel == null) {
            currentLevel = 0;
        }
        String levelText = "";
        if (currentLevel == 0) {
            levelText = "财务审核";
        } else if (currentLevel == 1) {
            levelText = "项目经理审核";
        } else if (currentLevel == 2) {
            levelText = "经理审核";
        } else if (currentLevel >= 3) {
            levelText = "审核完成";
        }
        if (currentApprovalLevelLabel != null) {
            currentApprovalLevelLabel.setText(levelText);
        }
    }

    /**
     * 清空审核进度显示
     */
    private void clearApprovalProgress() {
        if (financeStatusLabel != null) {
            financeStatusLabel.setText("待审核");
        }
        if (financeInfoLabel != null) {
            financeInfoLabel.setText("");
        }
        if (projectManagerStatusLabel != null) {
            projectManagerStatusLabel.setText("待审核");
        }
        if (projectManagerInfoLabel != null) {
            projectManagerInfoLabel.setText("");
        }
        if (managerStatusLabel != null) {
            managerStatusLabel.setText("待审核");
        }
        if (managerInfoLabel != null) {
            managerInfoLabel.setText("");
        }
        if (currentApprovalLevelLabel != null) {
            currentApprovalLevelLabel.setText("财务审核");
        }
    }

    /**
     * 加载审核历史（通过HTTP API）
     */
    private void loadApprovalHistory(Integer receiptId) {
        if (approvalHistoryTable != null) {
            try {
                // 确保apiClient已初始化
                if (apiClient == null) {
                    apiClient = RemoteApiClient.getInstance();
                }

                // 调用后端API获取审批历史
                ApprovalRecordListResponse response = apiClient.get(
                    "/api/receipts/" + receiptId + "/approval-history",
                    ApprovalRecordListResponse.class
                );

                if (response != null && response.getRecords() != null) {
                    approvalHistoryTable.setItems(FXCollections.observableArrayList(response.getRecords()));
                } else {
                    approvalHistoryTable.setItems(FXCollections.observableArrayList());
                }
            } catch (Exception e) {
                System.out.println("加载审核历史失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理三级审核（通过HTTP POST API）
     */
    @FXML
    private void processApproval() {
        Receipt selectedReceipt = receiptTable.getSelectionModel().getSelectedItem();
        if (selectedReceipt == null) {
            System.out.println("请选择要审核的票据");
            return;
        }

        if (approvalActionComboBox == null || approvalCommentTextArea == null) {
            System.out.println("审核组件未初始化");
            return;
        }

        String action = approvalActionComboBox.getValue();
        String status = "通过".equals(action) ? "approved" : "rejected";
        String comment = approvalCommentTextArea.getText();

        // 从ClientSession获取当前用户信息
        User currentUser = clientSession != null ? clientSession.getCurrentUser() : null;
        Integer approverId = currentUser != null ? currentUser.getUserId() : null;
        String approverName = currentUser != null ? currentUser.getName() : "未知";

        if (approverId == null) {
            showError("错误", "未找到登录用户信息，请重新登录");
            return;
        }

        try {
            // 确保apiClient已初始化
            if (apiClient == null) {
                apiClient = RemoteApiClient.getInstance();
            }

            // 构建请求体
            Map<String, Object> approvalRequest = new HashMap<>();
            approvalRequest.put("approverId", approverId);
            approvalRequest.put("approverName", approverName);
            approvalRequest.put("status", status);
            approvalRequest.put("comment", comment);

            // 调用后端API处理审核
            apiClient.post("/api/receipts/" + selectedReceipt.getId() + "/approval", approvalRequest, Void.class);

            System.out.println("审核提交成功");
            // 刷新票据列表
            loadReceipts();
            // 清空审核意见
            approvalCommentTextArea.clear();

            // 重新加载票据详情
            Receipt updatedReceipt = apiClient.get("/api/receipts/" + selectedReceipt.getId(), Receipt.class);
            if (updatedReceipt != null) {
                updateApprovalProgress(updatedReceipt);
                loadApprovalHistory(updatedReceipt.getId());
            }
        } catch (IOException e) {
            System.err.println("审核提交失败: " + e.getMessage());
            e.printStackTrace();
            showError("审核失败", "无法提交审核：" + e.getMessage());
        }
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
     * 票据列表响应内部类（用于解析分页API响应）
     */
    private static class ReceiptListResponse {
        private List<Receipt> content;
        private int totalPages;
        private int totalElements;
        private int number;
        private int size;

        public List<Receipt> getContent() {
            return content;
        }

        public void setContent(List<Receipt> content) {
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
     * 审批记录列表响应内部类
     */
    private static class ApprovalRecordListResponse {
        private List<ApprovalRecord> records;

        public List<ApprovalRecord> getRecords() {
            return records;
        }

        public void setRecords(List<ApprovalRecord> records) {
            this.records = records;
        }
    }
}