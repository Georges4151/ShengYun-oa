package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Subcontractor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import com.ceams.model.entity.Project;
import com.ceams.model.entity.SubcontractorContract;

public class FXSubcontractorController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TextField nameField;

    @FXML
    private TextField contactField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField contactPersonField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField qualificationLevelField;

    @FXML
    private TextField creditRatingField;

    @FXML
    private TableView<Subcontractor> subcontractorTable;

    @FXML
    private TableColumn<Subcontractor, Integer> idColumn;

    @FXML
    private TableColumn<Subcontractor, String> nameColumn;

    @FXML
    private TableColumn<Subcontractor, String> contactPersonColumn;

    @FXML
    private TableColumn<Subcontractor, String> phoneColumn;

    @FXML
    private TableColumn<Subcontractor, String> addressColumn;

    @FXML
    private TableColumn<Subcontractor, String> qualificationLevelColumn;

    @FXML
    private TableColumn<Subcontractor, String> creditRatingColumn;

    @FXML
    private TableColumn<Subcontractor, String> projectsColumn;

    @FXML
    private TableColumn<Subcontractor, Integer> contractCountColumn;

    @FXML
    private ListView<String> projectListView;

    @FXML
    private ComboBox<Project> projectComboBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("FXSubcontractorController.initialize() 开始执行");

        // 检查apiClient是否已注入
        if (apiClient == null) {
            System.out.println("apiClient尚未注入，使用默认单例");
            apiClient = RemoteApiClient.getInstance();
        }

        // 检查表格和列是否为null
        System.out.println("subcontractorTable: " + subcontractorTable);
        System.out.println("idColumn: " + idColumn);
        System.out.println("nameColumn: " + nameColumn);
        System.out.println("contactPersonColumn: " + contactPersonColumn);
        System.out.println("phoneColumn: " + phoneColumn);
        System.out.println("addressColumn: " + addressColumn);
        System.out.println("qualificationLevelColumn: " + qualificationLevelColumn);
        System.out.println("creditRatingColumn: " + creditRatingColumn);
        System.out.println("projectsColumn: " + projectsColumn);
        System.out.println("contractCountColumn: " + contractCountColumn);

        // 初始化表格列
        if (idColumn != null) {
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        }
        if (nameColumn != null) {
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        }
        if (contactPersonColumn != null) {
            contactPersonColumn.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));
        }
        if (phoneColumn != null) {
            phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        }
        if (addressColumn != null) {
            addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        }
        if (qualificationLevelColumn != null) {
            qualificationLevelColumn.setCellValueFactory(new PropertyValueFactory<>("qualificationLevel"));
        }
        if (creditRatingColumn != null) {
            creditRatingColumn.setCellValueFactory(new PropertyValueFactory<>("creditRating"));
        }
        // 初始化新的表格列
        if (projectsColumn != null) {
            projectsColumn.setCellValueFactory(cellData -> {
                Subcontractor subcontractor = cellData.getValue();
                if (subcontractor != null) {
                    List<String> projectNames = subcontractor.getProjectNames();
                    if (projectNames != null && !projectNames.isEmpty()) {
                        return new SimpleStringProperty(String.join(", ", projectNames));
                    } else {
                        // 尝试直接从projects列表获取项目名称
                        List<Project> projects = subcontractor.getProjects();
                        if (projects != null && !projects.isEmpty()) {
                            List<String> names = new ArrayList<>();
                            for (Project project : projects) {
                                if (project != null && project.getProjectName() != null) {
                                    names.add(project.getProjectName());
                                }
                            }
                            if (!names.isEmpty()) {
                                return new SimpleStringProperty(String.join(", ", names));
                            }
                        }
                        return new SimpleStringProperty("无");
                    }
                } else {
                    return new SimpleStringProperty("无");
                }
            });
            projectsColumn.setText("归属项目");
            // 设置列宽，确保内容能够完全显示
            projectsColumn.setPrefWidth(200);
        }
        if (contractCountColumn != null) {
            contractCountColumn.setCellValueFactory(new PropertyValueFactory<>("contractCount"));
            contractCountColumn.setText("合同数量");
        }

        // 加载项目列表到下拉菜单
        if (projectComboBox != null) {
            try {
                // 通过HTTP API获取项目列表
                ProjectListResponse response = apiClient.get("/api/projects", ProjectListResponse.class);
                if (response != null && response.getContent() != null) {
                    List<Project> allProjects = response.getContent();
                    ObservableList<Project> items = FXCollections.observableArrayList(allProjects);
                    projectComboBox.setItems(items);

                    // 设置单元格工厂，显示项目名称
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

                    // 设置按钮单元格工厂，显示项目名称
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
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("加载项目列表失败: " + e.getMessage());
            }
        }

        // 加载数据
        loadSubcontractors();

        // 添加双击事件监听器
        subcontractorTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                // 双击事件
                Subcontractor selected = subcontractorTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    // 打开合同管理窗口
                    FXSubcontractorContractController.openContractManagementWindow(selected.getId());
                }
            }
        });
    }

    /**
     * 打开项目选择窗口
     */
    private void openProjectSelectionWindow(int subcontractorId) {
        try {
            // 获取分包商信息
            Subcontractor subcontractor = apiClient.get("/api/subcontractors/" + subcontractorId, Subcontractor.class);
            if (subcontractor == null) {
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/subcontractor/project.fxml"));
            Parent root = loader.load();
            FXSubcontractorProjectController controller = loader.getController();
            if (controller != null) {
                controller.setApiClient(apiClient);
                controller.setClientSession(clientSession);
                controller.setSubcontractor(subcontractor);
            }
            Stage stage = new Stage();
            stage.setTitle("管理项目关联 - " + subcontractor.getName());
            stage.setScene(new Scene(root));
            WindowUtils.setStageIcon(stage);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载所有分包商数据
     */
    private void loadSubcontractors() {
        try {
            // 通过HTTP API获取所有分包商
            SubcontractorListResponse response = apiClient.get("/api/subcontractors", SubcontractorListResponse.class);
            List<Subcontractor> subcontractors;
            if (response != null && response.getContent() != null) {
                subcontractors = response.getContent();
            } else {
                subcontractors = new ArrayList<>();
            }
            ObservableList<Subcontractor> data = FXCollections.observableArrayList(subcontractors);
            subcontractorTable.setItems(data);
            System.out.println("加载分包商数据: " + subcontractors.size() + " 条记录");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("加载分包商数据失败: " + e.getMessage());
        }
    }

    /**
     * 刷新分包商列表
     */
    @FXML
    private void refresh() {
        loadSubcontractors();
    }

    /**
     * 查询分包商
     */
    @FXML
    private void search() {
        try {
            System.out.println("开始查询分包商");
            // 获取查询条件
            String name = nameField.getText();
            String contact = contactField.getText();
            String phone = phoneField.getText();

            // 获取选中的项目
            Project selectedProject = projectComboBox.getValue();
            Integer projectId = selectedProject != null ? selectedProject.getId() : null;

            System.out.println("查询条件: 名称=" + name + ", 联系人=" + contact + ", 电话=" + phone + ", 项目ID=" + projectId);

            // 构建查询参数
            Map<String, String> queryParams = new HashMap<>();
            if (name != null && !name.isEmpty()) {
                queryParams.put("name", name);
            }
            if (contact != null && !contact.isEmpty()) {
                queryParams.put("contactPerson", contact);
            }
            if (phone != null && !phone.isEmpty()) {
                queryParams.put("phone", phone);
            }
            if (projectId != null) {
                queryParams.put("projectId", String.valueOf(projectId));
            }

            // 通过HTTP API执行查询
            SubcontractorListResponse response = apiClient.get("/api/subcontractors", queryParams, SubcontractorListResponse.class);
            List<Subcontractor> subcontractors;
            if (response != null && response.getContent() != null) {
                subcontractors = response.getContent();
            } else {
                subcontractors = new ArrayList<>();
            }
            ObservableList<Subcontractor> data = FXCollections.observableArrayList(subcontractors);
            subcontractorTable.setItems(data);
            System.out.println("查询结果: " + subcontractors.size() + " 条记录");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("查询分包商时发生错误: " + e.getMessage());
        }
    }

    /**
     * 重置查询条件
     */
    @FXML
    private void reset() {
        try {
            System.out.println("重置查询条件");
            // 清除所有查询条件
            nameField.setText("");
            contactField.setText("");
            phoneField.setText("");
            projectComboBox.setValue(null);

            // 重新加载所有分包商数据
            loadSubcontractors();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("重置查询条件时发生错误: " + e.getMessage());
        }
    }

    /**
     * 添加分包商
     */
    @FXML
    private void addSubcontractor() {
        try {
            System.out.println("打开添加分包商窗口");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/subcontractor/add.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("添加分包商");
            stage.initModality(Modality.WINDOW_MODAL);
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            // 刷新分包商列表
            loadSubcontractors();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("打开添加分包商窗口时发生错误: " + e.getMessage());
        }
    }

    /**
     * 编辑分包商
     */
    @FXML
    private void edit() {
        try {
            System.out.println("编辑分包商");
            // 获取选中的分包商
            Subcontractor selected = subcontractorTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                System.out.println("请选择要编辑的分包商");
                return;
            }

            // 打开编辑窗口
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/subcontractor/edit.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();

            // 获取控制器并设置编辑数据
            FXSubcontractorController controller = loader.getController();
            controller.setEditingSubcontractor(selected);

            // 显示窗口
            Stage stage = new Stage();
            stage.setTitle("编辑分包商");
            stage.initModality(Modality.WINDOW_MODAL);
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // 刷新列表
            loadSubcontractors();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("编辑分包商时发生错误: " + e.getMessage());
        }
    }

    /**
     * 删除分包商
     */
    @FXML
    private void delete() {
        try {
            System.out.println("删除分包商");
            // 获取选中的分包商
            Subcontractor selected = subcontractorTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                System.out.println("请选择要删除的分包商");
                return;
            }

            // 确认删除
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText("确定要删除分包商 " + selected.getName() + " 吗？");
            alert.setContentText("此操作不可撤销");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // 通过HTTP API执行删除
                apiClient.delete("/api/subcontractors/" + selected.getId(), Void.class);
                System.out.println("分包商删除成功");
                // 刷新列表
                loadSubcontractors();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("删除分包商时发生错误: " + e.getMessage());
        }
    }

    // 用于编辑的分包商对象
    private Subcontractor editingSubcontractor;

    /**
     * 设置要编辑的分包商
     */
    public void setEditingSubcontractor(Subcontractor subcontractor) {
        this.editingSubcontractor = subcontractor;
        // 填充表单数据
        if (subcontractor != null) {
            nameField.setText(subcontractor.getName());
            contactPersonField.setText(subcontractor.getContactPerson());
            phoneField.setText(subcontractor.getPhone());
            addressField.setText(subcontractor.getAddress());
            qualificationLevelField.setText(subcontractor.getQualificationLevel());
            creditRatingField.setText(subcontractor.getCreditRating());

            // 加载并显示项目信息
            if (projectListView != null) {
                List<String> projectNames = subcontractor.getProjectNames();
                if (projectNames == null || projectNames.isEmpty()) {
                    projectListView.setItems(FXCollections.observableArrayList("无"));
                } else {
                    projectListView.setItems(FXCollections.observableArrayList(projectNames));
                }
            }
        }
    }

    /**
     * 保存分包商
     */
    @FXML
    private void saveSubcontractor() {
        try {
            System.out.println("保存分包商");
            if (editingSubcontractor != null) {
                // 更新现有分包商
                editingSubcontractor.setName(nameField.getText());
                editingSubcontractor.setContactPerson(contactPersonField.getText());
                editingSubcontractor.setPhone(phoneField.getText());
                editingSubcontractor.setAddress(addressField.getText());
                editingSubcontractor.setQualificationLevel(qualificationLevelField.getText());
                editingSubcontractor.setCreditRating(creditRatingField.getText());

                // 通过HTTP API更新分包商
                apiClient.put("/api/subcontractors/" + editingSubcontractor.getId(), editingSubcontractor, Void.class);
                System.out.println("分包商更新成功");
                // 关闭窗口
                cancelEdit();
            } else {
                // 保存新分包商（复用现有的save方法逻辑）
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("保存分包商时发生错误: " + e.getMessage());
        }
    }

    /**
     * 取消编辑
     */
    @FXML
    private void cancelEdit() {
        System.out.println("取消编辑分包商");
        // 关闭当前窗口
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    /**
     * 保存分包商
     */
    @FXML
    private void save() {
        try {
            System.out.println("保存分包商");
            // 创建新的分包商对象
            Subcontractor subcontractor = new Subcontractor();
            subcontractor.setName(nameField.getText());
            subcontractor.setContactPerson(contactPersonField.getText());
            subcontractor.setPhone(phoneField.getText());
            subcontractor.setAddress(addressField.getText());
            subcontractor.setQualificationLevel(qualificationLevelField.getText());
            subcontractor.setCreditRating(creditRatingField.getText());
            // 设置其他字段的默认值
            subcontractor.setBusinessScope("");
            subcontractor.setTaxIdentificationNumber("");
            subcontractor.setBankAccount("");
            subcontractor.setDescription("");
            // 通过HTTP API保存分包商
            Subcontractor result = apiClient.post("/api/subcontractors", subcontractor, Subcontractor.class);
            if (result != null) {
                System.out.println("分包商保存成功，ID: " + result.getId());

                // 显示成功信息
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("成功");
                alert.setHeaderText("分包商保存成功");
                alert.setContentText("分包商信息已成功保存");
                alert.showAndWait();

                // 关闭窗口
                cancel();
            } else {
                System.out.println("分包商保存失败");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText("分包商保存失败");
                alert.setContentText("请稍后重试");
                alert.showAndWait();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("保存分包商时发生错误: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("保存分包商时发生错误");
            alert.setContentText("错误信息: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * 取消添加
     */
    @FXML
    private void cancel() {
        // 关闭当前窗口
        System.out.println("取消添加分包商");
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    /**
     * 查看分包商合同
     */
    @FXML
    private void viewContracts() {
        try {
            System.out.println("查看分包商合同");
            // 获取选中的分包商
            Subcontractor selected = subcontractorTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                System.out.println("请选择要查看合同的分包商");
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("未选择分包商");
                alert.setContentText("请选择要查看合同的分包商");
                alert.showAndWait();
                return;
            }

            // 打开合同管理窗口
            FXSubcontractorContractController.openContractManagementWindow(selected.getId());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("查看分包商合同时发生错误: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("查看合同失败");
            alert.setContentText("查看分包商合同时发生错误: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * 管理分包商项目关联
     */
    @FXML
    private void manageProjects() {
        try {
            System.out.println("管理分包商项目关联");

            // 检查是否是编辑模式
            if (editingSubcontractor != null) {
                // 编辑模式：打开项目选择窗口
                openProjectSelectionWindow(editingSubcontractor.getId());

                // 通过HTTP API重新加载分包商信息，更新项目列表
                try {
                    Subcontractor updatedSubcontractor = apiClient.get(
                            "/api/subcontractors/" + editingSubcontractor.getId(), Subcontractor.class);
                    if (updatedSubcontractor != null) {
                        editingSubcontractor = updatedSubcontractor;
                        // 更新项目列表显示
                        if (projectListView != null) {
                            List<String> projectNames = updatedSubcontractor.getProjectNames();
                            if (projectNames == null || projectNames.isEmpty()) {
                                projectListView.setItems(FXCollections.observableArrayList("无"));
                            } else {
                                projectListView.setItems(FXCollections.observableArrayList(projectNames));
                            }
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.out.println("获取分包商详情失败: " + ex.getMessage());
                }
            } else {
                // 添加模式：提示用户先保存分包商
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("请先保存分包商");
                alert.setContentText("请先保存分包商信息，然后再管理项目关联");
                alert.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("管理分包商项目关联时发生错误: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("管理项目关联失败");
            alert.setContentText("管理分包商项目关联时发生错误: " + e.getMessage());
            alert.showAndWait();
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
     * 分包商列表响应内部类（用于解析API响应）
     */
    private static class SubcontractorListResponse {
        private List<Subcontractor> content;
        private int totalPages;
        private int totalElements;

        public List<Subcontractor> getContent() {
            return content;
        }

        public void setContent(List<Subcontractor> content) {
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
