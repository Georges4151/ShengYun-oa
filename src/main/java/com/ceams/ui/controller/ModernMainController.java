package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Project;
import com.ceams.model.entity.Receipt;
import com.ceams.model.entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.Node;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ModernMainController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private Label projectCountLabel;

    @FXML
    private Label accountCountLabel;

    @FXML
    private Label receiptCountLabel;

    @FXML
    private Label materialCountLabel;

    @FXML
    private Label userCountLabel;

    @FXML
    private Label pendingReceiptsLabel;

    @FXML
    private Label monthlyProjectsLabel;

    @FXML
    private Label monthlyReceiptsLabel;

    @FXML
    private VBox recentProjectsContainer;

    @FXML
    private VBox pendingTasksContainer;

    @FXML
    private Label welcomeUserLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label userAvatarLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userEmailLabel;

    @FXML
    private Label currentDateLabel;

    @FXML
    private Label notificationBadgeLabel;

    @FXML
    private javafx.scene.control.TextField searchField;

    @FXML
    public void initialize() {
        loadUserInfo();
        updateCurrentDate();
        loadStatistics();
        loadDashboardData();
        loadNotificationBadge();
        initializeAnimations();
        startAutoUpdateDate();
    }

    /**
     * 更新当前日期显示
     */
    private void updateCurrentDate() {
        if (currentDateLabel != null) {
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                    .ofPattern("yyyy 年 M 月 d 日");
            currentDateLabel.setText(now.format(formatter));
        }
    }

    /**
     * 启动自动更新日期定时器（每分钟更新一次）
     */
    private void startAutoUpdateDate() {
        javafx.animation.AnimationTimer dateTimer = new javafx.animation.AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate > 60_000_000_000L) {
                    updateCurrentDate();
                    lastUpdate = now;
                }
            }
        };
        dateTimer.start();
    }

    /**
     * 从 ClientSession 中读取并显示用户信息
     */
    private void loadUserInfo() {
        try {
            User currentUser = clientSession != null ? clientSession.getCurrentUser() : null;
            if (currentUser != null) {
                if (welcomeUserLabel != null) {
                    welcomeUserLabel.setText("欢迎回来，"
                            + (currentUser.getName() != null ? currentUser.getName() : currentUser.getUsername()));
                }
                if (userRoleLabel != null) {
                    String roleName = getRoleName(currentUser.getRole());
                    userRoleLabel.setText(roleName);
                }
                if (userAvatarLabel != null) {
                    String displayName = currentUser.getName() != null ? currentUser.getName()
                            : currentUser.getUsername();
                    if (displayName != null && !displayName.isEmpty()) {
                        userAvatarLabel.setText(displayName.substring(0, 1));
                    } else {
                        userAvatarLabel.setText("用");
                    }
                }
                if (userNameLabel != null) {
                    userNameLabel.setText(currentUser.getName() != null ? currentUser.getName() : currentUser.getUsername());
                }
                if (userEmailLabel != null && currentUser.getEmail() != null) {
                    userEmailLabel.setText(currentUser.getEmail());
                }
                System.out.println("当前登录用户：" + currentUser.getUsername() + ", 角色：" + currentUser.getRole());
            } else {
                if (welcomeUserLabel != null) welcomeUserLabel.setText("欢迎回来");
                if (userRoleLabel != null) userRoleLabel.setText("");
                if (userAvatarLabel != null) userAvatarLabel.setText("用");
                if (userNameLabel != null) userNameLabel.setText("游客");
                if (userEmailLabel != null) userEmailLabel.setText("");
            }
        } catch (Exception e) {
            System.out.println("加载用户信息失败：" + e.getMessage());
            if (welcomeUserLabel != null) welcomeUserLabel.setText("欢迎回来");
            if (userRoleLabel != null) userRoleLabel.setText("");
            if (userAvatarLabel != null) userAvatarLabel.setText("用");
            if (userNameLabel != null) userNameLabel.setText("游客");
            if (userEmailLabel != null) userEmailLabel.setText("");
        }
    }

    private String getRoleName(String role) {
        if (role == null) return "用户";
        switch (role) {
            case "ADMIN": return "系统管理员";
            case "MANAGER": return "项目经理";
            case "FINANCE": return "财务人员";
            case "USER": return "普通用户";
            default: return role;
        }
    }

    private void initializeAnimations() {
        if (recentProjectsContainer != null) addFadeInAnimation(recentProjectsContainer);
        if (pendingTasksContainer != null) addFadeInAnimation(pendingTasksContainer);
    }

    private void addFadeInAnimation(Node node) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void addSlideInAnimation(Node node) {
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), node);
        slideIn.setFromX(-20);
        slideIn.setToX(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        slideIn.play();
    }

    private void addHoverAnimation(Node node) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), node);
        scaleUp.setToX(1.02);
        scaleUp.setToY(1.02);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), node);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        node.setOnMouseEntered(e -> scaleUp.play());
        node.setOnMouseExited(e -> scaleDown.play());
    }

    @SuppressWarnings("unchecked")
    private void loadStatistics() {
        try {
            // 通过远程API获取统计数据
            Map<String, Object> statistics = apiClient.get("/api/dashboard/statistics", Map.class);

            if (projectCountLabel != null) {
                Object projectCount = statistics.get("projectCount");
                projectCountLabel.setText(projectCount != null ? String.valueOf(projectCount) : "0");
            }
            if (accountCountLabel != null) {
                Object accountCount = statistics.get("accountCount");
                accountCountLabel.setText(accountCount != null ? String.valueOf(accountCount) : "0");
            }
            if (receiptCountLabel != null) {
                Object receiptCount = statistics.get("receiptCount");
                receiptCountLabel.setText(receiptCount != null ? String.valueOf(receiptCount) : "0");
            }
            if (materialCountLabel != null) {
                Object materialCount = statistics.get("materialCount");
                materialCountLabel.setText(materialCount != null ? String.valueOf(materialCount) : "0");
            }
        } catch (Exception e) {
            System.out.println("获取统计数据失败: " + e.getMessage());
            if (projectCountLabel != null) projectCountLabel.setText("0");
            if (accountCountLabel != null) accountCountLabel.setText("0");
            if (receiptCountLabel != null) receiptCountLabel.setText("0");
            if (materialCountLabel != null) materialCountLabel.setText("0");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDashboardData() {
        try {
            Map<String, Object> statistics = apiClient.get("/api/dashboard/statistics", Map.class);

            if (userCountLabel != null) {
                Object userCount = statistics.get("userCount");
                userCountLabel.setText(userCount != null ? String.valueOf(userCount) : "0");
            }
            if (pendingReceiptsLabel != null) {
                Object pendingReceipts = statistics.get("pendingReceipts");
                pendingReceiptsLabel.setText(pendingReceipts != null ? String.valueOf(pendingReceipts) : "0");
            }

            loadRecentProjects();
            loadPendingTasks();

        } catch (Exception e) {
            System.out.println("获取仪表盘数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadRecentProjects() {
        if (recentProjectsContainer == null) return;

        try {
            recentProjectsContainer.getChildren().clear();
            List<Map<String, Object>> recentProjects = apiClient.get("/api/dashboard/recent-projects?limit=5", List.class);

            if (recentProjects == null || recentProjects.isEmpty()) {
                Label emptyLabel = new Label("暂无项目数据");
                emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 12px; -fx-padding: 20 0;");
                recentProjectsContainer.getChildren().add(emptyLabel);
                return;
            }

            for (Map<String, Object> projectData : recentProjects) {
                HBox projectItem = createProjectItemFromMap(projectData);
                recentProjectsContainer.getChildren().add(projectItem);
            }

        } catch (Exception e) {
            System.out.println("加载最近项目失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private HBox createProjectItemFromMap(Map<String, Object> projectData) {
        HBox item = new HBox(12.0);
        item.getStyleClass().add("list-item");

        VBox avatar = new VBox();
        avatar.getStyleClass().addAll("avatar", "avatar-sm");
        Label avatarText = new Label("项");
        avatarText.getStyleClass().add("avatar-text");
        avatar.getChildren().add(avatarText);

        VBox content = new VBox(4.0);
        content.getStyleClass().add("flex-grow");
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);

        String projectName = projectData.get("projectName") != null ? projectData.get("projectName").toString() : "未命名项目";
        Label title = new Label(projectName);
        title.getStyleClass().add("list-item-title");

        Object budgetObj = projectData.get("budgetAmount");
        String budgetText = budgetObj != null
                ? String.format("预算：%.2f万元", ((Number) budgetObj).doubleValue() / 10000)
                : "预算：未设置";
        Label subtitle = new Label(budgetText);
        subtitle.getStyleClass().add("list-item-subtitle");

        content.getChildren().addAll(title, subtitle);

        String status = projectData.get("status") != null ? projectData.get("status").toString() : "";
        Label statusLabel = new Label(getProjectStatusText(status));
        statusLabel.getStyleClass().addAll("status-badge", getStatusBadgeStyle(status));

        item.getChildren().addAll(avatar, content, statusLabel);
        return item;
    }

    @SuppressWarnings("unchecked")
    private void loadPendingTasks() {
        if (pendingTasksContainer == null) return;

        try {
            pendingTasksContainer.getChildren().clear();

            Map<String, Object> statistics = apiClient.get("/api/dashboard/statistics", Map.class);
            Object financePendingObj = statistics.get("financePending");
            Object projectManagerPendingObj = statistics.get("projectManagerPending");
            Object managerPendingObj = statistics.get("managerPending");

            long financePending = financePendingObj != null ? ((Number) financePendingObj).longValue() : 0;
            long projectManagerPending = projectManagerPendingObj != null ? ((Number) projectManagerPendingObj).longValue() : 0;
            long managerPending = managerPendingObj != null ? ((Number) managerPendingObj).longValue() : 0;

            if (financePending > 0) {
                HBox task = createTaskItem("财务审核", financePending + "个票据待财务审核", "需要财务部门审核", "warning");
                pendingTasksContainer.getChildren().add(task);
            }
            if (projectManagerPending > 0) {
                HBox task = createTaskItem("项目经理审核", projectManagerPending + "个票据待项目经理审核", "需要项目经理审核", "info");
                pendingTasksContainer.getChildren().add(task);
            }
            if (managerPending > 0) {
                HBox task = createTaskItem("经理审核", managerPending + "个票据待经理审核", "需要经理审核", "error");
                pendingTasksContainer.getChildren().add(task);
            }

            if (pendingTasksContainer.getChildren().isEmpty()) {
                Label emptyLabel = new Label("暂无待处理事项");
                emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 12px; -fx-padding: 20 0;");
                pendingTasksContainer.getChildren().add(emptyLabel);
            }

        } catch (Exception e) {
            System.out.println("加载待处理事项失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private HBox createTaskItem(String iconText, String title, String subtitle, String type) {
        HBox item = new HBox(12.0);
        item.getStyleClass().add("list-item");

        VBox icon = new VBox();
        icon.getStyleClass().addAll("icon-container", "icon-container-" + type);
        Label iconLabel = new Label(iconText.substring(0, 1));
        iconLabel.getStyleClass().add("icon-text");
        icon.getChildren().add(iconLabel);

        VBox content = new VBox(4.0);
        content.getStyleClass().add("flex-grow");
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("list-item-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("list-item-subtitle");

        content.getChildren().addAll(titleLabel, subtitleLabel);

        Button actionButton = new Button("处理");
        actionButton.getStyleClass().addAll("button", "button-sm", "button-primary");
        actionButton.setOnAction(e -> approvalFlow());

        item.getChildren().addAll(icon, content, actionButton);
        return item;
    }

    private String getProjectStatusText(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "active": return "进行中";
            case "completed": return "已完成";
            case "paused": return "已暂停";
            case "cancelled": return "已取消";
            default: return status;
        }
    }

    private String getStatusBadgeStyle(String status) {
        if (status == null) return "status-badge-secondary";
        switch (status) {
            case "active": return "status-badge-primary";
            case "completed": return "status-badge-success";
            case "paused": return "status-badge-warning";
            case "cancelled": return "status-badge-error";
            default: return "status-badge-secondary";
        }
    }

    @FXML
    private void showDashboard() {
        System.out.println("显示首页");
    }

    private String getVerificationStatusText(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "verified": return "已验证";
            case "pending": return "待验证";
            case "rejected": return "已拒绝";
            default: return status;
        }
    }

    @FXML
    private void newAccount() { openNewWindow("fxml/account/add.fxml", "新建账目"); }

    @FXML
    private void uploadReceipt() { openNewWindow("fxml/receipt/upload.fxml", "上传票据"); }

    @FXML
    private void projectList() { openNewWindow("fxml/project/list.fxml", "项目列表"); }

    @FXML
    private void addProject() { openNewWindow("fxml/project/add.fxml", "添加项目"); }

    @FXML
    private void projectCostAnalysis() { openNewWindow("fxml/project/cost_analysis.fxml", "项目成本分析"); }

    @FXML
    private void accountList() { openNewWindow("fxml/account/list-modern.fxml", "账目列表"); }

    @FXML
    private void addAccount() { openNewWindow("fxml/account/add.fxml", "添加账目"); }

    @FXML
    private void financialReport() { openNewWindow("fxml/account/report.fxml", "财务报表"); }

    @FXML
    private void accountVisualization() { openNewWindow("fxml/account/account-visualization.fxml", "账目流水可视化"); }

    @FXML
    private void receiptList() { openNewWindow("fxml/receipt/list.fxml", "票据列表"); }

    @FXML
    private void verifyReceipt() { openNewWindow("fxml/receipt/verify.fxml", "票据验证"); }

    @FXML
    private void approvalFlow() { openNewWindow("fxml/receipt/approval_flow.fxml", "审核流程"); }

    @FXML
    private void receiptVerification() { openNewWindow("fxml/receipt/list.fxml", "票据核销"); }

    @FXML
    private void materialList() { openNewWindow("fxml/material/list.fxml", "材料列表"); }

    @FXML
    private void addMaterial() { openNewWindow("fxml/material/add.fxml", "添加材料"); }

    @FXML
    private void materialReport() { openNewWindow("fxml/material/report.fxml", "材料报表"); }

    @FXML
    private void subcontractorList() { openNewWindow("fxml/subcontractor/list.fxml", "分包商列表"); }

    @FXML
    private void addSubcontractor() { openNewWindow("fxml/subcontractor/add.fxml", "添加分包商"); }

    @FXML
    private void paymentList() { openNewWindow("fxml/payment/list.fxml", "账户管理"); }

    @FXML
    private void transactionQuery() { openNewWindow("fxml/payment/transaction.fxml", "收支查询"); }

    @FXML
    private void electronicSealApplication() { openNewWindow("fxml/electronic-seal/application.fxml", "电子章使用申请"); }

    @FXML
    private void electronicSealApproval() { openNewWindow("fxml/electronic-seal/approval.fxml", "电子章申请审批"); }

    @FXML
    private void electronicSealStamp() { openNewWindow("fxml/electronic-seal/stamp.fxml", "电子盖章"); }

    @FXML
    private void electronicSealImport() { openNewWindow("fxml/electronic-seal/import.fxml", "电子章导入"); }

    @FXML
    private void electronicSealManagement() { openNewWindow("fxml/electronic-seal/management.fxml", "电子章管理"); }

    @FXML
    private void usbSealManagement() { openNewWindow("fxml/electronic-seal/usb-seal-management.fxml", "U盘印章安全管理"); }

    @FXML
    private void stampApprovalManagement() { openNewWindow("fxml/electronic-seal/stamp-approval.fxml", "盖章审批管理"); }

    @FXML
    private void myStampApproval() { openNewWindow("fxml/electronic-seal/my-stamp-approval.fxml", "我的盖章审批"); }

    @FXML
    private void projectCostReport() { openNewWindow("fxml/project/cost_analysis.fxml", "项目成本报表"); }

    @FXML
    private void systemSettings() { openNewWindow("fxml/system-settings.fxml", "系统设置"); }

    @FXML
    private void userManagement() { openNewWindow("fxml/user/list.fxml", "用户管理"); }

    @FXML
    private void about() { openNewWindow("fxml/about.fxml", "关于"); }

    @FXML
    private void createBackup() {
        // 权限检查：仅经理和管理员可执行备份
        User currentUser = clientSession != null ? clientSession.getCurrentUser() : null;
        if (currentUser == null || !("ADMIN".equals(currentUser.getRole()) || "MANAGER".equals(currentUser.getRole()))) {
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

            Map<String, Object> result = apiClient.post("/backup/create", null, Map.class);
            alert.close();

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("成功");
            successAlert.setHeaderText(null);
            successAlert.setContentText("备份成功！" + (result != null ? result.toString() : ""));
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
        User currentUser = clientSession != null ? clientSession.getCurrentUser() : null;
        if (currentUser == null || !("ADMIN".equals(currentUser.getRole()) || "MANAGER".equals(currentUser.getRole()))) {
            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
            warningAlert.setTitle("权限不足");
            warningAlert.setHeaderText(null);
            warningAlert.setContentText("抱歉，您没有权限执行数据库恢复操作。\n\n备份和恢复功能仅对经理和管理员开放。\n\n如需此权限，请联系系统管理员。");
            warningAlert.showAndWait();
            return;
        }

        try {
            File backupFile = chooseBackupFile();
            if (backupFile == null) return;
            if (!confirmRestore()) return;

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("恢复中");
            alert.setHeaderText(null);
            alert.setContentText("正在执行恢复操作，请稍候...");
            alert.show();

            Map<String, String> params = new java.util.HashMap<>();
            params.put("backupPath", backupFile.getAbsolutePath());
            apiClient.post("/backup/restore", params, Map.class);

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

    @FXML
    private void exit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("退出确认");
        alert.setHeaderText("确定要退出登录吗？");
        alert.setContentText("退出后需要重新登录才能使用系统。");

        ButtonType confirmButton = new ButtonType("确定退出", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == confirmButton) {
                if (clientSession != null) {
                    clientSession.clear();
                }
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        try {
            Stage currentStage = (Stage) projectCountLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-modern.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("盛韵建筑账目管理系统 - 登录");
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("错误");
            errorAlert.setHeaderText("返回登录界面失败");
            errorAlert.setContentText("错误信息: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    /**
     * 创建控制器工厂，用于远程客户端模式
     */
    private javafx.util.Callback<Class<?>, Object> createControllerFactory() {
        return controllerClass -> {
            try {
                Object controller = controllerClass.getDeclaredConstructor().newInstance();

                // 注入RemoteApiClient
                try {
                    java.lang.reflect.Method setApiClient = controllerClass.getMethod("setApiClient", RemoteApiClient.class);
                    setApiClient.invoke(controller, apiClient != null ? apiClient : RemoteApiClient.getInstance());
                } catch (NoSuchMethodException e) {
                    // 控制器不需要ApiClient，忽略
                }

                // 注入ClientSession
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
        };
    }

    private void openNewWindow(String fxmlPath, String title) {
        try {
            System.out.println("尝试打开窗口: " + fxmlPath + " - " + title);

            java.net.URL fxmlUrl = getClass().getResource("/" + fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("FXML文件不存在: " + fxmlPath);
            }
            System.out.println("FXML文件路径: " + fxmlUrl);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(createControllerFactory());
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            WindowUtils.setStageIcon(stage);
            Scene scene = new Scene(root);

            // 加载CSS样式表
            String[] cssFiles = {
                    "/css/design-system.css",
                    "/css/layout.css",
                    "/css/responsive.css",
                    "/css/components.css",
                    "/css/performance.css",
                    "/css/modern-theme.css"
            };

            for (String cssFile : cssFiles) {
                try {
                    java.net.URL cssUrl = getClass().getResource(cssFile);
                    if (cssUrl != null) {
                        scene.getStylesheets().add(cssUrl.toExternalForm());
                    }
                } catch (Exception cssEx) {
                    System.out.println("加载CSS文件失败: " + cssFile + " - " + cssEx.getMessage());
                }
            }

            stage.setScene(scene);

            // 根据不同的窗口设置合适的尺寸
            if (fxmlPath.contains("verify.fxml")) {
                stage.setWidth(900);
                stage.setHeight(650);
            } else if (fxmlPath.contains("approval_flow.fxml")) {
                stage.setWidth(1000);
                stage.setHeight(700);
            } else if (fxmlPath.contains("detail.fxml")) {
                stage.setWidth(700);
                stage.setHeight(600);
            } else if (fxmlPath.contains("upload.fxml")) {
                stage.setWidth(600);
                stage.setHeight(500);
            } else if (fxmlPath.contains("list.fxml")) {
                stage.setWidth(1100);
                stage.setHeight(700);
            } else if (fxmlPath.contains("account-visualization.fxml")) {
                stage.setWidth(1200);
                stage.setHeight(900);
                stage.setMinWidth(1000);
                stage.setMinHeight(700);
            }

            stage.showAndWait();
            loadStatistics();
        } catch (Exception e) {
            System.err.println("打开窗口失败，详细信息:");
            System.err.println("FXML的路: " + fxmlPath);
            System.err.println("窗口标题: " + title);
            System.err.println("错误类型: " + e.getClass().getName());
            System.err.println("错误消息: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("无法打开窗口");
            alert.setContentText("加载失败: " + e.getMessage() + "\n\nFXML文件: " + fxmlPath);
            alert.showAndWait();
        }
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

        return fileChooser.showOpenDialog(null);
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

    @SuppressWarnings("unchecked")
    private void loadNotificationBadge() {
        try {
            User currentUser = clientSession != null ? clientSession.getCurrentUser() : null;
            if (currentUser == null) {
                notificationBadgeLabel.setVisible(false);
                return;
            }
            Integer userId = currentUser.getUserId();
            List<Object> unreadList = apiClient.get("/api/notifications/user/unread", List.class);
            int unreadCount = unreadList != null ? unreadList.size() : 0;
            if (unreadCount > 0) {
                notificationBadgeLabel.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                notificationBadgeLabel.setVisible(true);
            } else {
                notificationBadgeLabel.setVisible(false);
            }
        } catch (Exception e) {
            System.out.println("加载通知徽章失败: " + e.getMessage());
            notificationBadgeLabel.setVisible(false);
        }
    }

    @FXML
    private void openNotificationCenter() {
        openNewWindow("fxml/notification/center.fxml", "通知中心");
        loadNotificationBadge();
    }

    @FXML
    private void openPreferences() {
        openNewWindow("fxml/notification/preferences.fxml", "通知偏好设置");
    }

    @FXML
    private void notificationManagement() {
        openNewWindow("fxml/notification/management.fxml", "通知管理");
    }

    @FXML
    private void performSearch() {
        if (searchField == null) return;

        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            showSearchTip("请输入搜索关键词");
            return;
        }

        try {
            javafx.scene.control.TabPane searchResultTabPane = createSearchResultWindow(keyword);

            Stage searchStage = new Stage();
            searchStage.initModality(Modality.APPLICATION_MODAL);
            searchStage.setTitle("搜索结果 - " + keyword);
            searchStage.setScene(new Scene(searchResultTabPane, 900, 600));
            searchStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("搜索错误");
            alert.setHeaderText(null);
            alert.setContentText("搜索失败：" + e.getMessage());
            alert.showAndWait();
        }
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.control.TabPane createSearchResultWindow(String keyword) {
        javafx.scene.control.TabPane tabPane = new javafx.scene.control.TabPane();
        tabPane.setStyle("-fx-padding: 10;");

        javafx.scene.control.Tab projectTab = new javafx.scene.control.Tab("项目");
        projectTab.setContent(createProjectSearchResult(keyword));
        projectTab.setClosable(false);

        javafx.scene.control.Tab receiptTab = new javafx.scene.control.Tab("票据");
        receiptTab.setContent(createReceiptSearchResult(keyword));
        receiptTab.setClosable(false);

        javafx.scene.control.Tab accountTab = new javafx.scene.control.Tab("账目");
        accountTab.setContent(createAccountSearchResult(keyword));
        accountTab.setClosable(false);

        tabPane.getTabs().addAll(projectTab, receiptTab, accountTab);

        return tabPane;
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.layout.VBox createProjectSearchResult(String keyword) {
        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(10);
        container.setStyle("-fx-padding: 15; -fx-background-color: white;");

        try {
            List<Map<String, Object>> projects = apiClient.get("/api/projects/search?keyword=" + keyword, List.class);

            javafx.scene.control.Label countLabel = new javafx.scene.control.Label(
                    "找到 " + (projects != null ? projects.size() : 0) + " 个相关项目");
            countLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-padding: 5 0;");
            container.getChildren().add(countLabel);

            javafx.scene.control.ListView<HBox> listView = new javafx.scene.control.ListView<>();
            listView.setPrefHeight(500);

            if (projects != null) {
                for (Map<String, Object> projectData : projects) {
                    String projectName = projectData.get("projectName") != null ? projectData.get("projectName").toString() : "未命名项目";
                    String clientName = projectData.get("clientName") != null ? projectData.get("clientName").toString() : "未设置";
                    Object budgetObj = projectData.get("budgetAmount");
                    String budget = budgetObj != null ? String.format("%.2f万元", ((Number) budgetObj).doubleValue() / 10000) : "未设置";
                    Object idObj = projectData.get("id");
                    Integer projectId = idObj != null ? ((Number) idObj).intValue() : null;

                    HBox item = createSearchResultItem(projectName, "客户：" + clientName, "预算：" + budget,
                            () -> { if (projectId != null) openProjectDetail(projectId); });
                    listView.getItems().add(item);
                }
            }

            container.getChildren().add(listView);
            VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS);

        } catch (Exception e) {
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label("搜索失败：" + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
            container.getChildren().add(errorLabel);
        }

        return container;
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.layout.VBox createReceiptSearchResult(String keyword) {
        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(10);
        container.setStyle("-fx-padding: 15; -fx-background-color: white;");

        try {
            List<Map<String, Object>> receipts = apiClient.get("/api/receipts/search?keyword=" + keyword, List.class);

            javafx.scene.control.Label countLabel = new javafx.scene.control.Label(
                    "找到 " + (receipts != null ? receipts.size() : 0) + " 个相关票据");
            countLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-padding: 5 0;");
            container.getChildren().add(countLabel);

            javafx.scene.control.ListView<HBox> listView = new javafx.scene.control.ListView<>();
            listView.setPrefHeight(500);

            if (receipts != null) {
                for (Map<String, Object> receiptData : receipts) {
                    String type = receiptData.get("type") != null ? receiptData.get("type").toString() : "";
                    String projectName = receiptData.get("projectName") != null ? receiptData.get("projectName").toString() : "未归属项目";
                    String reporterName = receiptData.get("reporterName") != null ? receiptData.get("reporterName").toString() : "未设置";
                    Object amountObj = receiptData.get("amount");
                    String amount = amountObj != null ? amountObj.toString() : "未设置";
                    String status = receiptData.get("verificationStatus") != null ? receiptData.get("verificationStatus").toString() : "";
                    Object idObj = receiptData.get("id");
                    Integer receiptId = idObj != null ? ((Number) idObj).intValue() : null;

                    HBox item = createSearchResultItem(
                            type + " - " + projectName,
                            "报票人：" + reporterName,
                            "金额：" + amount + " | 状态：" + getVerificationStatusText(status),
                            () -> { if (receiptId != null) openReceiptDetail(receiptId); });
                    listView.getItems().add(item);
                }
            }

            container.getChildren().add(listView);
            VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS);

        } catch (Exception e) {
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label("搜索失败：" + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
            container.getChildren().add(errorLabel);
        }

        return container;
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.layout.VBox createAccountSearchResult(String keyword) {
        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(10);
        container.setStyle("-fx-padding: 15; -fx-background-color: white;");

        try {
            List<Map<String, Object>> accounts = apiClient.get("/api/accounts/search?keyword=" + keyword, List.class);

            javafx.scene.control.Label countLabel = new javafx.scene.control.Label(
                    "找到 " + (accounts != null ? accounts.size() : 0) + " 个相关账目");
            countLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-padding: 5 0;");
            container.getChildren().add(countLabel);

            javafx.scene.control.ListView<HBox> listView = new javafx.scene.control.ListView<>();
            listView.setPrefHeight(500);

            if (accounts != null) {
                for (Map<String, Object> accountData : accounts) {
                    String description = accountData.get("description") != null ? accountData.get("description").toString() : "无描述";
                    String type = accountData.get("type") != null ? accountData.get("type").toString() : "未设置";
                    Object amountObj = accountData.get("amount");
                    String amount = amountObj != null ? amountObj.toString() : "未设置";
                    Object idObj = accountData.get("id");
                    Integer accountId = idObj != null ? ((Number) idObj).intValue() : null;

                    HBox item = createSearchResultItem(description, "类型：" + type, "金额：" + amount,
                            () -> { if (accountId != null) openAccountDetail(accountId); });
                    listView.getItems().add(item);
                }
            }

            container.getChildren().add(listView);
            VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS);

        } catch (Exception e) {
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label("搜索失败：" + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
            container.getChildren().add(errorLabel);
        }

        return container;
    }

    private HBox createSearchResultItem(String title, String subtitle1, String subtitle2, Runnable onClick) {
        HBox item = new HBox(15);
        item.setStyle("-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-cursor: hand;");
        item.setOnMouseEntered(e -> item.setStyle(
                "-fx-padding: 10; -fx-background-color: #e9ecef; -fx-background-radius: 5; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle(
                "-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-cursor: hand;"));
        item.setOnMouseClicked(e -> {
            if (onClick != null) onClick.run();
        });

        VBox content = new VBox(5);
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

        javafx.scene.control.Label subLabel1 = new javafx.scene.control.Label(subtitle1);
        subLabel1.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        javafx.scene.control.Label subLabel2 = new javafx.scene.control.Label(subtitle2);
        subLabel2.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        content.getChildren().addAll(titleLabel, subLabel1, subLabel2);
        item.getChildren().add(content);

        return item;
    }

    private void openProjectDetail(Integer projectId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/project/list.fxml"));
            loader.setControllerFactory(createControllerFactory());
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("项目详情");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openReceiptDetail(Integer receiptId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/receipt/detail.fxml"));
            loader.setControllerFactory(createControllerFactory());
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("票据详情");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openAccountDetail(Integer accountId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/account/list-modern.fxml"));
            loader.setControllerFactory(createControllerFactory());
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("账目详情");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSearchTip(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
