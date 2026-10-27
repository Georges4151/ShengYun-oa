package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Project;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class FXProjectController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TableView<Project> projectTable;

    @FXML
    private javafx.scene.control.Pagination pagination;

    @FXML
    private TableColumn<Project, Integer> idColumn;

    @FXML
    private TableColumn<Project, String> projectNameColumn;

    @FXML
    private TableColumn<Project, String> clientNameColumn;

    @FXML
    private TableColumn<Project, String> projectTypeColumn;

    @FXML
    private TableColumn<Project, String> statusColumn;

    @FXML
    private TableColumn<Project, Object> contractAmountColumn;

    @FXML
    private TableColumn<Project, Object> budgetAmountColumn;

    @FXML
    private TableColumn<Project, Object> actualAmountColumn;

    @FXML
    private TableColumn<Project, String> managerNameColumn;

    @FXML
    private TextField searchField;

    private int currentPage = 1;
    private int pageSize = 10;
    private String currentKeyword = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化表格列
        idColumn.setCellValueFactory(new PropertyValueFactory<Project, Integer>("id"));
        projectNameColumn.setCellValueFactory(new PropertyValueFactory<Project, String>("projectName"));
        clientNameColumn.setCellValueFactory(new PropertyValueFactory<Project, String>("clientName"));
        projectTypeColumn.setCellValueFactory(new PropertyValueFactory<Project, String>("projectType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<Project, String>("status"));

        // 初始化金额列，使用自定义单元格工厂处理BigDecimal
        contractAmountColumn.setCellValueFactory(new PropertyValueFactory<Project, Object>("contractAmount"));
        contractAmountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Project, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else if (item instanceof java.math.BigDecimal) {
                    setText(((java.math.BigDecimal) item).toString());
                } else {
                    setText(item.toString());
                }
            }
        });

        budgetAmountColumn.setCellValueFactory(new PropertyValueFactory<Project, Object>("budgetAmount"));
        budgetAmountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Project, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else if (item instanceof java.math.BigDecimal) {
                    setText(((java.math.BigDecimal) item).toString());
                } else {
                    setText(item.toString());
                }
            }
        });

        actualAmountColumn.setCellValueFactory(new PropertyValueFactory<Project, Object>("actualAmount"));
        actualAmountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Project, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else if (item instanceof java.math.BigDecimal) {
                    setText(((java.math.BigDecimal) item).toString());
                } else {
                    setText(item.toString());
                }
            }
        });

        managerNameColumn.setCellValueFactory(new PropertyValueFactory<Project, String>("managerName"));

        // 初始化分页控件
        initPagination();

        // 加载项目数据
        loadProjects();
    }

    /**
     * 初始化分页控件
     */
    private void initPagination() {
        pagination.setPageFactory(this::createPage);
    }

    /**
     * 创建分页页面
     */
    private Node createPage(int pageIndex) {
        currentPage = pageIndex + 1;
        loadProjects();
        return projectTable;
    }

    /**
     * 刷新项目数据（由外部调用，替代事件监听）
     */
    public void refreshData() {
        Platform.runLater(() -> {
            loadProjects();
        });
    }

    /**
     * 加载所有项目数据（通过HTTP API）
     */
    private void loadProjects() {
        // 检查UI组件是否已经被初始化
        if (projectTable == null) {
            System.out.println("projectTable尚未初始化，跳过项目列表加载");
            return;
        }

        // 检查apiClient是否已注入
        if (apiClient == null) {
            System.out.println("apiClient尚未注入，使用默认单例");
            apiClient = RemoteApiClient.getInstance();
        }

        try {
            // 构建分页查询参数
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("page", String.valueOf(currentPage));
            queryParams.put("size", String.valueOf(pageSize));

            ProjectListResponse response;
            if (currentKeyword.isEmpty()) {
                // 调用后端API获取项目列表
                response = apiClient.get(
                    "/api/projects",
                    queryParams,
                    ProjectListResponse.class
                );
            } else {
                // 调用后端API搜索项目
                queryParams.put("keyword", currentKeyword);
                response = apiClient.get(
                    "/api/projects",
                    queryParams,
                    ProjectListResponse.class
                );
            }

            if (response != null && response.getContent() != null) {
                List<Project> projects = response.getContent();
                int totalPages = response.getTotalPages();

                ObservableList<Project> data = FXCollections.observableArrayList(projects);
                projectTable.setItems(data);

                // 更新分页控件
                this.pagination.setCurrentPageIndex(currentPage - 1);
                this.pagination.setPageCount(Math.max(totalPages, 1));
            } else {
                // 响应为空，清空表格
                projectTable.setItems(FXCollections.observableArrayList());
                this.pagination.setCurrentPageIndex(0);
                this.pagination.setPageCount(1);
            }

        } catch (IOException e) {
            System.err.println("加载项目数据失败: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("加载失败", "无法从服务器加载项目数据：" + e.getMessage());
        }
    }

    /**
     * 项目列表响应内部类（用于解析分页API响应）
     */
    private static class ProjectListResponse {
        private List<Project> content;
        private int totalPages;
        private int totalElements;
        private int number;
        private int size;

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
     * 刷新项目列表
     */
    @FXML
    private void refresh() {
        searchField.clear();
        currentKeyword = "";
        currentPage = 1;
        loadProjects();
    }

    /**
     * 查询项目
     */
    @FXML
    private void search() {
        System.out.println("查询项目");
        currentKeyword = searchField.getText();
        currentPage = 1;
        loadProjects();
    }

    /**
     * 添加项目
     */
    @FXML
    private void addProject() {
        System.out.println("添加项目");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/project/add.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("添加项目");
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            // 刷新项目列表，重置为第一页
            currentPage = 1;
            loadProjects();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("添加项目失败", "打开添加项目窗口时出错: " + e.getMessage());
        }
    }

    /**
     * 编辑项目
     */
    @FXML
    private void edit() {
        // 获取选中的项目
        Project selectedProject = projectTable.getSelectionModel().getSelectedItem();
        if (selectedProject != null) {
            System.out.println("编辑项目: " + selectedProject.getId());
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/project/add.fxml"));
                loader.setControllerFactory(this::createController);
                Parent root = loader.load();

                // 获取控制器并设置项目数据
                FXProjectAddController controller = loader.getController();
                controller.setProject(selectedProject);

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("编辑项目");
                WindowUtils.setStageIcon(stage);
                stage.setScene(new Scene(root));
                stage.showAndWait();

                // 刷新项目列表
                loadProjects();
            } catch (IOException e) {
                e.printStackTrace();
                showErrorAlert("编辑项目失败", "打开编辑项目窗口时出错: " + e.getMessage());
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText("未选择项目");
            alert.setContentText("请选择要编辑的项目");
            alert.showAndWait();
        }
    }

    /**
     * 删除项目（通过HTTP DELETE API）
     */
    @FXML
    private void delete() {
        Project selectedProject = projectTable.getSelectionModel().getSelectedItem();
        if (selectedProject != null) {
            // 弹出确认对话框
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText("删除项目");
            alert.setContentText("确定要删除ID为" + selectedProject.getId() + "的项目吗？此操作不可恢复。");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    System.out.println("删除项目: " + selectedProject.getId());
                    if (apiClient == null) {
                        apiClient = RemoteApiClient.getInstance();
                    }
                    apiClient.delete("/api/projects/" + selectedProject.getId(), Void.class);
                    System.out.println("项目删除成功");
                    // 刷新项目列表
                    loadProjects();
                } catch (IOException e) {
                    System.err.println("删除项目失败: " + e.getMessage());
                    e.printStackTrace();
                    showErrorAlert("删除失败", "无法删除项目：" + e.getMessage());
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText("未选择项目");
            alert.setContentText("请选择要删除的项目");
            alert.showAndWait();
        }
    }

    /**
     * 项目成本分析
     */
    @FXML
    private void projectCostAnalysis() {
        System.out.println("项目成本分析");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/project/cost_analysis.fxml"));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("项目成本分析");
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("打开成本分析窗口失败", "打开项目成本分析窗口时出错: " + e.getMessage());
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
}