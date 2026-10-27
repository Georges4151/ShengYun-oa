package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Project;
import com.ceams.model.entity.SubcontractorContract;
import com.ceams.model.entity.SettlementProof;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

public class FXSubcontractorContractController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TableView<SubcontractorContract> contractTable;

    @FXML
    private TableColumn<SubcontractorContract, Integer> idColumn;

    @FXML
    private TableColumn<SubcontractorContract, String> contractNameColumn;

    @FXML
    private TableColumn<SubcontractorContract, String> contractNumberColumn;

    @FXML
    private TableColumn<SubcontractorContract, String> projectColumn;

    @FXML
    private TableColumn<SubcontractorContract, String> contractAmountColumn;

    @FXML
    private TableColumn<SubcontractorContract, String> statusColumn;

    @FXML
    private TableColumn<SubcontractorContract, String> fileNameColumn;

    @FXML
    private TextField contractNameField;

    @FXML
    private TextField contractNumberField;

    @FXML
    private ComboBox<Project> projectComboBox;

    @FXML
    private TextField contractAmountField;

    @FXML
    private TextField paidAmountField;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField filePathField;

    // 当前编辑的合同
    private SubcontractorContract editingContract;
    // 分包商ID
    private Integer subcontractorId;

    private static String getFileStoragePath() {
        String appPath = System.getProperty("user.dir");
        return appPath + File.separator + "contracts";
    }

    private static final String FILE_STORAGE_PATH = getFileStoragePath();

    // 临时存储文件路径和文件名（用于新合同）
    private String tempFilePath;
    private String tempFileName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("FXSubcontractorContractController.initialize() 开始执行");

        // 检查apiClient是否已注入
        if (apiClient == null) {
            System.out.println("apiClient尚未注入，使用默认单例");
            apiClient = RemoteApiClient.getInstance();
        }

        // 初始化表格列
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        contractNameColumn.setCellValueFactory(new PropertyValueFactory<>("contractName"));
        contractNumberColumn.setCellValueFactory(new PropertyValueFactory<>("contractNumber"));
        // 项目名称需要特殊处理
        projectColumn.setCellValueFactory(cellData -> {
            SubcontractorContract contract = cellData.getValue();
            if (contract.getProject() != null) {
                return new javafx.beans.property.SimpleStringProperty(contract.getProject().getProjectName());
            } else {
                return new javafx.beans.property.SimpleStringProperty("无");
            }
        });
        contractAmountColumn.setCellValueFactory(new PropertyValueFactory<>("contractAmount"));
        // 初始化已支付金额列
        if (contractTable.getColumns().size() > 5) {
            TableColumn<?, ?> column = contractTable.getColumns().get(5);
            if (column instanceof TableColumn<?, ?>) {
                @SuppressWarnings("unchecked")
                TableColumn<SubcontractorContract, java.math.BigDecimal> paidAmountColumn = (TableColumn<SubcontractorContract, java.math.BigDecimal>) column;
                paidAmountColumn.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
            }
        }
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));

        // 初始化下拉框
        initializeComboBoxes();

        // 确保文件存储目录存在
        ensureStorageDirectoryExists();

        // 添加双击事件监听器
        contractTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                // 双击事件
                SubcontractorContract selected = contractTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    // 填充表单显示合同详情
                    editingContract = selected;
                    fillForm(selected);
                    // 显示详情时设置为不可编辑
                    setFormEditable(false);
                }
            }
        });

        // 初始设置表单为不可编辑
        setFormEditable(false);
    }

    /**
     * 设置表单字段的可编辑状态
     */
    private void setFormEditable(boolean editable) {
        contractNameField.setEditable(editable);
        contractNumberField.setEditable(editable);
        projectComboBox.setDisable(!editable);
        contractAmountField.setEditable(editable);
        paidAmountField.setEditable(editable);
        startDatePicker.setDisable(!editable);
        endDatePicker.setDisable(!editable);
        statusComboBox.setDisable(!editable);
        descriptionField.setEditable(editable);
    }

    /**
     * 设置分包商ID
     */
    public void setSubcontractorId(Integer subcontractorId) {
        this.subcontractorId = subcontractorId;
        // 加载合同列表
        loadContracts();
    }

    /**
     * 初始化下拉框
     */
    private void initializeComboBoxes() {
        // 加载项目列表
        try {
            ProjectListResponse response = apiClient.get("/api/projects", ProjectListResponse.class);
            List<Project> projects = response != null && response.getContent() != null ? response.getContent() : new java.util.ArrayList<>();
            ObservableList<Project> projectItems = FXCollections.observableArrayList(projects);
            projectComboBox.setItems(projectItems);

            // 设置项目下拉框的显示方式
            projectComboBox.setCellFactory(param -> new ListCell<Project>() {
                @Override
                protected void updateItem(Project item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getProjectName());
                    }
                }
            });

            // 设置项目下拉框按钮的显示方式
            projectComboBox.setButtonCell(new ListCell<Project>() {
                @Override
                protected void updateItem(Project item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getProjectName());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("加载项目列表失败: " + e.getMessage());
        }

        // 初始化状态下拉框
        ObservableList<String> statusItems = FXCollections.observableArrayList(
                "未开始", "进行中", "已完成", "已终止");
        statusComboBox.setItems(statusItems);
    }

    /**
     * 确保文件存储目录存在
     */
    private void ensureStorageDirectoryExists() {
        Path storagePath = Paths.get(FILE_STORAGE_PATH);
        if (!Files.exists(storagePath)) {
            try {
                Files.createDirectories(storagePath);
                System.out.println("创建文件存储目录: " + FILE_STORAGE_PATH);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("创建文件存储目录失败: " + e.getMessage());
            }
        }
    }

    /**
     * 加载合同列表
     */
    private void loadContracts() {
        try {
            if (subcontractorId != null) {
                ContractListResponse response = apiClient.get("/api/subcontractors/" + subcontractorId + "/contracts", ContractListResponse.class);
                List<SubcontractorContract> contracts = response != null && response.getContent() != null ? response.getContent() : new java.util.ArrayList<>();
                ObservableList<SubcontractorContract> data = FXCollections.observableArrayList(contracts);
                contractTable.setItems(data);
                System.out.println("加载合同数据: " + contracts.size() + " 条记录");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("加载合同数据失败: " + e.getMessage());
        }
    }

    /**
     * 刷新合同列表
     */
    @FXML
    private void refresh() {
        loadContracts();
    }

    /**
     * 添加合同
     */
    @FXML
    private void addContract() {
        System.out.println("添加合同");
        // 清空表单
        clearForm();
        editingContract = null;
        // 设置表单为可编辑
        setFormEditable(true);
    }

    /**
     * 编辑合同
     */
    @FXML
    private void editContract() {
        System.out.println("编辑合同");
        // 获取选中的合同
        SubcontractorContract selected = contractTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText("未选择合同");
            alert.setContentText("请选择要编辑的合同");
            alert.showAndWait();
            return;
        }

        // 填充表单
        editingContract = selected;
        fillForm(selected);
        // 设置表单为可编辑
        setFormEditable(true);
    }

    /**
     * 删除合同
     */
    @FXML
    private void deleteContract() {
        System.out.println("删除合同");
        // 获取选中的合同
        SubcontractorContract selected = contractTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText("未选择合同");
            alert.setContentText("请选择要删除的合同");
            alert.showAndWait();
            return;
        }

        // 确认删除
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确定要删除合同 " + selected.getContractName() + " 吗？");
        alert.setContentText("此操作不可撤销");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 删除文件
            if (selected.getFilePath() != null) {
                File file = new File(selected.getFilePath());
                if (file.exists()) {
                    file.delete();
                    System.out.println("删除合同文件: " + selected.getFilePath());
                }
            }

            // 删除合同记录
            try {
                apiClient.delete("/api/subcontractors/contracts/" + selected.getId(), Void.class);
                System.out.println("合同删除成功");
                // 刷新列表
                loadContracts();
                // 清空表单
                clearForm();
            } catch (IOException ex) {
                System.out.println("合同删除失败: " + ex.getMessage());
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("错误");
                errorAlert.setHeaderText("合同删除失败");
                errorAlert.setContentText("请稍后重试");
                errorAlert.showAndWait();
            }
        }
    }

    /**
     * 上传合同文件
     */
    @FXML
    private void uploadContractFile() {
        System.out.println("===== 上传合同文件开始 =====");
        System.out.println("当前编辑合同: " + (editingContract != null ? editingContract.getId() : "null"));
        System.out.println("临时文件路径: " + tempFilePath);
        System.out.println("临时文件名: " + tempFileName);

        // 创建文件选择器
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择PDF文件");

        // 设置文件过滤器
        FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF文件 (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(pdfFilter);

        // 显示文件选择对话框
        Stage stage = (Stage) filePathField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                System.out.println("选择的文件: " + selectedFile.getAbsolutePath());
                System.out.println("文件大小: " + selectedFile.length() + " 字节");

                // 检查文件类型
                if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("文件类型错误");
                    alert.setContentText("请选择PDF文件");
                    alert.showAndWait();
                    return;
                }

                // 生成存储文件名
                String fileName = selectedFile.getName();
                String storageFileName = System.currentTimeMillis() + "_" + fileName;
                Path storagePath = Paths.get(FILE_STORAGE_PATH, storageFileName);
                System.out.println("存储路径: " + storagePath.toString());

                // 确保存储目录存在
                if (!Files.exists(storagePath.getParent())) {
                    Files.createDirectories(storagePath.getParent());
                    System.out.println("创建存储目录: " + storagePath.getParent().toString());
                }

                // 复制文件到存储目录
                Files.copy(selectedFile.toPath(), storagePath);
                System.out.println("文件上传成功: " + storagePath.toString());
                System.out.println("存储文件大小: " + Files.size(storagePath) + " 字节");

                // 更新表单
                filePathField.setText(selectedFile.getName());

                // 如果正在编辑合同，更新合同的文件信息
                if (editingContract != null) {
                    System.out.println("更新编辑合同的文件信息");
                    editingContract.setFilePath(storagePath.toString());
                    editingContract.setFileName(fileName);
                    System.out.println("编辑合同文件路径: " + editingContract.getFilePath());
                } else {
                    // 如果是新合同，存储到临时变量
                    System.out.println("存储到临时变量");
                    tempFilePath = storagePath.toString();
                    tempFileName = fileName;
                    System.out.println("临时文件路径: " + tempFilePath);
                    System.out.println("临时文件名: " + tempFileName);
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("文件上传失败: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText("文件上传失败");
                alert.setContentText("错误信息: " + e.getMessage());
                alert.showAndWait();
            }
        } else {
            System.out.println("用户取消了文件选择");
        }
        System.out.println("===== 上传合同文件结束 =====");
    }

    /**
     * 查看合同文件
     */
    @FXML
    private void viewContractFile() {
        System.out.println("查看合同文件");
        // 获取选中的合同
        SubcontractorContract selected = contractTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText("未选择合同");
            alert.setContentText("请选择要查看文件的合同");
            alert.showAndWait();
            return;
        }

        // 检查合同是否有文件
        if (selected.getFilePath() == null || selected.getFilePath().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("信息");
            alert.setHeaderText("无合同文件");
            alert.setContentText("该合同暂无文件");
            alert.showAndWait();
            return;
        }

        // 检查文件是否存在
        File file = new File(selected.getFilePath());
        if (!file.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("文件不存在");
            alert.setContentText("合同文件不存在或已被删除");
            alert.showAndWait();
            return;
        }

        // 打开文件
        try {
            System.out.println("打开合同文件: " + selected.getFilePath());

            // 在单独的线程中打开文件，避免在FX应用线程中调用可能阻塞的操作
            new Thread(() -> {
                try {
                    boolean fileOpened = false;

                    // 尝试方法1: 使用java.awt.Desktop（有图形界面的环境）
                    if (!java.awt.GraphicsEnvironment.isHeadless()) {
                        try {
                            java.awt.Desktop.getDesktop().open(file);
                            System.out.println("方法1成功: 使用java.awt.Desktop打开文件");
                            fileOpened = true;
                        } catch (Exception e) {
                            System.out.println("方法1失败: " + e.getMessage());
                        }
                    }

                    // 尝试方法2: 使用命令行工具（跨平台）
                    if (!fileOpened) {
                        try {
                            String os = System.getProperty("os.name").toLowerCase();
                            ProcessBuilder pb;

                            if (os.contains("win")) {
                                // Windows
                                pb = new ProcessBuilder("cmd.exe", "/c", "start", "", file.getAbsolutePath());
                            } else if (os.contains("mac")) {
                                // macOS
                                pb = new ProcessBuilder("open", file.getAbsolutePath());
                            } else {
                                // Linux/Unix
                                pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
                            }

                            pb.start();
                            System.out.println("方法2成功: 使用命令行工具打开文件");
                            fileOpened = true;
                        } catch (Exception e) {
                            System.out.println("方法2失败: " + e.getMessage());
                        }
                    }

                    // 如果所有方法都失败，显示文件路径
                    if (!fileOpened) {
                        System.out.println("所有方法都失败，显示文件路径");
                        // 在FX应用线程中显示信息
                        javafx.application.Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("信息");
                            alert.setHeaderText("文件路径");
                            alert.setContentText("文件路径: " + selected.getFilePath() + "\n请手动打开此文件");
                            alert.showAndWait();
                        });
                    } else {
                        System.out.println("文件打开成功");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("打开文件失败: " + e.getMessage());
                    // 在FX应用线程中显示错误信息
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("错误");
                        alert.setHeaderText("打开文件失败");
                        alert.setContentText(
                                "错误信息: " + e.getMessage() + "\n文件路径: " + selected.getFilePath() + "\n请手动打开此文件");
                        alert.showAndWait();
                    });
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("打开文件失败: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("打开文件失败");
            alert.setContentText("错误信息: " + e.getMessage() + "\n文件路径: " + selected.getFilePath() + "\n请手动打开此文件");
            alert.showAndWait();
        }
    }

    /**
     * 保存合同
     */
    @FXML
    private void saveContract() {
        try {
            System.out.println("保存合同");

            // 验证必填字段
            if (contractNameField.getText().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("缺少必填字段");
                alert.setContentText("请输入合同名称");
                alert.showAndWait();
                return;
            }

            if (projectComboBox.getValue() == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("缺少必填字段");
                alert.setContentText("请选择所属项目");
                alert.showAndWait();
                return;
            }

            if (editingContract != null) {
                // 更新现有合同
                updateContract();
            } else {
                // 创建新合同
                createContract();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("保存合同失败: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("保存合同失败");
            alert.setContentText("错误信息: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * 创建新合同
     */
    private void createContract() {
        // 创建新合同对象
        SubcontractorContract contract = new SubcontractorContract();
        contract.setSubcontractorId(subcontractorId);
        contract.setContractName(contractNameField.getText());
        contract.setContractNumber(contractNumberField.getText());
        contract.setProjectId(projectComboBox.getValue().getId());
        contract.setProject(projectComboBox.getValue());

        // 设置合同金额
        if (!contractAmountField.getText().isEmpty()) {
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(contractAmountField.getText());
                contract.setContractAmount(amount);
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("金额格式错误");
                alert.setContentText("请输入正确的金额格式");
                alert.showAndWait();
                return;
            }
        }

        // 设置已支付金额
        if (!paidAmountField.getText().isEmpty()) {
            try {
                java.math.BigDecimal paidAmount = new java.math.BigDecimal(paidAmountField.getText());
                contract.setPaidAmount(paidAmount);
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("金额格式错误");
                alert.setContentText("请输入正确的已支付金额格式");
                alert.showAndWait();
                return;
            }
        }

        // 设置日期
        if (startDatePicker.getValue() != null) {
            contract.setStartDate(
                    Date.from(startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (endDatePicker.getValue() != null) {
            contract.setEndDate(Date.from(endDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        // 设置其他字段
        contract.setStatus(statusComboBox.getValue());
        contract.setDescription(descriptionField.getText());

        // 设置文件路径和文件名（如果有）
        if (editingContract != null && editingContract.getFilePath() != null) {
            contract.setFilePath(editingContract.getFilePath());
            contract.setFileName(editingContract.getFileName());
            System.out.println("设置新合同文件路径: " + editingContract.getFilePath());
        } else if (tempFilePath != null) {
            // 如果是新合同，使用临时存储的文件路径和文件名
            contract.setFilePath(tempFilePath);
            contract.setFileName(tempFileName);
            System.out.println("设置新合同临时文件路径: " + tempFilePath);
            // 清空临时变量
            tempFilePath = null;
            tempFileName = null;
        }

        // 保存合同
        try {
            SubcontractorContract result = apiClient.post("/api/subcontractors/contracts", contract, SubcontractorContract.class);
            if (result != null) {
                System.out.println("合同创建成功");
                // 刷新列表
                loadContracts();
                // 清空表单
                clearForm();
                // 设置表单为不可编辑
                setFormEditable(false);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("成功");
                alert.setHeaderText("合同创建成功");
                alert.setContentText("合同已成功创建");
                alert.showAndWait();
            } else {
                System.out.println("合同创建失败");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText("合同创建失败");
                alert.setContentText("请稍后重试");
                alert.showAndWait();
            }
        } catch (IOException ex) {
            System.out.println("合同创建失败: " + ex.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("合同创建失败");
            alert.setContentText("请稍后重试");
            alert.showAndWait();
        }
    }

    /**
     * 更新现有合同
     */
    private void updateContract() {
        String oldStatus = editingContract.getStatus();
        String newStatus = statusComboBox.getValue();

        if (!"已终止".equals(oldStatus) && "已终止".equals(newStatus)) {
            try {
                Boolean hasProof = apiClient.get("/api/subcontractors/settlement-proofs/has/" + editingContract.getId(), Boolean.class);
                if (hasProof == null || !hasProof) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("结清证明缺失");
                    confirmAlert.setHeaderText("该合同尚未导入结清证明");
                    confirmAlert.setContentText("将合同状态更改为\"已终止\"前，必须先导入结清证明合同。\n\n是否现在导入结清证明？");

                    Optional<ButtonType> result = confirmAlert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        showImportSettlementProofDialog(editingContract);
                    }
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("检查结清证明失败: " + e.getMessage());
            }
        }

        editingContract.setContractName(contractNameField.getText());
        editingContract.setContractNumber(contractNumberField.getText());
        editingContract.setProjectId(projectComboBox.getValue().getId());
        editingContract.setProject(projectComboBox.getValue());

        if (!contractAmountField.getText().isEmpty()) {
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(contractAmountField.getText());
                editingContract.setContractAmount(amount);
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("金额格式错误");
                alert.setContentText("请输入正确的金额格式");
                alert.showAndWait();
                return;
            }
        }

        if (!paidAmountField.getText().isEmpty()) {
            try {
                java.math.BigDecimal paidAmount = new java.math.BigDecimal(paidAmountField.getText());
                editingContract.setPaidAmount(paidAmount);
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("金额格式错误");
                alert.setContentText("请输入正确的已支付金额格式");
                alert.showAndWait();
                return;
            }
        }

        if (startDatePicker.getValue() != null) {
            editingContract.setStartDate(
                    Date.from(startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (endDatePicker.getValue() != null) {
            editingContract
                    .setEndDate(Date.from(endDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        editingContract.setStatus(newStatus);
        editingContract.setDescription(descriptionField.getText());

        try {
            SubcontractorContract result = apiClient.put("/api/subcontractors/contracts/" + editingContract.getId(), editingContract, SubcontractorContract.class);
            if (result != null) {
                System.out.println("合同更新成功");
                loadContracts();
                clearForm();
                editingContract = null;
                setFormEditable(false);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("成功");
                alert.setHeaderText("合同更新成功");
                alert.setContentText("合同已成功更新");
                alert.showAndWait();
            } else {
                System.out.println("合同更新失败");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText("合同更新失败");
                alert.setContentText("请稍后重试");
                alert.showAndWait();
            }
        } catch (IOException ex) {
            System.out.println("合同更新失败: " + ex.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("合同更新失败");
            alert.setContentText("请稍后重试");
            alert.showAndWait();
        }
    }

    /**
     * 取消操作
     */
    @FXML
    private void cancel() {
        System.out.println("取消操作");
        // 清空表单
        clearForm();
        editingContract = null;
        // 设置表单为不可编辑
        setFormEditable(false);
    }

    /**
     * 清空表单
     */
    private void clearForm() {
        contractNameField.setText("");
        contractNumberField.setText("");
        projectComboBox.setValue(null);
        contractAmountField.setText("");
        paidAmountField.setText("");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        statusComboBox.setValue(null);
        descriptionField.setText("");
        filePathField.setText("");
        // 清空临时文件变量
        tempFilePath = null;
        tempFileName = null;
        System.out.println("清空表单和临时文件变量");
    }

    /**
     * 填充表单
     */
    private void fillForm(SubcontractorContract contract) {
        contractNameField.setText(contract.getContractName());
        contractNumberField.setText(contract.getContractNumber());
        if (contract.getProject() != null) {
            projectComboBox.setValue(contract.getProject());
        }
        if (contract.getContractAmount() != null) {
            contractAmountField.setText(contract.getContractAmount().toString());
        }
        if (contract.getPaidAmount() != null) {
            paidAmountField.setText(contract.getPaidAmount().toString());
        }
        if (contract.getStartDate() != null) {
            startDatePicker.setValue(contract.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        if (contract.getEndDate() != null) {
            endDatePicker.setValue(contract.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        statusComboBox.setValue(contract.getStatus());
        descriptionField.setText(contract.getDescription());
        if (contract.getFileName() != null) {
            filePathField.setText(contract.getFileName());
        }
    }

    /**
     * 打开合同管理窗口
     */
    public static void openContractManagementWindow(Integer subcontractorId) {
        try {
            System.out.println("打开合同管理窗口，分包商ID: " + subcontractorId);

            FXMLLoader loader = new FXMLLoader(
                    FXSubcontractorContractController.class.getResource("/fxml/subcontractor/contract_manage.fxml"));
            
            // 使用自定义的控制器工厂，手动注入依赖
            final RemoteApiClient apiClient = RemoteApiClient.getInstance();
            final ClientSession clientSession = ClientSession.getInstance();
            loader.setControllerFactory(controllerClass -> {
                try {
                    Object controller = controllerClass.getDeclaredConstructor().newInstance();
                    
                    // 如果控制器有setApiClient方法，则注入RemoteApiClient
                    try {
                        java.lang.reflect.Method setApiClient = controllerClass.getMethod("setApiClient", RemoteApiClient.class);
                        setApiClient.invoke(controller, apiClient);
                    } catch (NoSuchMethodException e) {
                        // 控制器不需要ApiClient，忽略
                    }
                    
                    // 如果控制器有setClientSession方法，则注入ClientSession
                    try {
                        java.lang.reflect.Method setClientSession = controllerClass.getMethod("setClientSession", ClientSession.class);
                        setClientSession.invoke(controller, clientSession);
                    } catch (NoSuchMethodException e) {
                        // 控制器不需要ClientSession，忽略
                    }
                    
                    return controller;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create controller: " + controllerClass.getName(), e);
                }
            });
            
            Parent root = loader.load();

            FXSubcontractorContractController controller = loader.getController();
            controller.setSubcontractorId(subcontractorId);

            Stage stage = new Stage();
            stage.setTitle("分包商合同管理");
            stage.initModality(Modality.WINDOW_MODAL);
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("打开合同管理窗口失败: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("打开合同管理窗口失败");
            alert.setContentText("错误信息: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void showImportSettlementProofDialog(SubcontractorContract contract) {
        Dialog<SettlementProof> dialog = new Dialog<>();
        dialog.setTitle("导入结清证明合同");
        dialog.setHeaderText("为合同【" + contract.getContractName() + "】导入结清证明");

        ButtonType importButtonType = new ButtonType("导入", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(importButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField proofNameField = new TextField();
        proofNameField.setPromptText("结清证明名称");
        TextField proofNumberField = new TextField();
        proofNumberField.setPromptText("证明编号（可选）");
        DatePicker proofDatePicker = new DatePicker(LocalDate.now());
        TextField settlementAmountField = new TextField();
        settlementAmountField.setPromptText("结清金额（可选）");
        TextField fileNameField = new TextField();
        fileNameField.setEditable(false);
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("备注说明（可选）");
        descriptionArea.setPrefRowCount(3);

        Button selectFileBtn = new Button("选择文件");
        final File[] selectedFile = { null };
        final String[] selectedFileName = { null };

        selectFileBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择结清证明文件");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PDF文件", "*.pdf"),
                    new FileChooser.ExtensionFilter("Word文档", "*.doc", "*.docx"),
                    new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("所有文件", "*.*"));

            File file = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedFile[0] = file;
                selectedFileName[0] = file.getName();
                fileNameField.setText(file.getName());
            }
        });

        grid.add(new Label("证明名称:"), 0, 0);
        grid.add(proofNameField, 1, 0);
        grid.add(new Label("证明编号:"), 0, 1);
        grid.add(proofNumberField, 1, 1);
        grid.add(new Label("证明日期:"), 0, 2);
        grid.add(proofDatePicker, 1, 2);
        grid.add(new Label("结清金额:"), 0, 3);
        grid.add(settlementAmountField, 1, 3);
        grid.add(new Label("证明文件:"), 0, 4);
        HBox fileBox = new HBox(10, fileNameField, selectFileBtn);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(fileBox, 1, 4);
        grid.add(new Label("备注说明:"), 0, 5);
        grid.add(descriptionArea, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == importButtonType) {
                if (proofNameField.getText().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "警告", "请输入证明名称");
                    return null;
                }
                if (selectedFile[0] == null) {
                    showAlert(Alert.AlertType.WARNING, "警告", "请选择结清证明文件");
                    return null;
                }

                SettlementProof proof = new SettlementProof();
                proof.setContractId(contract.getId());
                proof.setSubcontractorId(contract.getSubcontractorId());
                proof.setProofName(proofNameField.getText());
                proof.setProofNumber(proofNumberField.getText());
                if (proofDatePicker.getValue() != null) {
                    proof.setProofDate(
                            Date.from(proofDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }
                if (!settlementAmountField.getText().isEmpty()) {
                    try {
                        proof.setSettlementAmount(new java.math.BigDecimal(settlementAmountField.getText()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                proof.setFileName(selectedFileName[0]);
                proof.setDescription(descriptionArea.getText());
                return proof;
            }
            return null;
        });

        Optional<SettlementProof> result = dialog.showAndWait();
        result.ifPresent(proof -> {
            Integer userId = clientSession != null && clientSession.getCurrentUser() != null
                    ? clientSession.getCurrentUser().getUserId()
                    : null;
            proof.setCreatedBy(userId);

            try {
                // Upload file using multipart/form-data
                Map<String, String> additionalParams = new HashMap<>();
                additionalParams.put("contractId", String.valueOf(proof.getContractId()));
                additionalParams.put("subcontractorId", String.valueOf(proof.getSubcontractorId()));
                additionalParams.put("proofName", proof.getProofName());
                if (proof.getProofNumber() != null) {
                    additionalParams.put("proofNumber", proof.getProofNumber());
                }
                if (proof.getProofDate() != null) {
                    additionalParams.put("proofDate", String.valueOf(proof.getProofDate().getTime()));
                }
                if (proof.getSettlementAmount() != null) {
                    additionalParams.put("settlementAmount", proof.getSettlementAmount().toString());
                }
                if (proof.getDescription() != null) {
                    additionalParams.put("description", proof.getDescription());
                }
                if (userId != null) {
                    additionalParams.put("createdBy", String.valueOf(userId));
                }

                SettlementProof resultProof = apiClient.uploadFile(
                        "/api/subcontractors/settlement-proofs",
                        selectedFile[0],
                        "file",
                        additionalParams,
                        SettlementProof.class
                );

                if (resultProof != null) {
                    contract.setStatus("已终止");
                    apiClient.put("/api/subcontractors/contracts/" + contract.getId(), contract, SubcontractorContract.class);
                    loadContracts();
                    showAlert(Alert.AlertType.INFORMATION, "成功", "结清证明导入成功！\n合同状态已自动更改为\"已终止\"。");
                } else {
                    showAlert(Alert.AlertType.ERROR, "错误", "结清证明导入失败，请重试。");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "错误", "结清证明导入失败: " + ex.getMessage());
            }
        });
    }

    @FXML
    private void importSettlementProof() {
        SubcontractorContract selected = contractTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "警告", "请先选择一个合同");
            return;
        }

        try {
            Boolean hasProof = apiClient.get("/api/subcontractors/settlement-proofs/has/" + selected.getId(), Boolean.class);
            if (hasProof != null && hasProof) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("结清证明已存在");
                confirmAlert.setHeaderText("该合同已有结清证明");
                confirmAlert.setContentText("是否要重新导入并覆盖现有结清证明？");
                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    showImportSettlementProofDialog(selected);
                }
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("检查结清证明失败: " + e.getMessage());
        }

        showImportSettlementProofDialog(selected);
    }

    @FXML
    private void viewSettlementProof() {
        SubcontractorContract selected = contractTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "警告", "请先选择一个合同");
            return;
        }

        if (!"已终止".equals(selected.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "只有已终止状态的合同才能查看结清证明");
            return;
        }

        try {
            SettlementProof proof = apiClient.get("/api/subcontractors/settlement-proofs/contract/" + selected.getId(), SettlementProof.class);
            if (proof == null) {
                showAlert(Alert.AlertType.WARNING, "提示", "该合同暂无结清证明");
                return;
            }

            showSettlementProofDetailDialog(proof, selected);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "获取结清证明失败: " + e.getMessage());
        }
    }

    private void showSettlementProofDetailDialog(SettlementProof proof, SubcontractorContract contract) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("查看结清证明合同");
        dialog.setHeaderText("合同【" + contract.getContractName() + "】的结清证明");

        ButtonType closeType = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType openFileType = new ButtonType("打开文件", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(openFileType, closeType);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(10);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        int row = 0;
        infoGrid.add(createBoldLabel("证明名称:"), 0, row);
        infoGrid.add(new Label(proof.getProofName()), 1, row++);

        infoGrid.add(createBoldLabel("证明编号:"), 0, row);
        infoGrid.add(new Label(proof.getProofNumber() != null ? proof.getProofNumber() : "-"), 1, row++);

        infoGrid.add(createBoldLabel("证明日期:"), 0, row);
        String dateStr = proof.getProofDate() != null
                ? proof.getProofDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
                : "-";
        infoGrid.add(new Label(dateStr), 1, row++);

        infoGrid.add(createBoldLabel("结清金额:"), 0, row);
        String amountStr = proof.getSettlementAmount() != null ? String.format("¥%.2f", proof.getSettlementAmount())
                : "-";
        infoGrid.add(new Label(amountStr), 1, row++);

        infoGrid.add(createBoldLabel("证明文件:"), 0, row);
        infoGrid.add(new Label(proof.getFileName() != null ? proof.getFileName() : "-"), 1, row++);

        infoGrid.add(createBoldLabel("创建时间:"), 0, row);
        String createStr = proof.getCreateTime() != null
                ? proof.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString()
                : "-";
        infoGrid.add(new Label(createStr), 1, row++);

        if (proof.getDescription() != null && !proof.getDescription().isEmpty()) {
            infoGrid.add(createBoldLabel("备注说明:"), 0, row);
            Label descLabel = new Label(proof.getDescription());
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(400);
            infoGrid.add(descLabel, 1, row);
        }

        content.getChildren().add(infoGrid);

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == openFileType && proof.getFilePath() != null) {
                openFile(proof.getFilePath());
            }
            return null;
        });

        dialog.showAndWait();
    }

    private Label createBoldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private void openFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            showAlert(Alert.AlertType.ERROR, "错误", "文件不存在或已被删除");
            return;
        }

        new Thread(() -> {
            try {
                if (!java.awt.GraphicsEnvironment.isHeadless()) {
                    java.awt.Desktop.getDesktop().open(file);
                } else {
                    String os = System.getProperty("os.name").toLowerCase();
                    ProcessBuilder pb;
                    if (os.contains("win")) {
                        pb = new ProcessBuilder("cmd.exe", "/c", "start", "", file.getAbsolutePath());
                    } else if (os.contains("mac")) {
                        pb = new ProcessBuilder("open", file.getAbsolutePath());
                    } else {
                        pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
                    }
                    pb.start();
                }
            } catch (Exception e) {
                javafx.application.Platform
                        .runLater(() -> showAlert(Alert.AlertType.ERROR, "错误", "无法打开文件: " + e.getMessage()));
            }
        }).start();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 合同列表响应内部类（用于解析API响应）
     */
    private static class ContractListResponse {
        private List<SubcontractorContract> content;
        private int totalPages;
        private int totalElements;

        public List<SubcontractorContract> getContent() {
            return content;
        }

        public void setContent(List<SubcontractorContract> content) {
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
    }

    /**
     * 项目列表响应内部类（用于解析API响应）
     */
    private static class ProjectListResponse {
        private List<Project> content;
        private int totalPages;
        private int totalElements;

        public List<Project> getContent() {
            return content;
        }

        public void setContent(List<Project> content) {
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
    }
}