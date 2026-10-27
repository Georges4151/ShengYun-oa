package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Account;
import com.ceams.model.entity.Project;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class ModernAccountController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private ComboBox<Project> projectComboBox;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Button searchButton;

    @FXML
    private Button resetButton;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<Account> accountTable;

    @FXML
    private TableColumn<Account, Integer> idColumn;

    @FXML
    private TableColumn<Account, String> projectNameColumn;

    @FXML
    private TableColumn<Account, String> typeColumn;

    @FXML
    private TableColumn<Account, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Account, String> descriptionColumn;

    @FXML
    private TableColumn<Account, Date> dateColumn;

    @FXML
    private TableColumn<Account, String> createdByNameColumn;

    @FXML
    private TableColumn<Account, Date> createdAtColumn;

    @FXML
    private TableColumn<Account, String> updatedByNameColumn;

    @FXML
    private TableColumn<Account, Date> updatedAtColumn;

    @FXML
    private Pagination pagination;

    private ObservableList<Account> accountList = FXCollections.observableArrayList();
    private List<Project> projectList = new ArrayList<>();
    private int currentPage = 0;
    private int pageSize = 20;
    private long totalCount = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化表格列
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        projectNameColumn.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));

        accountTable.setItems(accountList);

        // 初始化下拉框
        typeComboBox.getItems().addAll("全部", "收入", "支出", "预付款", "预收款", "借出款", "借入款", "结转", "其他");
        typeComboBox.setValue("全部");

        // 加载项目列表
        loadProjects();

        // 加载账目数据
        loadAccountsWithPagination();

        // 设置分页
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            currentPage = newIndex.intValue();
            loadAccountsWithPagination();
        });

        // 设置按钮事件
        searchButton.setOnAction(event -> searchAccounts());
        resetButton.setOnAction(event -> resetFilters());
        addButton.setOnAction(event -> openAddAccountDialog());
        editButton.setOnAction(event -> openEditAccountDialog());
        deleteButton.setOnAction(event -> deleteSelectedAccount());
        refreshButton.setOnAction(event -> refreshData());
    }

    private void loadProjects() {
        try {
            Project[] projects = apiClient.get("/api/projects", Project[].class);
            projectList = new ArrayList<>();
            if (projects != null) {
                projectList.addAll(Arrays.asList(projects));
            }
            projectComboBox.getItems().clear();
            projectComboBox.getItems().add(null); // 空选项代表"全部"
            projectComboBox.getItems().addAll(projectList);

            projectComboBox.setCellFactory(param -> new ListCell<Project>() {
                @Override
                protected void updateItem(Project item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("全部");
                    } else {
                        setText(item.getProjectName() == null || item.getProjectName().isEmpty() ? "全部" : item.getProjectName());
                    }
                }
            });

            projectComboBox.setButtonCell(new ListCell<Project>() {
                @Override
                protected void updateItem(Project item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("全部");
                    } else {
                        setText(item.getProjectName() == null || item.getProjectName().isEmpty() ? "全部" : item.getProjectName());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAccountsWithPagination() {
        try {

            Project selectedProject = projectComboBox.getValue();
            String selectedType = typeComboBox.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            Map<String, String> params = new HashMap<>();
            if (selectedProject != null) {
                params.put("projectId", String.valueOf(selectedProject.getId()));
            }
            if (selectedType != null && !selectedType.equals("全部")) {
                params.put("type", selectedType);
            }
            if (startDate != null) {
                params.put("startDate", startDate.toString());
            }
            if (endDate != null) {
                params.put("endDate", endDate.toString());
            }
            params.put("pageNum", String.valueOf(currentPage + 1));
            params.put("pageSize", String.valueOf(pageSize));

            // API 返回格式：无筛选条件时返回 {list, total, pageNum, pageSize}，有筛选条件时返回 List
            Object response = apiClient.get("/api/accounts", params, Object.class);

            accountList.clear();
            if (response instanceof List) {
                List<?> rawList = (List<?>) response;
                // 可能是 Account 列表
                for (Object item : rawList) {
                    if (item instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) item;
                        Account account = mapToAccount(map);
                        accountList.add(account);
                    }
                }
                totalCount = accountList.size();
            } else if (response instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) response;
                List<?> list = (List<?>) map.get("list");
                if (list != null) {
                    for (Object item : list) {
                        if (item instanceof Map) {
                            Map<String, Object> itemMap = (Map<String, Object>) item;
                            Account account = mapToAccount(itemMap);
                            accountList.add(account);
                        }
                    }
                }
                Number total = (Number) map.get("total");
                totalCount = total != null ? total.longValue() : 0;
            }

            updatePagination();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private Account mapToAccount(Map<String, Object> map) {
        Account account = new Account();
        account.setId(getIntValue(map, "id"));
        account.setProjectName((String) map.get("projectName"));
        account.setProjectId(getIntValue(map, "projectId"));
        String typeStr = (String) map.get("type");
        account.setType(typeStr);
        Object amountObj = map.get("amount");
        if (amountObj instanceof Number) {
            account.setAmount(new BigDecimal(amountObj.toString()));
        }
        account.setDescription((String) map.get("description"));
        Object dateObj = map.get("transactionDate");
        if (dateObj instanceof String) {
            try {
                LocalDate localDate = LocalDate.parse((String) dateObj);
                account.setTransactionDate(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            } catch (Exception e) {
                // ignore
            }
        }
        return account;
    }

    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private void updatePagination() {
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        pagination.setPageCount(Math.max(1, totalPages));
        pagination.setCurrentPageIndex(Math.min(currentPage, totalPages - 1));
    }

    private void searchAccounts() {
        currentPage = 0;
        loadAccountsWithPagination();
    }

    private void resetFilters() {
        projectComboBox.setValue(null);
        typeComboBox.setValue("全部");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        currentPage = 0;
        loadAccountsWithPagination();
    }

    private void refreshData() {
        loadAccountsWithPagination();
    }

    private void deleteSelectedAccount() {
        Account selectedAccount = accountTable.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) {
            showAlert(Alert.AlertType.WARNING, "警告", "请选择要删除的账目");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确定要删除此账目记录吗？");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                try {
                    apiClient.delete("/api/accounts/" + selectedAccount.getId(), Void.class);
                    showAlert(Alert.AlertType.INFORMATION, "成功", "账目删除成功");
                    loadAccountsWithPagination();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "错误", "删除账目失败: " + e.getMessage());
                }
            }
        });
    }

    private void openAddAccountDialog() {
        // TODO: 打开添加账目对话框
        showAlert(Alert.AlertType.INFORMATION, "提示", "添加账目功能待实现");
    }

    private void openEditAccountDialog() {
        Account selectedAccount = accountTable.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) {
            showAlert(Alert.AlertType.WARNING, "警告", "请选择要编辑的账目");
            return;
        }
        // TODO: 打开编辑账目对话框
        showAlert(Alert.AlertType.INFORMATION, "提示", "编辑账目功能待实现");
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) accountTable.getScene().getWindow();
        stage.close();
    }
}