package com.ceams.ui.controller;

import com.ceams.client.RemoteApiClient;
import com.ceams.client.ClientSession;
import com.ceams.model.entity.Material;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FXMaterialReportController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TextField categoryIdField;

    @FXML
    private TextField supplierIdField;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label averagePriceLabel;

    @FXML
    private Label maxPriceLabel;

    @FXML
    private Label minPriceLabel;

    @FXML
    private VBox categoryStatsBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    private void generateReport() {
        System.out.println("生成材料报表");
        try {
            String categoryIdStr = categoryIdField.getText();
            String supplierIdStr = supplierIdField.getText();
            Integer categoryId = categoryIdStr.isEmpty() ? null : Integer.parseInt(categoryIdStr);
            Integer supplierId = supplierIdStr.isEmpty() ? null : Integer.parseInt(supplierIdStr);

            Map<String, String> params = new HashMap<>();
            if (categoryId != null)
                params.put("categoryId", String.valueOf(categoryId));
            if (supplierId != null)
                params.put("supplierId", String.valueOf(supplierId));

            Material[] materials = apiClient.get("/api/materials", params, Material[].class);
            List<Material> materialList = Arrays.asList(materials);

            if (materialList != null && !materialList.isEmpty()) {
                int totalCount = materialList.size();
                double totalPrice = 0;
                double maxPrice = materialList.get(0).getCurrentPrice().doubleValue();
                double minPrice = materialList.get(0).getCurrentPrice().doubleValue();

                for (Material material : materialList) {
                    double price = material.getCurrentPrice().doubleValue();
                    totalPrice += price;
                    if (price > maxPrice) {
                        maxPrice = price;
                    }
                    if (price < minPrice) {
                        minPrice = price;
                    }
                }

                double averagePrice = totalPrice / totalCount;

                totalCountLabel.setText(String.valueOf(totalCount));
                averagePriceLabel.setText(String.format("%.2f", averagePrice));
                maxPriceLabel.setText(String.format("%.2f", maxPrice));
                minPriceLabel.setText(String.format("%.2f", minPrice));

                categoryStatsBox.getChildren().clear();
            } else {
                totalCountLabel.setText("0");
                averagePriceLabel.setText("0.00");
                maxPriceLabel.setText("0.00");
                minPriceLabel.setText("0.00");
                categoryStatsBox.getChildren().clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportPDF() {
        System.out.println("exportPDF()方法被调用");
        try {
            byte[] reportBytes = apiClient.get("/api/reports/export/material/pdf", byte[].class);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存材料报表PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF文件 (*.pdf)", "*.pdf"));
            fileChooser.setInitialFileName("材料报表_" + System.currentTimeMillis() + ".pdf");
            File file = fileChooser.showSaveDialog(new Stage());
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(reportBytes);
                }
            }
        } catch (Exception e) {
            System.out.println("导出材料报表PDF失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void exportExcel() {
        System.out.println("exportExcel()方法被调用");
        try {
            byte[] reportBytes = apiClient.get("/api/reports/export/material/excel", byte[].class);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存材料报表Excel");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel文件 (*.xlsx)", "*.xlsx"));
            fileChooser.setInitialFileName("材料报表_" + System.currentTimeMillis() + ".xlsx");
            File file = fileChooser.showSaveDialog(new Stage());
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(reportBytes);
                }
            }
        } catch (Exception e) {
            System.out.println("导出材料报表Excel失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void back() {
        Stage stage = (Stage) categoryIdField.getScene().getWindow();
        stage.close();
    }
}