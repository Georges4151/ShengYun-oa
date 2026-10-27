package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Project;
import com.ceams.model.entity.Receipt;
import com.ceams.model.entity.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FXReceiptUploadController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    // 保存选择的文件
    private File selectedFile;

    @FXML
    private TextField userIdField;

    @FXML
    private TextField receiptNumberField;

    @FXML
    private ComboBox<String> reporterComboBox;

    @FXML
    private ComboBox<Project> projectComboBox;

    @FXML
    private DatePicker receiptDatePicker;

    @FXML
    private TextField amountField;

    @FXML
    private TextField typeField;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField filePathField;

    // 票据类型下拉框
    @FXML
    private ComboBox<String> typeComboBox;

    // 拍摄预览区域
    @FXML
    private VBox captureSection;

    @FXML
    private ImageView capturePreview;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化报票人下拉列表
        loadReporters();
        // 初始化项目下拉列表
        loadProjects();
        // 初始化票据类型下拉列表
        loadReceiptTypes();
        // 初始化票据日期为今天
        receiptDatePicker.setValue(java.time.LocalDate.now());
    }

    /**
     * 初始化票据类型列表
     */
    private void loadReceiptTypes() {
        if (typeComboBox != null) {
            typeComboBox.getItems().addAll(
                    "增值税发票",
                    "普通发票",
                    "收据",
                    "银行票据",
                    "支票",
                    "汇票",
                    "本票",
                    "其他");
            typeComboBox.setValue("增值税发票");
        }
    }

    /**
     * 自动生成票据编号
     */
    @FXML
    private void generateReceiptNumber() {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
        String randomNum = String.format("%04d", (int) (Math.random() * 10000));
        String generatedNumber = "RC-" + timestamp + "-" + randomNum;
        receiptNumberField.setText(generatedNumber);
    }

    /**
     * 打开摄像头拍摄窗口
     */
    @FXML
    private void openCamera() {
        try {
            // 获取当前选择的项信息
            Integer currentProjectId = null;
            String currentProjectName = null;
            if (projectComboBox != null && projectComboBox.getValue() != null) {
                currentProjectId = projectComboBox.getValue().getId();
                currentProjectName = projectComboBox.getValue().getProjectName();
            }

            // 打开摄像头拍摄窗口
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/camera/camera-capture.fxml"));
            Parent root = loader.load();

            // 传递项目信息给摄像头控制器
            CameraCaptureController captureController = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("摄像头拍摄 - " + (currentProjectName != null ? currentProjectName : "未选择项目"));
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));

            // 显示窗口并等待关闭
            stage.showAndWait();

            // 获取拍摄结果
            File capturedFile = captureController.getCapturedImageFile();

            if (capturedFile != null && capturedFile.exists()) {
                // 更新文件路径显示
                selectedFile = capturedFile;
                filePathField.setText(capturedFile.getAbsolutePath());

                // 显示预览
                Image image = new Image(capturedFile.toURI().toString());
                capturePreview.setImage(image);
                captureSection.setVisible(true);
                captureSection.setManaged(true);

                showAlert(Alert.AlertType.INFORMATION, "成功", "图像已拍摄并保存到项目文件夹");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "打开摄像头失败: " + e.getMessage());
        }
    }

    /**
     * 显示提示信息
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 加载报票人到下拉列表（根据角色权限）
     */
    private void loadReporters() {
        if (clientSession == null || !clientSession.isLoggedIn()) {
            System.out.println("用户未登录，无法加载报票人列表");
            return;
        }

        User currentUser = clientSession.getCurrentUser();
        String role = currentUser.getRole();

        // 只有 ADMIN 和 FINANCE 可以选择所有用户，MANAGER 和 STAFF 只能选自己
        boolean canSelectAll = "ADMIN".equals(role) || "FINANCE".equals(role);

        ObservableList<String> reporterList = FXCollections.observableArrayList();

        if (canSelectAll) {
            // 管理员/财务：通过HTTP API加载所有用户
            try {
                User[] users = apiClient.get("/api/users", User[].class);
                if (users != null) {
                    for (User user : users) {
                        reporterList.add(user.getName());
                    }
                }
                // 可编辑，允许选择
                reporterComboBox.setEditable(true);
                reporterComboBox.setDisable(false);
            } catch (Exception e) {
                System.err.println("加载用户列表失败: " + e.getMessage());
                e.printStackTrace();
                // 降级处理：至少显示当前用户
                reporterList.add(currentUser.getName());
                reporterComboBox.setValue(currentUser.getName());
                reporterComboBox.setEditable(false);
                reporterComboBox.setDisable(true);
            }
        } else {
            // 经理/普通员工：只能选自己
            reporterList.add(currentUser.getName());
            // 禁用下拉，只显示自己
            reporterComboBox.setValue(currentUser.getName());
            reporterComboBox.setEditable(false);
            reporterComboBox.setDisable(true);
        }

        reporterComboBox.setItems(reporterList);
    }

    /**
     * 加载所有项目到下拉列表
     */
    private void loadProjects() {
        try {
            // 通过HTTP API获取所有项目
            Project[] projects = apiClient.get("/api/projects", Project[].class);
            ObservableList<Project> projectList = FXCollections.observableArrayList();
            if (projects != null) {
                for (Project project : projects) {
                    projectList.add(project);
                }
            }
            projectComboBox.setItems(projectList);
            // 设置下拉列表显示的属性
            projectComboBox.setCellFactory(param -> new javafx.scene.control.ListCell<Project>() {
                @Override
                protected void updateItem(Project project, boolean empty) {
                    super.updateItem(project, empty);
                    if (empty || project == null) {
                        setText(null);
                    } else {
                        setText(project.getProjectName());
                    }
                }
            });
            // 设置选中项显示的属性
            projectComboBox.setButtonCell(new javafx.scene.control.ListCell<Project>() {
                @Override
                protected void updateItem(Project project, boolean empty) {
                    super.updateItem(project, empty);
                    if (empty || project == null) {
                        setText(null);
                    } else {
                        setText(project.getProjectName());
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("加载项目列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 选择文件
     */
    @FXML
    private void selectFile() {
        // 创建文件选择对话框
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择票据文件");

        // 设置文件过滤器
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("PDF文件", "*.pdf"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));

        // 显示文件选择对话框
        Stage stage = (Stage) filePathField.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        // 如果选择了文件，显示文件路径
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
            System.out.println("选择了文件: " + selectedFile.getAbsolutePath());
        }
    }

    /**
     * 上传票据
     */
    @FXML
    private void uploadReceiptAction() {
        try {
            // 检查是否选择了文件
            System.out.println("开始上传票据，selectedFile状态:");
            System.out.println("selectedFile = " + selectedFile);
            System.out.println("filePathField.getText() = " + filePathField.getText());
            if (selectedFile == null) {
                // 如果selectedFile为null，但filePathField有值，尝试从filePathField获取文件
                if (filePathField != null && !filePathField.getText().isEmpty()) {
                    selectedFile = new File(filePathField.getText());
                    System.out.println("从filePathField重新获取文件: " + selectedFile.getAbsolutePath());
                }
                if (selectedFile == null) {
                    System.out.println("请先选择文件");
                    return;
                }
            }

            // 检查用户是否登录
            if (clientSession == null || !clientSession.isLoggedIn()) {
                System.out.println("用户未登录，无法上传票据");
                showAlert(Alert.AlertType.ERROR, "错误", "用户未登录，请先登录后再上传票据");
                return;
            }

            // 获取用户ID - 从客户端会话中获取
            User currentUser = clientSession.getCurrentUser();
            Integer userId = currentUser.getUserId();
            if (userId == null) {
                System.out.println("用户ID为空，无法上传票据");
                showAlert(Alert.AlertType.ERROR, "错误", "用户信息异常，请重新登录");
                return;
            }
            System.out.println("当前登录用户ID: " + userId);

            // 获取类型
            String type = "invoice";
            if (typeComboBox != null && typeComboBox.getValue() != null) {
                String selectedType = typeComboBox.getValue();
                // 转换为系统内部类型
                switch (selectedType) {
                    case "增值税发票":
                        type = "VAT_INVOICE";
                        break;
                    case "普通发票":
                        type = "NORMAL_INVOICE";
                        break;
                    case "收据":
                        type = "RECEIPT";
                        break;
                    case "银行票据":
                        type = "BANK_BILL";
                        break;
                    case "支票":
                        type = "CHEQUE";
                        break;
                    case "汇票":
                        type = "DRAFT";
                        break;
                    case "本票":
                        type = "CASHIER_CHECK";
                        break;
                    default:
                        type = "OTHER";
                }
            } else if (typeField != null && !typeField.getText().isEmpty()) {
                type = typeField.getText();
            }

            // 获取票据日期
            java.util.Date receiptDate = null;
            if (receiptDatePicker != null && receiptDatePicker.getValue() != null) {
                receiptDate = java.util.Date
                        .from(receiptDatePicker.getValue().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            }

            // 获取金额
            java.math.BigDecimal amount = null;
            if (amountField != null && !amountField.getText().isEmpty()) {
                try {
                    amount = new java.math.BigDecimal(amountField.getText());
                    // 验证金额范围，确保不超过decimal(12,2)的限制
                    java.math.BigDecimal maxAmount = new java.math.BigDecimal("9999999999.99");
                    if (amount.compareTo(maxAmount) > 0) {
                        showAlert(Alert.AlertType.ERROR, "错误", "金额不能超过9999999999.99");
                        return;
                    }
                    if (amount.compareTo(java.math.BigDecimal.ZERO) < 0) {
                        showAlert(Alert.AlertType.ERROR, "错误", "金额不能为负数");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "错误", "无效的金额格式");
                    return;
                }
            }

            // 获取报票人ID
            Integer reporterId = null;
            String reporterName = null;
            if (reporterComboBox != null && reporterComboBox.getValue() != null) {
                reporterName = reporterComboBox.getValue();
                System.out.println("选择的报票人姓名: " + reporterName);

                // 通过HTTP API获取用户列表，根据姓名查找用户ID
                try {
                    User[] users = apiClient.get("/api/users", User[].class);
                    if (users != null) {
                        for (User user : users) {
                            if (reporterName.equals(user.getName())) {
                                reporterId = user.getUserId();
                                System.out.println("对应的报票人ID: " + reporterId);
                                break;
                            }
                        }
                    }
                    if (reporterId == null) {
                        System.out.println("未找到对应报票人，将使用null作为报票人ID");
                    }
                } catch (Exception e) {
                    System.err.println("查找报票人ID失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 获取归属项目ID
            Integer projectId = null;
            if (projectComboBox != null && projectComboBox.getValue() != null) {
                projectId = projectComboBox.getValue().getId();
                System.out.println("选择的项目ID: " + projectId);
            }

            // 获取描述
            String description = null;
            if (descriptionField != null && !descriptionField.getText().isEmpty()) {
                description = descriptionField.getText();
                System.out.println("票据描述: " + description);
            }

            // 获取票据编号
            String receiptNumber = null;
            if (receiptNumberField != null && !receiptNumberField.getText().trim().isEmpty()) {
                receiptNumber = receiptNumberField.getText().trim();
                System.out.println("票据编号: " + receiptNumber);

                // 通过HTTP API检查票据编号是否重复
                try {
                    Map<String, String> params = new HashMap<>();
                    params.put("receiptNumber", receiptNumber);
                    DuplicateCheckResponse duplicateResponse = apiClient.get(
                            "/api/receipts/check-duplicate", params, DuplicateCheckResponse.class);
                    if (duplicateResponse != null && duplicateResponse.isDuplicate()) {
                        showAlert(Alert.AlertType.WARNING, "票据编号重复",
                                "系统中已存在编号为 [" + receiptNumber + "] 的票据。\n\n请使用不同的票据编号，或联系管理员确认该票据是否已录入。");
                        receiptNumberField.requestFocus();
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("检查票据编号重复失败: " + e.getMessage());
                    e.printStackTrace();
                    // 如果检查失败，继续上传，让后端再次验证
                }
            }

            // 准备上传参数
            Map<String, String> uploadParams = new HashMap<>();
            uploadParams.put("userId", String.valueOf(userId));
            if (reporterId != null) {
                uploadParams.put("reporterId", String.valueOf(reporterId));
            }
            if (projectId != null) {
                uploadParams.put("projectId", String.valueOf(projectId));
            }
            uploadParams.put("type", type);
            if (reporterName != null) {
                uploadParams.put("reporterName", reporterName);
            }
            if (description != null) {
                uploadParams.put("description", description);
            }
            if (receiptNumber != null) {
                uploadParams.put("receiptNumber", receiptNumber);
            }
            if (receiptDate != null) {
                uploadParams.put("receiptDate", new java.text.SimpleDateFormat("yyyy-MM-dd").format(receiptDate));
            }
            if (amount != null) {
                uploadParams.put("amount", amount.toPlainString());
            }

            // 通过HTTP API上传票据文件（multipart/form-data）
            Receipt receipt = apiClient.uploadFile(
                    "/api/receipts/upload",
                    selectedFile,
                    "file",
                    uploadParams,
                    Receipt.class);

            if (receipt != null && receipt.getId() != null) {
                System.out.println("票据上传成功: " + receipt.getId());
                showAlert(Alert.AlertType.INFORMATION, "成功", "票据上传成功！");

                // 关闭当前窗口
                Stage stage = (Stage) filePathField.getScene().getWindow();
                stage.close();
            } else {
                System.out.println("票据上传失败，服务器返回空结果");
                showAlert(Alert.AlertType.ERROR, "错误", "票据上传失败，请稍后重试");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("票据上传失败: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "错误", "票据上传失败: " + e.getMessage());
        }
    }

    /**
     * 票据编号重复检查响应内部类
     */
    private static class DuplicateCheckResponse {
        private boolean exists;

        public boolean isDuplicate() {
            return exists;
        }

        public void setExists(boolean exists) {
            this.exists = exists;
        }
    }

    /**
     * 取消操作
     */
    @FXML
    private void cancelEdit() {
        // 关闭当前窗口
        if (filePathField != null) {
            Stage stage = (Stage) filePathField.getScene().getWindow();
            stage.close();
        } else {
            System.out.println("无法获取当前窗口，取消操作失败");
        }
    }
}