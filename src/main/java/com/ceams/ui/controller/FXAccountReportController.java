package com.ceams.ui.controller;

import com.ceams.client.RemoteApiClient;
import com.ceams.client.ClientSession;
import com.ceams.model.entity.Project;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FXAccountReportController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TextField projectIdField;

    @FXML
    private ComboBox<Project> projectNameComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label totalIncomeLabel;

    @FXML
    private Label totalExpenseLabel;

    @FXML
    private Label netAmountLabel;

    @FXML
    private TextField yearField;

    @FXML
    private VBox monthlyStatsBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadProjects();
        generateReport();
    }

    @FXML
    public void generateReport() {
        System.out.println("生成财务报表");
        try {
            if (totalCountLabel == null || totalIncomeLabel == null || totalExpenseLabel == null
                    || netAmountLabel == null) {
                System.out.println("UI组件尚未初始化，跳过报表生成");
                return;
            }

            Integer projectId = null;
            if (projectIdField != null && !projectIdField.getText().trim().isEmpty()) {
                try {
                    projectId = Integer.parseInt(projectIdField.getText().trim());
                } catch (NumberFormatException e) {
                    projectId = null;
                }
            }

            if (projectId == null && projectNameComboBox != null && projectNameComboBox.getValue() != null) {
                Project selectedProject = projectNameComboBox.getValue();
                projectId = selectedProject.getId();
            }

            Map<String, String> params = new HashMap<>();
            if (projectId != null)
                params.put("projectId", String.valueOf(projectId));
            if (startDatePicker != null && startDatePicker.getValue() != null)
                params.put("startDate", startDatePicker.getValue().toString());
            if (endDatePicker != null && endDatePicker.getValue() != null)
                params.put("endDate", endDatePicker.getValue().toString());

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = apiClient.get("/api/accounts/summary", params, Map.class);

            if (summary != null) {
                Object totalCountObj = summary.get("total_count");
                int totalCount = totalCountObj != null
                        ? (totalCountObj instanceof Number ? ((Number) totalCountObj).intValue() : 0)
                        : 0;
                totalCountLabel.setText(String.valueOf(totalCount));

                BigDecimal totalIncome = toBigDecimal(summary.get("total_income"));
                BigDecimal totalExpense = toBigDecimal(summary.get("total_expense"));

                totalIncomeLabel.setText(totalIncome.toString());
                totalExpenseLabel.setText(totalExpense.toString());
                netAmountLabel.setText(totalIncome.subtract(totalExpense).toString());
            } else {
                totalCountLabel.setText("0");
                totalIncomeLabel.setText("0.00");
                totalExpenseLabel.setText("0.00");
                netAmountLabel.setText("0.00");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    @FXML
    public void viewMonthlyStats() {
        System.out.println("查看月度统计");
        try {
            Integer year = null;
            if (yearField != null && !yearField.getText().trim().isEmpty()) {
                try {
                    year = Integer.parseInt(yearField.getText().trim());
                } catch (NumberFormatException e) {
                    year = null;
                }
            }

            if (year == null) {
                year = java.time.Year.now().getValue();
            }

            Object result = apiClient.get("/api/accounts/monthly-stats/" + year, Object.class);
            List<Map<String, Object>> monthlyStats = null;
            if (result instanceof List) {
                monthlyStats = (List<Map<String, Object>>) result;
            }

            monthlyStatsBox.getChildren().clear();

            if (monthlyStats != null) {
                for (Map<String, Object> stat : monthlyStats) {
                    Object monthObj = stat.get("month");
                    if (monthObj == null)
                        continue;
                    Integer month = monthObj instanceof Number ? ((Number) monthObj).intValue() : null;
                    if (month == null)
                        continue;

                    BigDecimal income = toBigDecimal(stat.get("income"));
                    BigDecimal expense = toBigDecimal(stat.get("expense"));

                    HBox statRow = new HBox();
                    statRow.setSpacing(20);
                    statRow.setPadding(new Insets(5));

                    Label monthLabel = new Label(month + "月");
                    monthLabel.setMinWidth(50);

                    Label incomeLabel = new Label("收入: " + income.toString());
                    incomeLabel.setMinWidth(100);

                    Label expenseLabel = new Label("支出: " + expense.toString());
                    expenseLabel.setMinWidth(100);

                    statRow.getChildren().addAll(monthLabel, incomeLabel, expenseLabel);
                    monthlyStatsBox.getChildren().add(statRow);
                }
            }

            if (monthlyStats == null || monthlyStats.isEmpty()) {
                Label noDataLabel = new Label("该年份没有统计数据");
                monthlyStatsBox.getChildren().add(noDataLabel);
            }
        } catch (Exception e) {
            System.out.println("查看月度统计失败: " + e.getMessage());
            e.printStackTrace();
            monthlyStatsBox.getChildren().clear();
            Label errorLabel = new Label("获取月度统计数据失败: " + e.getMessage());
            monthlyStatsBox.getChildren().add(errorLabel);
        }
    }

    @FXML
    public void exportPDF() {
        System.out.println("exportPDF()方法被调用");
        try {
            Integer projectId = getProjectId();
            String startDateStr = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "";
            String endDateStr = endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "";

            Map<String, String> params = new HashMap<>();
            if (projectId != null)
                params.put("projectId", String.valueOf(projectId));
            if (!startDateStr.isEmpty())
                params.put("startDate", startDateStr);
            if (!endDateStr.isEmpty())
                params.put("endDate", endDateStr);

            byte[] reportBytes = apiClient.get("/api/reports/export/financial/pdf", params, byte[].class);

            Stage stage = (Stage) projectIdField.getScene().getWindow();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存财务报表PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF文件 (*.pdf)", "*.pdf"));
            fileChooser.setInitialFileName("财务报表_" + System.currentTimeMillis() + ".pdf");
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(reportBytes);
                }
            }
        } catch (Exception e) {
            System.out.println("导出财务报表PDF失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void exportExcel() {
        System.out.println("exportExcel()方法被调用");
        try {
            Integer projectId = getProjectId();
            String startDateStr = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "";
            String endDateStr = endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "";

            Map<String, String> params = new HashMap<>();
            if (projectId != null)
                params.put("projectId", String.valueOf(projectId));
            if (!startDateStr.isEmpty())
                params.put("startDate", startDateStr);
            if (!endDateStr.isEmpty())
                params.put("endDate", endDateStr);

            byte[] reportBytes = apiClient.get("/api/reports/export/financial/excel", params, byte[].class);

            Stage stage = (Stage) projectIdField.getScene().getWindow();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存财务报表Excel");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel文件 (*.xlsx)", "*.xlsx"));
            fileChooser.setInitialFileName("财务报表_" + System.currentTimeMillis() + ".xlsx");
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(reportBytes);
                }
            }
        } catch (Exception e) {
            System.out.println("导出财务报表Excel失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void back() {
        Stage stage = (Stage) projectIdField.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void refreshProjectList() {
        loadProjects();
    }

    private void loadProjects() {
        if (projectNameComboBox != null) {
            try {
                Project[] projects = apiClient.get("/api/projects", Project[].class);
                List<Project> projectList = Arrays.asList(projects);
                projectNameComboBox.getItems().clear();
                projectNameComboBox.getItems().addAll(projectList);

                projectNameComboBox.setCellFactory(param -> new ListCell<Project>() {
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

                projectNameComboBox.setButtonCell(new ListCell<Project>() {
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
            }
        }
    }

    private Integer getProjectId() {
        if (projectIdField != null && !projectIdField.getText().trim().isEmpty()) {
            try {
                return Integer.parseInt(projectIdField.getText().trim());
            } catch (NumberFormatException e) {
            }
        }
        if (projectNameComboBox != null && projectNameComboBox.getValue() != null) {
            return projectNameComboBox.getValue().getId();
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return BigDecimal.ZERO;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        if (value instanceof Number)
            return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}