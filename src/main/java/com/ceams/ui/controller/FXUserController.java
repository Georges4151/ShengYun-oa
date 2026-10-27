package com.ceams.ui.controller;

import com.ceams.model.PageResult;
import com.ceams.model.entity.User;
import com.ceams.client.RemoteApiClient;
import com.ceams.client.ClientSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FXUserController implements Initializable {

    private RemoteApiClient remoteApiClient;

    private ClientSession clientSession;

    /**
     * 设置远程API客户端
     * 
     * @param apiClient 远程API客户端
     */
    public void setApiClient(RemoteApiClient apiClient) {
        this.remoteApiClient = apiClient;
    }

    /**
     * 设置远程API客户端（兼容旧方法名）
     */
    public void setRemoteApiClient(RemoteApiClient remoteApiClient) {
        this.remoteApiClient = remoteApiClient;
    }

    /**
     * 设置客户端会话
     * 
     * @param clientSession 客户端会话
     */
    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TableView<User> userTable;

    @FXML
    private javafx.scene.control.Pagination pagination;

    @FXML
    private TableColumn<User, Integer> userIdColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> statusColumn;

    @FXML
    private TableColumn<User, String> createTimeColumn;

    @FXML
    private TableColumn<User, Void> actionColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button addButton;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private int currentPage = 1;
    private int pageSize = 10;
    private String currentKeyword = "";

    // 角色和状态的映射关系
    private final String[][] ROLE_MAPPING = { { "admin", "管理员" }, { "finance", "财务人员" }, { "staff", "普通员工" } };
    private final String[][] STATUS_MAPPING = { { "ACTIVE", "启用" }, { "INACTIVE", "禁用" } };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        roleColumn.setCellValueFactory(cellData -> {
            String englishRole = cellData.getValue().getRole();
            return new SimpleStringProperty(getChineseRole(englishRole));
        });

        statusColumn.setCellValueFactory(cellData -> {
            String englishStatus = cellData.getValue().getStatus();
            return new SimpleStringProperty(getChineseStatus(englishStatus));
        });

        createTimeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreateTime() != null) {
                return new SimpleStringProperty(dateFormat.format(cellData.getValue().getCreateTime()));
            } else {
                return new SimpleStringProperty("");
            }
        });

        addActionColumn();

        initPagination();

        checkAdminPermission();

        loadUsers();
    }

    private void checkAdminPermission() {
        boolean isAdmin = isAdmin();
        addButton.setDisable(!isAdmin);
        addButton.setVisible(isAdmin);

        if (!isAdmin) {
            addButton.setTooltip(new Tooltip("只有管理员才能添加用户"));
        }
    }

    /**
     * 检查当前用户是否为管理员
     * 
     * @return true表示是管理员，false表示不是
     */
    private boolean isAdmin() {
        if (clientSession == null || clientSession.getCurrentUser() == null) {
            return false;
        }
        User currentUser = clientSession.getCurrentUser();
        return "admin".equals(currentUser.getRole());
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
        loadUsers();
        return userTable;
    }

    /**
     * 将英文角色转换为中文
     */
    private String getChineseRole(String englishRole) {
        for (String[] role : ROLE_MAPPING) {
            if (role[0].equals(englishRole)) {
                return role[1];
            }
        }
        return englishRole;
    }

    /**
     * 将英文状态转换为中文
     */
    private String getChineseStatus(String englishStatus) {
        for (String[] status : STATUS_MAPPING) {
            if (status[0].equals(englishStatus)) {
                return status[1];
            }
        }
        return englishStatus;
    }

    /**
     * 添加操作列
     */
    private void addActionColumn() {
        boolean isAdmin = isAdmin();

        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(TableColumn<User, Void> param) {
                final TableCell<User, Void> cell = new TableCell<>() {

                    private final Button editButton = new Button("编辑");
                    private final Button deleteButton = new Button("删除");
                    private final HBox hbox = new HBox(editButton, deleteButton);

                    {
                        hbox.setSpacing(5);
                        editButton
                                .setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 3 8 3 8;");
                        deleteButton
                                .setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 3 8 3 8;");

                        editButton.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            editUser(user);
                        });

                        deleteButton.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            deleteUser(user);
                        });

                        editButton.setVisible(isAdmin);
                        editButton.setManaged(isAdmin);
                        deleteButton.setVisible(isAdmin);
                        deleteButton.setManaged(isAdmin);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || !isAdmin) {
                            setGraphic(null);
                        } else {
                            setGraphic(hbox);
                        }
                    }
                };
                return cell;
            }
        };

        actionColumn.setCellFactory(cellFactory);
    }

    /**
     * 加载用户数据
     */
    private void loadUsers() {
        if (remoteApiClient == null) {
            showError("远程API客户端未初始化");
            return;
        }

        try {
            // 构建查询参数
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("pageNum", String.valueOf(currentPage));
            queryParams.put("pageSize", String.valueOf(pageSize));
            
            if (currentKeyword != null && !currentKeyword.isEmpty()) {
                queryParams.put("keyword", currentKeyword);
            }

            // 调用远程API获取用户列表
            PageResult<User> pageResult = remoteApiClient.get("/api/users", queryParams, PageResult.class);

            if (pageResult != null && pageResult.getList() != null) {
                List<User> users = pageResult.getList();
                ObservableList<User> data = FXCollections.observableArrayList(users);
                userTable.setItems(data);

                // 更新分页控件
                this.pagination.setCurrentPageIndex(currentPage - 1);
                this.pagination.setPageCount(pageResult.getPages());
            }
        } catch (IOException e) {
            showError("加载用户数据失败: " + e.getMessage());
        }
    }

    /**
     * 显示错误对话框
     * 
     * @param message 错误信息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 搜索用户
     */
    @FXML
    private void search() {
        currentKeyword = searchField.getText();
        currentPage = 1;
        loadUsers();
    }

    /**
     * 刷新用户列表
     */
    @FXML
    private void refresh() {
        searchField.clear();
        currentKeyword = "";
        currentPage = 1;
        loadUsers();
    }

    /**
     * 添加用户
     */
    @FXML
    private void addUser() {
        if (!isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("权限不足");
            alert.setHeaderText(null);
            alert.setContentText("只有管理员才能添加用户");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/add.fxml"));
            loader.setControllerFactory(clazz -> {
                try {
                    Object controller = clazz.getDeclaredConstructor().newInstance();
                    if (controller instanceof FXUserAddController) {
                        ((FXUserAddController) controller).setRemoteApiClient(remoteApiClient);
                        ((FXUserAddController) controller).setClientSession(clientSession);
                    }
                    return controller;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("添加用户");
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            currentPage = 1;
            loadUsers();
        } catch (IOException e) {
            showError("打开添加用户窗口失败: " + e.getMessage());
        }
    }

    /**
     * 编辑用户
     */
    private void editUser(User user) {
        if (!isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("权限不足");
            alert.setHeaderText(null);
            alert.setContentText("只有管理员才能编辑用户");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/add.fxml"));
            loader.setControllerFactory(clazz -> {
                try {
                    Object controller = clazz.getDeclaredConstructor().newInstance();
                    if (controller instanceof FXUserAddController) {
                        ((FXUserAddController) controller).setRemoteApiClient(remoteApiClient);
                        ((FXUserAddController) controller).setClientSession(clientSession);
                    }
                    return controller;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent root = loader.load();
            FXUserAddController controller = loader.getController();
            controller.setUser(user);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("编辑用户");
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadUsers();
        } catch (IOException e) {
            showError("打开编辑用户窗口失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    private void deleteUser(User user) {
        if (!isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("权限不足");
            alert.setHeaderText(null);
            alert.setContentText("只有管理员才能删除用户");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除确认");
        alert.setHeaderText("确定要删除用户吗？");
        alert.setContentText("用户名: " + user.getUsername() + "\n姓名: " + user.getName());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // 调用远程API删除用户
                    String deleteUrl = "/api/users/" + user.getUserId();
                    remoteApiClient.delete(deleteUrl, Void.class);
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("删除成功");
                    successAlert.setContentText("用户已成功删除");
                    successAlert.showAndWait();
                    loadUsers();
                } catch (IOException e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("删除失败");
                    errorAlert.setContentText("删除用户失败: " + e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        });
    }
}