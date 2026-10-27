package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.ElectronicSeal;
import com.ceams.model.entity.ElectronicSealApplication;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class FXElectronicSealApplicationController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private ComboBox<ElectronicSeal> sealComboBox;

    @FXML
    private TextArea purposeTextArea;

    @FXML
    private Button submitButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TableView<ElectronicSealApplication> applicationTable;

    @FXML
    private TableColumn<ElectronicSealApplication, String> sealNameColumn;

    @FXML
    private TableColumn<ElectronicSealApplication, String> purposeColumn;

    @FXML
    private TableColumn<ElectronicSealApplication, String> statusColumn;

    @FXML
    private TableColumn<ElectronicSealApplication, String> applyTimeColumn;

    private ObservableList<ElectronicSeal> sealList;
    private ObservableList<ElectronicSealApplication> applicationList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化电子章下拉框
        loadSeals();

        // 初始化表格
        initTable();

        // 加载我的申请记录
        loadMyApplications();

        // 设置按钮事件
        submitButton.setOnAction(event -> submitApplication());
        cancelButton.setOnAction(event -> closeWindow());
    }

    private void loadSeals() {
        try {
            ElectronicSeal[] seals = apiClient.get("/api/electronic-seal/seals", ElectronicSeal[].class);
            sealList = FXCollections.observableArrayList();
            if (seals != null) {
                sealList.addAll(Arrays.asList(seals));
            }
            sealComboBox.setItems(sealList);

            // 设置显示的字段
            sealComboBox.setCellFactory(param -> new ListCell<ElectronicSeal>() {
                @Override
                protected void updateItem(ElectronicSeal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getSealName());
                    }
                }
            });

            sealComboBox.setButtonCell(new ListCell<ElectronicSeal>() {
                @Override
                protected void updateItem(ElectronicSeal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getSealName());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTable() {
        sealNameColumn.setCellValueFactory(new PropertyValueFactory<>("sealName"));
        purposeColumn.setCellValueFactory(new PropertyValueFactory<>("purpose"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusText"));
        applyTimeColumn.setCellValueFactory(new PropertyValueFactory<>("applyTime"));
    }

    private void loadMyApplications() {
        try {
            ElectronicSealApplication[] applications = apiClient.get("/api/electronic-seal/applications/my", ElectronicSealApplication[].class);
            applicationList = FXCollections.observableArrayList();
            if (applications != null) {
                applicationList.addAll(Arrays.asList(applications));
            }
            applicationTable.setItems(applicationList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void submitApplication() {
        ElectronicSeal selectedSeal = sealComboBox.getValue();
        String purpose = purposeTextArea.getText();

        if (selectedSeal == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择电子章");
            return;
        }

        if (purpose == null || purpose.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请输入使用目的");
            return;
        }

        ElectronicSealApplication application = new ElectronicSealApplication();
        application.setSealId(selectedSeal.getId());
        application.setSealName(selectedSeal.getSealName());
        application.setPurpose(purpose);

        try {
            apiClient.post("/api/electronic-seal/applications", application, Void.class);
            showAlert(Alert.AlertType.INFORMATION, "成功", "电子章申请已提交，等待经理审批");
            loadMyApplications();
            clearForm();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "提交申请失败: " + e.getMessage());
        }
    }

    private void clearForm() {
        sealComboBox.setValue(null);
        purposeTextArea.clear();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}