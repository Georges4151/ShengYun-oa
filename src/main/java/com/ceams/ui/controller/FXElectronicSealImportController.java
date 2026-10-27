package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.ElectronicSeal;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class FXElectronicSealImportController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    private FXElectronicSealManagementController managementController;

    @FXML
    private TextField sealNameTextField;

    @FXML
    private ComboBox<String> sealTypeComboBox;

    @FXML
    private TextArea descriptionTextArea;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private ImageView sealImageView;

    private File selectedImageFile;

    @FXML
    public void initialize() {
        // 初始化下拉框选项
        sealTypeComboBox.getItems().addAll("公章", "财务章", "合同章", "法人章", "其他");
        statusComboBox.getItems().addAll("启用", "未启用", "已过期");
        statusComboBox.setValue("未启用");
    }

    public void setManagementController(FXElectronicSealManagementController controller) {
        this.managementController = controller;
    }

    @FXML
    private void importSeal() {
        String sealName = sealNameTextField.getText();
        String sealType = sealTypeComboBox.getValue();
        String description = descriptionTextArea.getText();
        String status = statusComboBox.getValue();

        // 验证输入
        if (sealName == null || sealName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请输入电子章名称");
            return;
        }

        if (sealType == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择电子章类型");
            return;
        }

        // 转换状态值
        String statusValue = "inactive";
        if ("启用".equals(status)) {
            statusValue = "active";
        } else if ("已过期".equals(status)) {
            statusValue = "expired";
        }

        // 创建电子章对象
        ElectronicSeal seal = new ElectronicSeal();
        seal.setSealName(sealName);
        seal.setSealType(sealType);
        seal.setDescription(description);
        seal.setStatus(statusValue);

        // 如果选择了图片，将其转换为Base64编码
        if (selectedImageFile != null) {
            try {
                String base64Image = convertFileToBase64(selectedImageFile);
                seal.setSealImage(base64Image);
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "错误", "图片编码失败: " + e.getMessage());
                return;
            }
        }

        try {
            apiClient.post("/api/electronic-seal/seals", seal, Void.class);
            showAlert(Alert.AlertType.INFORMATION, "成功", "电子章导入成功");
            clearForm();

            if (managementController != null) {
                managementController.loadSeals();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = e.getMessage();
            if (errorMessage == null) {
                errorMessage = e.getClass().getName();
            }
            showAlert(Alert.AlertType.ERROR, "错误", "导入电子章失败: " + errorMessage);
        }
    }

    @FXML
    private void clearForm() {
        sealNameTextField.clear();
        sealTypeComboBox.setValue(null);
        descriptionTextArea.clear();
        statusComboBox.setValue("未启用");
        sealImageView.setImage(null);
        selectedImageFile = null;
    }

    @FXML
    private void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择电子章图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));

        Stage stage = (Stage) sealImageView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedImageFile = file;
            // 显示图片预览
            try {
                FileInputStream fis = new FileInputStream(file);
                Image image = new Image(fis);
                sealImageView.setImage(image);
                fis.close();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "错误", "无法加载图片: " + e.getMessage());
            }
        }
    }

    private String convertFileToBase64(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fis.read(bytes);
        fis.close();
        return Base64.getEncoder().encodeToString(bytes);
    }

    @FXML
    private void cancel() {
        // 关闭当前窗口
        if (sealNameTextField.getScene() != null && sealNameTextField.getScene().getWindow() != null) {
            sealNameTextField.getScene().getWindow().hide();
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