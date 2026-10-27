package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MainController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Label projectCountLabel;

    @FXML
    private Label accountCountLabel;

    @FXML
    private Label receiptCountLabel;

    @FXML
    private Label materialCountLabel;

    @FXML
    public void initialize() {
        // 初始化数据统计
        loadStatistics();
    }

    @SuppressWarnings("unchecked")
    private void loadStatistics() {
        try {
            // 尝试从仪表板API获取统计数据
            Map<String, Object> stats = apiClient.get("/api/dashboard/statistics", Map.class);
            if (stats != null) {
                projectCountLabel.setText(String.valueOf(stats.getOrDefault("projectCount", 0)));
                accountCountLabel.setText(String.valueOf(stats.getOrDefault("accountCount", 0)));
                receiptCountLabel.setText(String.valueOf(stats.getOrDefault("receiptCount", 0)));
                materialCountLabel.setText(String.valueOf(stats.getOrDefault("materialCount", 0)));
            } else {
                setDefaultCounts();
            }
        } catch (Exception e) {
            System.out.println("获取统计数据失败: " + e.getMessage());
            // 尝试分别获取各模块数量
            try {
                loadCountsIndividually();
            } catch (Exception ex) {
                setDefaultCounts();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadCountsIndividually() throws Exception {
        try {
            Object[] projects = apiClient.get("/api/projects", Object[].class);
            projectCountLabel.setText(String.valueOf(projects != null ? projects.length : 0));
        } catch (Exception e) {
            projectCountLabel.setText("0");
        }
        try {
            Map<String, Object> accountsResult = apiClient.get("/api/accounts", Map.class);
            accountCountLabel.setText(String.valueOf(accountsResult.getOrDefault("total", 0)));
        } catch (Exception e) {
            accountCountLabel.setText("0");
        }
        try {
            Map<String, Object> receiptsResult = apiClient.get("/api/receipts", Map.class);
            receiptCountLabel.setText(String.valueOf(receiptsResult.getOrDefault("total", 0)));
        } catch (Exception e) {
            receiptCountLabel.setText("0");
        }
        try {
            Object[] materials = apiClient.get("/api/materials", Object[].class);
            materialCountLabel.setText(String.valueOf(materials != null ? materials.length : 0));
        } catch (Exception e) {
            materialCountLabel.setText("0");
        }
    }

    private void setDefaultCounts() {
        projectCountLabel.setText("0");
        accountCountLabel.setText("0");
        receiptCountLabel.setText("0");
        materialCountLabel.setText("0");
    }

    @FXML
    private void newAccount() {
        openNewWindow("fxml/account/add.fxml", "新建账目");
    }

    @FXML
    private void saveAccount() {
        System.out.println("保存账目");
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        stage.close();
        loadStatistics();
    }

    @FXML
    private void cancelEdit() {
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void uploadReceipt() {
        try {
            String fxmlPath = "/fxml/receipt/upload.fxml";
            System.out.println("尝试加载FXML文件: " + fxmlPath);
            System.out.println("FXML文件是否存在: " + (getClass().getResource(fxmlPath) != null));

            if (getClass().getResource(fxmlPath) == null) {
                System.out.println("FXML文件不存在: " + fxmlPath);
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("上传票据");
                Label label = new Label("功能开发中，敬请期待！");
                label.setStyle("-fx-font-size: 14px; -fx-padding: 20px;");
                stage.setScene(new Scene(label, 300, 100));
                WindowUtils.setStageIcon(stage);
                stage.showAndWait();
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                System.out.println("正在加载FXML文件...");
                Parent root = loader.load();
                injectDependencies(loader.getController());
                System.out.println("FXML文件加载成功");
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("上传票据");
                stage.setScene(new Scene(root));
                stage.sizeToScene();
                stage.showAndWait();

                loadStatistics();

                // 刷新所有已打开的票据列表标签页
                for (Tab tab : mainTabPane.getTabs()) {
                    if (tab.getText().equals("票据列表") || tab.getText().equals("票据验证")) {
                        Node content = tab.getContent();
                        if (content != null) {
                            Scene scene = content.getScene();
                            if (scene != null) {
                                FXMLLoader tabLoader = (FXMLLoader) scene.getUserData();
                                if (tabLoader != null) {
                                    Object controller = tabLoader.getController();
                                    if (controller instanceof FXReceiptController) {
                                        ((FXReceiptController) controller).loadReceipts();
                                        System.out.println("刷新了票据列表");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("打开窗口失败: " + e.getMessage());
            e.printStackTrace();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("上传票据");
            Label label = new Label("加载失败: " + e.getMessage());
            label.setStyle("-fx-font-size: 14px; -fx-padding: 20px; -fx-text-fill: red;");
            stage.setScene(new Scene(label, 400, 150));
            stage.showAndWait();
        }
    }

    @FXML
    private void selectFile() {
        System.out.println("选择文件");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择票据文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.jpg", "*.jpeg", "*.png", "*.pdf"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            System.out.println("选择了文件: " + file);
        }
    }

    @FXML
    private void uploadReceiptAction() {
        System.out.println("上传票据");
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        stage.close();
        loadStatistics();
    }

    @FXML
    private void createBackup() {
        // 权限检查：仅经理和管理员可执行备份
        if (!hasBackupPermission()) {
            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
            warningAlert.setTitle("权限不足");
            warningAlert.setHeaderText(null);
            warningAlert.setContentText("抱歉，您没有权限执行备份操作。\n\n备份和恢复功能仅对经理和管理员开放。\n\n如需此权限，请联系系统管理员。");
            warningAlert.showAndWait();
            return;
        }

        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("备份中");
            alert.setHeaderText(null);
            alert.setContentText("正在执行备份操作，请稍候...");
            alert.show();

            Map<String, Object> result = apiClient.post("/api/backup/create", null, Map.class);
            String backupPath = result != null ? (String) result.get("path") : "未知路径";

            alert.close();

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("成功");
            successAlert.setHeaderText(null);
            successAlert.setContentText("备份成功！备份文件保存到：\n" + backupPath);
            successAlert.showAndWait();
        } catch (Exception ex) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("错误");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("备份失败：" + ex.getMessage());
            errorAlert.showAndWait();
            ex.printStackTrace();
        }
    }

    @FXML
    private void restoreBackup() {
        if (!hasBackupPermission()) {
            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
            warningAlert.setTitle("权限不足");
            warningAlert.setHeaderText(null);
            warningAlert.setContentText("抱歉，您没有权限执行数据库恢复操作。\n\n备份和恢复功能仅对经理和管理员开放。\n\n如需此权限，请联系系统管理员。");
            warningAlert.showAndWait();
            return;
        }

        try {
            File backupFile = chooseBackupFile();
            if (backupFile == null) {
                return;
            }

            if (!confirmRestore()) {
                return;
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("恢复中");
            alert.setHeaderText(null);
            alert.setContentText("正在执行恢复操作，请稍候...");
            alert.show();

            Map<String, Object> params = new HashMap<>();
            params.put("backupPath", backupFile.getAbsolutePath());
            apiClient.post("/api/backup/restore", params, Void.class);

            alert.close();

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("成功");
            successAlert.setHeaderText(null);
            successAlert.setContentText("恢复成功！建议重启应用程序以确保数据生效。");
            successAlert.showAndWait();
        } catch (Exception ex) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("错误");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("恢复失败：" + ex.getMessage());
            errorAlert.showAndWait();
            ex.printStackTrace();
        }
    }

    private boolean hasBackupPermission() {
        if (clientSession == null || clientSession.getCurrentUser() == null) {
            return false;
        }
        User user = clientSession.getCurrentUser();
        String role = user.getRole();
        return "MANAGER".equals(role) || "ADMIN".equals(role);
    }

    private File chooseBackupFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择备份文件");

        File backupDir = new File("backup/");
        if (backupDir.exists() && backupDir.isDirectory()) {
            fileChooser.setInitialDirectory(backupDir);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("备份文件", "*.sql", "*.zip"),
                new FileChooser.ExtensionFilter("SQL文件", "*.sql"),
                new FileChooser.ExtensionFilter("ZIP文件", "*.zip"));

        return fileChooser.showOpenDialog(mainTabPane.getScene().getWindow());
    }

    private boolean confirmRestore() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("恢复确认");
        alert.setHeaderText("警告：恢复操作将覆盖现有数据！");
        alert.setContentText("确定要继续恢复备份吗？此操作不可撤销，将覆盖当前数据库中的所有数据。");

        ButtonType confirmButton = new ButtonType("确定恢复");
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        return alert.showAndWait().filter(response -> response == confirmButton).isPresent();
    }

    @FXML
    private void exit() {
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void projectList() {
        openTab("项目列表", "fxml/project/list.fxml");
    }

    @FXML
    private void addProject() {
        openNewWindow("fxml/project/add.fxml", "添加项目");
    }

    @FXML
    private void projectCostAnalysis() {
        openTab("项目成本分析", "fxml/project/cost_analysis.fxml");
    }

    @FXML
    private void accountList() {
        openTab("账目列表", "fxml/account/list.fxml");
    }

    @FXML
    private void addAccount() {
        openNewWindow("fxml/account/add.fxml", "添加账目");
    }

    @FXML
    private void financialReport() {
        openTab("财务报表", "fxml/account/report.fxml");
    }

    @FXML
    private void receiptList() {
        openTab("票据列表", "fxml/receipt/list.fxml");
    }

    @FXML
    private void verifyReceipt() {
        openTab("票据验证", "fxml/receipt/verify.fxml");
    }

    @FXML
    private void approvalFlow() {
        openTab("审核流程", "fxml/receipt/approval_flow.fxml");
    }

    @FXML
    private void receiptVerification() {
        openTab("票据列表", "fxml/receipt/list.fxml");
    }

    @FXML
    private void materialList() {
        openTab("材料列表", "fxml/material/list.fxml");
    }

    @FXML
    private void addMaterial() {
        openNewWindow("fxml/material/add.fxml", "添加材料");
    }

    @FXML
    private void materialReport() {
        openTab("材料报表", "fxml/material/report.fxml");
    }

    @FXML
    private void subcontractorList() {
        openTab("分包商列表", "fxml/subcontractor/list.fxml");
    }

    @FXML
    private void addSubcontractor() {
        openNewWindow("fxml/subcontractor/add.fxml", "添加分包商");
    }

    @FXML
    private void projectCostReport() {
        openTab("项目成本报表", "fxml/project/cost_analysis.fxml");
    }

    @FXML
    private void systemSettings() {
        openNewWindow("fxml/system-settings.fxml", "系统设置");
    }

    @FXML
    private void userManagement() {
        openTab("用户管理", "fxml/user/list.fxml");
    }

    @FXML
    private void about() {
        try {
            System.out.println("打开关于系统窗口");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/about.fxml"));
            Parent root = loader.load();
            injectDependencies(loader.getController());
            Stage stage = new Stage();
            stage.setTitle("关于系统");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("打开关于系统窗口时发生错误: " + e.getMessage());
        }
    }

    @FXML
    private void paymentList() {
        openTab("账户管理", "fxml/payment/list.fxml");
    }

    @FXML
    private void transactionQuery() {
        openNewWindow("fxml/payment/transaction.fxml", "收支查询");
    }

    @FXML
    private void electronicSealApplication() {
        openNewWindow("fxml/electronic-seal/application.fxml", "电子章使用申请");
    }

    @FXML
    private void electronicSealApproval() {
        openNewWindow("fxml/electronic-seal/approval.fxml", "电子章申请审批");
    }

    @FXML
    private void electronicSealStamp() {
        openNewWindow("fxml/electronic-seal/stamp.fxml", "电子盖章");
    }

    @FXML
    private void electronicSealImport() {
        openNewWindow("fxml/electronic-seal/import.fxml", "电子章导入");
    }

    @FXML
    private void electronicSealManagement() {
        openNewWindow("fxml/electronic-seal/stamp.fxml", "电子章盖章");
    }

    @FXML
    private void stampApprovalManagement() {
        openNewWindow("fxml/electronic-seal/stamp-approval.fxml", "盖章审批管理");
    }

    @FXML
    private void myStampApproval() {
        openNewWindow("fxml/electronic-seal/my-stamp-approval.fxml", "我的盖章审批");
    }

    private void openTab(String title, String fxmlPath) {
        // 检查标签是否已存在
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getText().equals(title)) {
                mainTabPane.getSelectionModel().select(tab);

                Node content = tab.getContent();
                if (content != null) {
                    try {
                        String fullPath = "/" + fxmlPath;
                        FXMLLoader loader = new FXMLLoader(getClass().getResource(fullPath));
                        Parent newContent = loader.load();
                        injectDependencies(loader.getController());
                        tab.setContent(newContent);
                        System.out.println("刷新了" + title + "标签页数据");
                    } catch (Exception e) {
                        System.out.println("刷新" + title + "标签页失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                return;
            }
        }

        // 创建新标签
        Tab tab = new Tab(title);
        try {
            String fullPath = "/" + fxmlPath;
            System.out.println("尝试加载FXML文件: " + fullPath);
            System.out.println("FXML文件是否存在: " + (getClass().getResource(fullPath) != null));

            if (getClass().getResource(fullPath) == null) {
                System.out.println("FXML文件不存在: " + fullPath);
                Label label = new Label("功能开发中，敬请期待！");
                label.setStyle("-fx-font-size: 14px; -fx-padding: 20px;");
                tab.setContent(label);
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fullPath));
                System.out.println("正在加载FXML文件...");
                Parent content = loader.load();
                injectDependencies(loader.getController());
                System.out.println("FXML文件加载成功");
                tab.setContent(content);
            }
            mainTabPane.getTabs().add(tab);
            mainTabPane.getSelectionModel().select(tab);
        } catch (Exception e) {
            System.out.println("打开标签失败: " + e.getMessage());
            e.printStackTrace();
            Label label = new Label("加载失败: " + e.getMessage());
            label.setStyle("-fx-font-size: 14px; -fx-padding: 20px; -fx-text-fill: red;");
            tab.setContent(label);
            mainTabPane.getTabs().add(tab);
            mainTabPane.getSelectionModel().select(tab);
        }
    }

    private void openNewWindow(String fxmlPath, String title) {
        try {
            String fullPath = "/" + fxmlPath;
            System.out.println("尝试加载FXML文件: " + fullPath);
            System.out.println("FXML文件是否存在: " + (getClass().getResource(fullPath) != null));

            if (getClass().getResource(fullPath) == null) {
                System.out.println("FXML文件不存在: " + fullPath);
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle(title);
                Label label = new Label("功能开发中，敬请期待！");
                label.setStyle("-fx-font-size: 14px; -fx-padding: 20px;");
                stage.setScene(new Scene(label, 300, 100));
                WindowUtils.setStageIcon(stage);
                stage.showAndWait();
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fullPath));
                System.out.println("正在加载FXML文件...");
                Parent root = loader.load();
                injectDependencies(loader.getController());
                System.out.println("FXML文件加载成功");
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle(title);
                stage.setScene(new Scene(root));
                stage.showAndWait();
                loadStatistics();
            }
        } catch (Exception e) {
            System.out.println("打开窗口失败: " + e.getMessage());
            e.printStackTrace();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            Label label = new Label("加载失败: " + e.getMessage());
            label.setStyle("-fx-font-size: 14px; -fx-padding: 20px; -fx-text-fill: red;");
            stage.setScene(new Scene(label, 400, 150));
            WindowUtils.setStageIcon(stage);
            stage.showAndWait();
        }
    }

    /**
     * 通过反射向控制器注入 apiClient 和 clientSession 依赖
     */
    private void injectDependencies(Object controller) {
        if (controller == null) {
            return;
        }
        try {
            Method setApiClient = controller.getClass().getMethod("setApiClient", RemoteApiClient.class);
            setApiClient.invoke(controller, apiClient);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            System.out.println("注入 apiClient 失败: " + e.getMessage());
        }
        try {
            Method setClientSession = controller.getClass().getMethod("setClientSession", ClientSession.class);
            setClientSession.invoke(controller, clientSession);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            System.out.println("注入 clientSession 失败: " + e.getMessage());
        }
    }
}