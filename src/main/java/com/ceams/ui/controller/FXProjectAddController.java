package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Project;
import com.ceams.model.entity.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FXProjectAddController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    private Integer projectId;

    @FXML
    private TextField projectName;

    @FXML
    private TextField clientName;

    @FXML
    private TextField projectAddress;

    @FXML
    private ComboBox<String> projectType;

    @FXML
    private DatePicker startDate;

    @FXML
    private DatePicker endDate;

    @FXML
    private TextField contractAmount;

    @FXML
    private TextField budgetAmount;

    @FXML
    private TextField actualAmount;

    @FXML
    private ComboBox<String> status;

    @FXML
    private TextArea description;

    @FXML
    private ComboBox<ManagerOption> managerComboBox;

    @FXML
    public void initialize() {
        loadManagers();
    }

    @SuppressWarnings("unchecked")
    private void loadManagers() {
        if (apiClient == null) {
            apiClient = RemoteApiClient.getInstance();
        }
        try {
            List<User> users = apiClient.get("/api/users", List.class);
            List<ManagerOption> managers = new ArrayList<>();
            if (users != null) {
                for (User u : users) {
                    if ("PROJECT_MANAGER".equals(u.getRole())) {
                        managers.add(new ManagerOption(u.getUserId(), u.getName()));
                    }
                }
            }
            managerComboBox.setItems(FXCollections.observableArrayList(managers));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ManagerOption {
        private Integer userId;
        private String userName;

        public ManagerOption(Integer userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        @Override
        public String toString() {
            return userName != null ? userName : "";
        }
    }

    /**
     * 设置要编辑的项目数据
     */
    public void setProject(Project project) {
        this.projectId = project.getId();
        projectName.setText(project.getProjectName());
        clientName.setText(project.getClientName());
        projectAddress.setText(project.getProjectAddress());
        projectType.setValue(project.getProjectType());

        if (project.getStartDate() != null) {
            startDate.setValue(project.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        if (project.getEndDate() != null) {
            endDate.setValue(project.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        if (project.getContractAmount() != null) {
            contractAmount.setText(project.getContractAmount().toString());
        }
        if (project.getBudgetAmount() != null) {
            budgetAmount.setText(project.getBudgetAmount().toString());
        }
        if (project.getActualAmount() != null) {
            actualAmount.setText(project.getActualAmount().toString());
        }

        status.setValue(project.getStatus());
        description.setText(project.getDescription());

        if (project.getManagerId() != null && project.getManagerName() != null) {
            for (ManagerOption option : managerComboBox.getItems()) {
                if (option.getUserId().equals(project.getManagerId())) {
                    managerComboBox.setValue(option);
                    break;
                }
            }
        }
    }

    @FXML
    private void save() {
        try {
            // 表单验证
            if (!validateForm()) {
                return;
            }

            if (apiClient == null) {
                apiClient = RemoteApiClient.getInstance();
            }

            Project project;

            if (projectId != null) {
                // 如果有项目ID，表示是编辑操作，先从服务器获取最新数据
                try {
                    project = apiClient.get("/api/projects/" + projectId, Project.class);
                } catch (IOException e) {
                    project = new Project();
                }
            } else {
                // 否则是添加操作
                project = new Project();
            }

            project.setProjectName(projectName.getText());
            project.setClientName(clientName.getText());
            project.setProjectAddress(projectAddress.getText());
            project.setProjectType(projectType.getValue());

            // 设置日期
            if (startDate.getValue() != null) {
                project.setStartDate(Date.from(startDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }
            if (endDate.getValue() != null) {
                project.setEndDate(Date.from(endDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }

            // 设置金额
            if (contractAmount.getText() != null && !contractAmount.getText().isEmpty()) {
                project.setContractAmount(new BigDecimal(contractAmount.getText()));
            }
            if (budgetAmount.getText() != null && !budgetAmount.getText().isEmpty()) {
                project.setBudgetAmount(new BigDecimal(budgetAmount.getText()));
            }
            // 实际金额可以编辑
            if (actualAmount.getText() != null && !actualAmount.getText().isEmpty()) {
                project.setActualAmount(new BigDecimal(actualAmount.getText()));
            }

            project.setStatus(status.getValue());
            project.setDescription(description.getText());

            ManagerOption selectedManager = managerComboBox.getValue();
            if (selectedManager != null) {
                project.setManagerId(selectedManager.getUserId());
                project.setManagerName(selectedManager.getUserName());
            } else {
                project.setManagerId(null);
                project.setManagerName(null);
            }

            if (projectId != null) {
                // 更新项目
                apiClient.put("/api/projects/" + projectId, project, Project.class);
            } else {
                // 添加项目
                apiClient.post("/api/projects", project, Project.class);
            }

            // 关闭当前窗口
            closeWindow();

        } catch (NumberFormatException e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("数据格式错误");
            alert.setContentText("金额格式不正确，请输入有效的数字");
            alert.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("保存项目失败");
            alert.setContentText("保存项目时出错: " + e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("保存项目失败");
            alert.setContentText("保存项目时出错: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * 表单验证
     * 
     * @return 是否验证通过
     */
    private boolean validateForm() {
        // 检查必填字段
        if (projectName.getText() == null || projectName.getText().trim().isEmpty()) {
            showAlert("警告", "项目名称不能为空", "请输入项目名称");
            projectName.requestFocus();
            return false;
        }

        if (clientName.getText() == null || clientName.getText().trim().isEmpty()) {
            showAlert("警告", "客户名称不能为空", "请输入客户名称");
            clientName.requestFocus();
            return false;
        }

        if (projectType.getValue() == null || projectType.getValue().trim().isEmpty()) {
            showAlert("警告", "项目类型不能为空", "请选择项目类型");
            projectType.requestFocus();
            return false;
        }

        if (status.getValue() == null || status.getValue().trim().isEmpty()) {
            showAlert("警告", "项目状态不能为空", "请选择项目状态");
            status.requestFocus();
            return false;
        }

        // 检查金额格式
        if (contractAmount.getText() != null && !contractAmount.getText().trim().isEmpty()) {
            if (!isValidNumber(contractAmount.getText())) {
                showAlert("警告", "合同金额格式不正确", "请输入有效的数字");
                contractAmount.requestFocus();
                return false;
            }
        }

        if (budgetAmount.getText() != null && !budgetAmount.getText().trim().isEmpty()) {
            if (!isValidNumber(budgetAmount.getText())) {
                showAlert("警告", "预算金额格式不正确", "请输入有效的数字");
                budgetAmount.requestFocus();
                return false;
            }
        }

        if (actualAmount.getText() != null && !actualAmount.getText().trim().isEmpty()) {
            if (!isValidNumber(actualAmount.getText())) {
                showAlert("警告", "实际金额格式不正确", "请输入有效的数字");
                actualAmount.requestFocus();
                return false;
            }
        }

        // 检查日期
        if (startDate.getValue() != null && endDate.getValue() != null) {
            if (startDate.getValue().isAfter(endDate.getValue())) {
                showAlert("警告", "日期范围错误", "开始日期不能晚于结束日期");
                startDate.requestFocus();
                return false;
            }
        }

        return true;
    }

    /**
     * 验证字符串是否为有效的数字
     * 
     * @param value 要验证的字符串
     * @return 是否为有效的数字
     */
    private boolean isValidNumber(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 显示警告对话框
     * 
     * @param title   标题
     * @param header  头部信息
     * @param content 内容
     */
    private void showAlert(String title, String header, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void cancel() {
        // 关闭当前窗口
        closeWindow();
    }

    /**
     * 关闭当前窗口
     */
    private void closeWindow() {
        Stage stage = (Stage) projectName.getScene().getWindow();
        stage.close();
    }
}