package com.ceams.ui.controller;

import com.ceams.client.RemoteApiClient;
import com.ceams.client.ClientSession;
import com.ceams.model.dto.ProjectCostAnalysisDTO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class FXProjectCostAnalysisController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TableView<ProjectCostAnalysisDTO> analysisTable;

    @FXML
    private TableColumn<ProjectCostAnalysisDTO, Integer> projectIdColumn;

    @FXML
    private TableColumn<ProjectCostAnalysisDTO, String> projectNameColumn;

    @FXML
    private TableColumn<ProjectCostAnalysisDTO, BigDecimal> contractAmountColumn;

    @FXML
    private TableColumn<ProjectCostAnalysisDTO, BigDecimal> budgetAmountColumn;

    @FXML
    private TableColumn<ProjectCostAnalysisDTO, BigDecimal> actualAmountColumn;

    @FXML
    private TableColumn<ProjectCostAnalysisDTO, BigDecimal> budgetSurplusColumn;

    @FXML
    private TableColumn<ProjectCostAnalysisDTO, Double> costOverrunRateColumn;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
    private final DecimalFormat percentageFormat = new DecimalFormat("#.00%");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("FXProjectCostAnalysisController.initialize() called");
        projectIdColumn.setCellValueFactory(cellData -> {
            return new SimpleIntegerProperty(cellData.getValue().getProjectId()).asObject();
        });
        projectNameColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getProjectName());
        });
        contractAmountColumn.setCellValueFactory(cellData -> {
            return new SimpleObjectProperty<>(cellData.getValue().getContractAmount());
        });
        budgetAmountColumn.setCellValueFactory(cellData -> {
            return new SimpleObjectProperty<>(cellData.getValue().getBudgetAmount());
        });
        actualAmountColumn.setCellValueFactory(cellData -> {
            return new SimpleObjectProperty<>(cellData.getValue().getActualAmount());
        });
        budgetSurplusColumn.setCellValueFactory(cellData -> {
            return new SimpleObjectProperty<>(cellData.getValue().getBudgetSurplus());
        });
        costOverrunRateColumn.setCellValueFactory(cellData -> {
            Double costOverrunRate = cellData.getValue().getCostOverrunRate();
            return costOverrunRate != null ? new SimpleDoubleProperty(costOverrunRate).asObject() : null;
        });

        Callback<TableColumn<ProjectCostAnalysisDTO, BigDecimal>, javafx.scene.control.TableCell<ProjectCostAnalysisDTO, BigDecimal>> currencyCellFactory = column -> {
            return new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(currencyFormat.format(item));
                    }
                }
            };
        };

        Callback<TableColumn<ProjectCostAnalysisDTO, Double>, javafx.scene.control.TableCell<ProjectCostAnalysisDTO, Double>> percentageCellFactory = column -> {
            return new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(percentageFormat.format(item / 100));
                    }
                }
            };
        };

        contractAmountColumn.setCellFactory(currencyCellFactory);
        budgetAmountColumn.setCellFactory(currencyCellFactory);
        actualAmountColumn.setCellFactory(currencyCellFactory);
        budgetSurplusColumn.setCellFactory(currencyCellFactory);
        costOverrunRateColumn.setCellFactory(percentageCellFactory);

        loadCostAnalysisData();
    }

    private void loadCostAnalysisData() {
        System.out.println("FXProjectCostAnalysisController.loadCostAnalysisData() called");
        try {
            ProjectCostAnalysisDTO[] costAnalysisList = apiClient.get("/api/projects/cost-analysis",
                    ProjectCostAnalysisDTO[].class);
            ObservableList<ProjectCostAnalysisDTO> data = FXCollections
                    .observableArrayList(Arrays.asList(costAnalysisList));
            analysisTable.setItems(data);
            System.out.println("Analysis table items set, size: " + analysisTable.getItems().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refresh() {
        loadCostAnalysisData();
    }

    @FXML
    private void exportPDF() {
        System.out.println("exportPDF()方法被调用");
        try {
            byte[] reportBytes = apiClient.get("/api/reports/export/project-cost/pdf", byte[].class);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存项目成本分析报表PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF文件 (*.pdf)", "*.pdf"));
            fileChooser.setInitialFileName("项目成本分析报表_" + System.currentTimeMillis() + ".pdf");
            File file = fileChooser.showSaveDialog(new javafx.stage.Stage());
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(reportBytes);
                }
            }
        } catch (Exception e) {
            System.out.println("导出项目成本分析报表PDF失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void exportExcel() {
        System.out.println("exportExcel()方法被调用");
        try {
            byte[] reportBytes = apiClient.get("/api/reports/export/project-cost/excel", byte[].class);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存项目成本分析报表Excel");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel文件 (*.xlsx)", "*.xlsx"));
            fileChooser.setInitialFileName("项目成本分析报表_" + System.currentTimeMillis() + ".xlsx");
            File file = fileChooser.showSaveDialog(new javafx.stage.Stage());
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(reportBytes);
                }
            }
        } catch (Exception e) {
            System.out.println("导出项目成本分析报表Excel失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}