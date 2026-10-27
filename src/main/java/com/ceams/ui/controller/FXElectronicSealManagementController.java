package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.ElectronicSeal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class FXElectronicSealManagementController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TableView<ElectronicSeal> sealTable;

    @FXML
    private TableColumn<ElectronicSeal, Integer> idColumn;

    @FXML
    private TableColumn<ElectronicSeal, String> sealNameColumn;

    @FXML
    private TableColumn<ElectronicSeal, String> sealTypeColumn;

    @FXML
    private TableColumn<ElectronicSeal, String> statusColumn;

    @FXML
    private TableColumn<ElectronicSeal, String> createTimeColumn;

    @FXML
    private Button importButton;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        // 初始化表格列
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        sealNameColumn.setCellValueFactory(new PropertyValueFactory<>("sealName"));
        sealTypeColumn.setCellValueFactory(new PropertyValueFactory<>("sealType"));
        statusColumn.setCellValueFactory(cellData -> {
            ElectronicSeal seal = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(seal.getStatusText());
        });
        createTimeColumn.setCellValueFactory(cellData -> {
            ElectronicSeal seal = cellData.getValue();
            if (seal.getCreateTime() != null) {
                return new javafx.beans.property.SimpleStringProperty(dateFormat.format(seal.getCreateTime()));
            } else {
                return new javafx.beans.property.SimpleStringProperty("");
            }
        });

        // 加载电子章列表
        loadSeals();
    }

    @FXML
    private void importSeal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/electronic-seal/import.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("导入电子章");
            stage.setScene(scene);

            FXElectronicSealImportController importController = loader.getController();
            importController.setApiClient(apiClient);
            importController.setClientSession(clientSession);
            importController.setManagementController(this);

            stage.showAndWait();
            loadSeals();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "打开导入窗口失败: " + e.getMessage());
        }
    }

    public void loadSeals() {
        try {
            ElectronicSeal[] seals = apiClient.get("/api/electronic-seal/seals", ElectronicSeal[].class);
            sealTable.getItems().clear();
            if (seals != null) {
                sealTable.getItems().addAll(Arrays.asList(seals));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "加载电子章列表失败: " + e.getMessage());
        }
    }

    @FXML
    private void refreshList() {
        loadSeals();
    }

    @FXML
    private void deleteSelectedSeal() {
        ElectronicSeal selectedSeal = sealTable.getSelectionModel().getSelectedItem();
        if (selectedSeal == null) {
            showAlert(Alert.AlertType.WARNING, "警告", "请选择要删除的电子章");
            return;
        }

        // 显示确认对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确定要删除电子章 '" + selectedSeal.getSealName() + "' 吗？");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                try {
                    apiClient.delete("/api/electronic-seal/seals/" + selectedSeal.getId(), Void.class);
                    showAlert(Alert.AlertType.INFORMATION, "成功", "电子章删除成功");
                    loadSeals(); // 刷新列表
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "错误", "删除电子章失败: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void close() {
        // 关闭当前窗口
        Stage stage = (Stage) sealTable.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void openStampWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/electronic-seal/stamp.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("电子章盖章");
            stage.setScene(scene);
            WindowUtils.setStageIcon(stage);

            // 注入 apiClient 和 clientSession
            Object controller = loader.getController();
            try {
                java.lang.reflect.Method setApiClient = controller.getClass().getMethod("setApiClient", RemoteApiClient.class);
                setApiClient.invoke(controller, apiClient);
            } catch (NoSuchMethodException ignored) {}
            try {
                java.lang.reflect.Method setClientSession = controller.getClass().getMethod("setClientSession", ClientSession.class);
                setClientSession.invoke(controller, clientSession);
            } catch (NoSuchMethodException ignored) {}

            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "打开盖章窗口失败: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}