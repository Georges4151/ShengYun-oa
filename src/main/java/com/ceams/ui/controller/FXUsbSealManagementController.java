package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.ElectronicSeal;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class FXUsbSealManagementController implements Initializable {

    private RemoteApiClient remoteApiClient;
    private ClientSession clientSession;

    /**
     * 设置远程API客户端
     *
     * @param remoteApiClient 远程API客户端
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
    private Label titleLabel;

    @FXML
    private Label usbStatusLabel;

    @FXML
    private ComboBox<String> usbDriveComboBox;

    @FXML
    private Button refreshUsbButton;

    @FXML
    private Button initUsbButton;

    @FXML
    private TableView<ElectronicSeal> sealTable;

    @FXML
    private TableColumn<ElectronicSeal, String> nameColumn;

    @FXML
    private TableColumn<ElectronicSeal, String> typeColumn;

    @FXML
    private TableColumn<ElectronicSeal, String> storageColumn;

    @FXML
    private TableColumn<ElectronicSeal, String> statusColumn;

    @FXML
    private TableColumn<ElectronicSeal, Integer> usageCountColumn;

    @FXML
    private ImageView sealPreview;

    @FXML
    private Button importToUsbButton;

    @FXML
    private Button importFromUsbButton;

    @FXML
    private Button deleteSealButton;

    @FXML
    private Button closeButton;

    private ObservableList<ElectronicSeal> sealList = FXCollections.observableArrayList();
    private String selectedDriveLetter;
    private ElectronicSeal selectedSeal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTable();
        refreshUsbDrives();
        loadSeals();
    }

    private void initTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("sealName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("sealType"));
        storageColumn.setCellValueFactory(new PropertyValueFactory<>("storageTypeText"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusText"));
        usageCountColumn.setCellValueFactory(new PropertyValueFactory<>("usageCount"));

        sealTable.setItems(sealList);

        sealTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedSeal = newVal;
            updateButtonStates();
            if (newVal != null) {
                previewSeal(newVal);
            }
        });
    }

    private void updateButtonStates() {
        boolean usbSelected = selectedDriveLetter != null;
        boolean sealSelected = selectedSeal != null;
        boolean isUsbSeal = sealSelected && selectedSeal.isUsbStorage();

        importToUsbButton.setDisable(!usbSelected || !sealSelected || isUsbSeal);
        importFromUsbButton.setDisable(!usbSelected);
        deleteSealButton.setDisable(!sealSelected);
    }

    @FXML
    private void onRefreshUsb() {
        refreshUsbDrives();
    }

    @SuppressWarnings("unchecked")
    private void refreshUsbDrives() {
        if (remoteApiClient == null) {
            showError("远程API客户端未初始化");
            return;
        }

        usbDriveComboBox.getItems().clear();

        try {
            Object result = remoteApiClient.get("/api/usb-detection/detect", Object.class);

            List<Map<String, Object>> driveList = null;
            if (result instanceof List) {
                driveList = (List<Map<String, Object>>) result;
            } else if (result instanceof Map) {
                Object data = ((Map<String, Object>) result).get("data");
                if (data instanceof List) {
                    driveList = (List<Map<String, Object>>) data;
                }
            }

            if (driveList == null || driveList.isEmpty()) {
                usbStatusLabel.setText("❌ 未检测到U盘");
                usbStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                selectedDriveLetter = null;
            } else {
                for (Map<String, Object> drive : driveList) {
                    String driveLetter = String.valueOf(drive.get("driveLetter"));
                    String description = String.valueOf(drive.get("description"));
                    String driveInfo = driveLetter + " - " + description;
                    usbDriveComboBox.getItems().add(driveInfo);
                }
                usbDriveComboBox.getSelectionModel().selectFirst();

                Map<String, String> firstDrive = new HashMap<>();
                firstDrive.put("driveLetter", String.valueOf(driveList.get(0).get("driveLetter")));
                selectedDriveLetter = firstDrive.get("driveLetter");

                updateUsbStatus(selectedDriveLetter);
            }
        } catch (IOException e) {
            showError("检测U盘失败: " + e.getMessage());
            selectedDriveLetter = null;
        }

        updateButtonStates();
    }

    @SuppressWarnings("unchecked")
    private void updateUsbStatus(String driveLetter) {
        try {
            Map<String, Object> info = remoteApiClient.get("/api/usb-detection/info/" + driveLetter, Map.class);

            Object data = info.get("data");
            if (data instanceof Map) {
                info = (Map<String, Object>) data;
            }

            Boolean hasStorage = (Boolean) info.get("hasSealStorage");
            if (Boolean.TRUE.equals(hasStorage)) {
                usbStatusLabel.setText("✅ U盘已初始化");
                usbStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            } else {
                usbStatusLabel.setText("⚠️ U盘未初始化");
                usbStatusLabel.setStyle("-fx-text-fill: #f39c12;");
            }
        } catch (IOException e) {
            usbStatusLabel.setText("⚠️ 无法获取U盘状态");
            usbStatusLabel.setStyle("-fx-text-fill: #f39c12;");
        }
    }

    @FXML
    private void onUsbDriveChanged() {
        int index = usbDriveComboBox.getSelectionModel().getSelectedIndex();
        if (remoteApiClient == null) {
            return;
        }

        try {
            Object result = remoteApiClient.get("/api/usb-detection/detect", Object.class);

            List<Map<String, Object>> driveList = null;
            if (result instanceof List) {
                driveList = (List<Map<String, Object>>) result;
            } else if (result instanceof Map) {
                Object data = ((Map<String, Object>) result).get("data");
                if (data instanceof List) {
                    driveList = (List<Map<String, Object>>) data;
                }
            }

            if (driveList != null && index >= 0 && index < driveList.size()) {
                selectedDriveLetter = String.valueOf(driveList.get(index).get("driveLetter"));
                updateUsbStatus(selectedDriveLetter);
            }
        } catch (IOException e) {
            showError("获取U盘信息失败: " + e.getMessage());
        }
        updateButtonStates();
    }

    @FXML
    private void onInitUsb() {
        if (selectedDriveLetter == null) {
            showError("请先选择U盘");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("初始化U盘");
        confirm.setHeaderText("确定要初始化此U盘吗？");
        confirm.setContentText("将在U盘创建电子章安全存储目录");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("driveLetter", selectedDriveLetter);
                remoteApiClient.post("/api/usb-detection/init-seal-storage", body, Object.class);
                showSuccess("U盘初始化成功！");
                refreshUsbDrives();
            } catch (Exception e) {
                showError("初始化失败: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onImportToUsb() {
        if (selectedDriveLetter == null) {
            showError("请先选择U盘");
            return;
        }

        if (selectedSeal == null) {
            showError("请选择要导入的电子章");
            return;
        }

        if (selectedSeal.isUsbStorage()) {
            showError("该电子章已存储在 U 盘中");
            return;
        }

        PasswordDialog passwordDialog = new PasswordDialog();
        passwordDialog.setTitle("设置密码");
        passwordDialog.setHeaderText("为电子章设置访问密码");
        passwordDialog.setPromptText("请输入密码:");

        Optional<String> passwordResult = passwordDialog.showAndWait();
        if (!passwordResult.isPresent() || passwordResult.get().trim().isEmpty()) {
            return;
        }

        String password = passwordResult.get().trim();

        PasswordDialog confirmDialog = new PasswordDialog();
        confirmDialog.setTitle("确认密码");
        confirmDialog.setHeaderText("请再次输入密码确认");
        confirmDialog.setPromptText("确认密码:");

        Optional<String> confirmResult = confirmDialog.showAndWait();
        if (!confirmResult.isPresent() || !confirmResult.get().trim().equals(password)) {
            showError("两次输入的密码不一致");
            return;
        }

        System.out.println("========== 开始导入电子章到 U 盘 ==========");
        System.out.println("电子章 ID: " + selectedSeal.getId());
        System.out.println("电子章名称: " + selectedSeal.getSealName());

        try {
            System.out.println("步骤1: 调用后端API导出电子章到U盘...");

            Map<String, Object> body = new HashMap<>();
            body.put("sealId", selectedSeal.getId());
            body.put("driveLetter", selectedDriveLetter);
            body.put("password", password);

            Object result = remoteApiClient.post("/api/electronic-seal/usb/export", body, Object.class);

            Boolean success = false;
            String sealFileId = null;

            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                Object dataObj = resultMap.get("data");
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    success = (Boolean) dataMap.get("success");
                    sealFileId = (String) dataMap.get("sealFileId");
                } else {
                    success = (Boolean) resultMap.get("success");
                    sealFileId = (String) resultMap.get("sealFileId");
                }
            }

            System.out.println("导出结果: " + success);

            if (Boolean.TRUE.equals(success)) {
                System.out.println("步骤2: 更新数据库记录...");
                selectedSeal.setStorageType("usb");
                if (sealFileId != null) {
                    selectedSeal.setSealFileId(sealFileId);
                }
                selectedSeal.setPasswordProtected(true);

                Map<String, Object> updateBody = new HashMap<>();
                updateBody.put("storageType", "usb");
                updateBody.put("sealFileId", selectedSeal.getSealFileId());
                updateBody.put("passwordProtected", true);
                remoteApiClient.put("/api/electronic-seal/seals/" + selectedSeal.getId(), updateBody, Object.class);

                System.out.println("========== 导入成功 ==========");
                showSuccess("电子章已成功导入 U 盘！");
                loadSeals();
            } else {
                System.out.println("错误: 导出返回失败");
                showError("导出到U盘失败，请重试");
            }
        } catch (Throwable e) {
            String errorType = e.getClass().getSimpleName();
            String errorMsg = e.getMessage();
            System.err.println("========== 导入失败 ==========");
            System.err.println("错误类型: " + errorType);
            System.err.println("错误信息: " + errorMsg);
            e.printStackTrace();

            String displayMsg = "导入失败 [" + errorType + "]";
            if (errorMsg != null && !errorMsg.isEmpty()) {
                displayMsg += ": " + errorMsg;
            }
            showError(displayMsg);
        }
    }

    @SuppressWarnings("unchecked")
    @FXML
    private void onImportFromUsb() {
        if (selectedDriveLetter == null) {
            showError("请先选择U盘");
            return;
        }

        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("driveLetter", selectedDriveLetter);
            Object result = remoteApiClient.get("/api/usb-detection/seal-files", queryParams, Object.class);

            List<String> sealFiles = null;
            if (result instanceof List) {
                sealFiles = (List<String>) result;
            } else if (result instanceof Map) {
                Object data = ((Map<String, Object>) result).get("data");
                if (data instanceof List) {
                    sealFiles = (List<String>) data;
                }
            }

            if (sealFiles == null || sealFiles.isEmpty()) {
                showError("U盘中没有电子章文件");
                return;
            }

            ChoiceDialog<String> choiceDialog = new ChoiceDialog<>(sealFiles.get(0), sealFiles);
            choiceDialog.setTitle("选择电子章");
            choiceDialog.setHeaderText("请选择要导入的电子章");
            choiceDialog.setContentText("电子章文件:");

            Optional<String> fileResult = choiceDialog.showAndWait();
            if (!fileResult.isPresent()) {
                return;
            }

            String selectedFileName = fileResult.get();

            PasswordDialog passwordDialog = new PasswordDialog();
            passwordDialog.setTitle("输入密码");
            passwordDialog.setHeaderText("请输入电子章访问密码");

            Optional<String> passwordResult = passwordDialog.showAndWait();
            if (!passwordResult.isPresent() || passwordResult.get().isEmpty()) {
                return;
            }

            String password = passwordResult.get();

            Map<String, Object> body = new HashMap<>();
            body.put("driveLetter", selectedDriveLetter);
            body.put("sealFileName", selectedFileName);
            body.put("password", password);

            Object importResult = remoteApiClient.post("/api/electronic-seal/import", body, Object.class);

            String imageData = null;
            if (importResult instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) importResult;
                Object data = resultMap.get("data");
                if (data instanceof Map) {
                    imageData = (String) ((Map<String, Object>) data).get("sealImage");
                } else {
                    imageData = (String) resultMap.get("sealImage");
                }
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存电子章图片");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PNG图片", "*.png"));
            fileChooser.setInitialFileName("seal_" + System.currentTimeMillis() + ".png");

            Stage stage = (Stage) closeButton.getScene().getWindow();
            File saveFile = fileChooser.showSaveDialog(stage);

            if (saveFile != null && imageData != null) {
                byte[] imageBytes = java.util.Base64.getDecoder().decode(imageData);
                java.nio.file.Files.write(saveFile.toPath(), imageBytes);
                showSuccess("电子章已成功导出到: " + saveFile.getAbsolutePath());
            }
        } catch (Exception e) {
            showError("导入失败，密码可能不正确或网络错误: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteSeal() {
        if (selectedSeal == null) {
            showError("请选择要删除的电子章");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("删除电子章");
        confirm.setHeaderText("确定要删除此电子章吗？");
        confirm.setContentText("电子章名称: " + selectedSeal.getSealName());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                remoteApiClient.delete("/api/electronic-seals/" + selectedSeal.getId(), Void.class);
                showSuccess("电子章已删除");
                loadSeals();
            } catch (Exception e) {
                showError("删除失败: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    @SuppressWarnings("unchecked")
    private void loadSeals() {
        if (remoteApiClient == null) {
            return;
        }

        try {
            sealList.clear();
            Object result = remoteApiClient.get("/api/electronic-seal/seals", Object.class);

            List<Map<String, Object>> sealMapList = null;
            if (result instanceof List) {
                sealMapList = (List<Map<String, Object>>) result;
            } else if (result instanceof Map) {
                Object data = ((Map<String, Object>) result).get("data");
                if (data instanceof List) {
                    sealMapList = (List<Map<String, Object>>) data;
                }
            }

            if (sealMapList != null) {
                List<ElectronicSeal> seals = new ArrayList<>();
                for (Map<String, Object> sealMap : sealMapList) {
                    ElectronicSeal seal = mapToElectronicSeal(sealMap);
                    seals.add(seal);
                }
                sealList.addAll(seals);
            }
        } catch (IOException e) {
            showError("加载电子章列表失败: " + e.getMessage());
        }
    }

    private ElectronicSeal mapToElectronicSeal(Map<String, Object> map) {
        ElectronicSeal seal = new ElectronicSeal();

        if (map.get("id") != null) {
            seal.setId(toInteger(map.get("id")));
        }
        seal.setSealName((String) map.get("sealName"));
        seal.setSealType((String) map.get("sealType"));
        seal.setDescription((String) map.get("description"));
        seal.setStatus((String) map.get("status"));
        seal.setSealImage((String) map.get("sealImage"));
        seal.setStorageType((String) map.get("storageType"));
        seal.setSealFileId((String) map.get("sealFileId"));
        seal.setUsbDriveId((String) map.get("usbDriveId"));

        if (map.get("passwordProtected") != null) {
            seal.setPasswordProtected(toBoolean(map.get("passwordProtected")));
        }
        if (map.get("usageCount") != null) {
            seal.setUsageCount(toInteger(map.get("usageCount")));
        }

        return seal;
    }

    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean toBoolean(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Boolean) return (Boolean) obj;
        return Boolean.parseBoolean(String.valueOf(obj));
    }

    private void previewSeal(ElectronicSeal seal) {
        try {
            if (seal.isUsbStorage()) {
                sealPreview.setImage(null);
                return;
            }

            String imagePath = seal.getSealImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                if (imagePath.startsWith("data:image")) {
                    int base64Index = imagePath.indexOf("base64,");
                    if (base64Index > 0) {
                        String base64Data = imagePath.substring(base64Index + 7);
                        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                        Image image = new Image(new ByteArrayInputStream(imageBytes));
                        sealPreview.setImage(image);
                    }
                } else if (imagePath.startsWith("iVBORw0KGgo") || imagePath.startsWith("/9j/") ||
                        imagePath.startsWith("R0lGOD") || imagePath.startsWith("UklGR")) {
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(imagePath);
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    sealPreview.setImage(image);
                } else {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        sealPreview.setImage(image);
                    }
                }
            }
        } catch (Exception e) {
            sealPreview.setImage(null);
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("成功");
        alert.setHeaderText("操作成功");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        System.err.println("显示错误: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText("操作失败");
        alert.setContentText(message != null ? message : "未知错误");
        alert.showAndWait();
    }

    public static class PasswordDialog extends Dialog<String> {
        private PasswordField passwordField;
        private Label contentLabel;

        public PasswordDialog() {
            setTitle("输入密码");
            setHeaderText("请输入密码");

            ButtonType loginButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

            passwordField = new PasswordField();
            passwordField.setPromptText("密码");

            contentLabel = new Label();
            contentLabel.setWrapText(true);

            VBox vbox = new VBox(10);
            vbox.getChildren().addAll(contentLabel, passwordField);
            vbox.setPadding(new javafx.geometry.Insets(10));

            getDialogPane().setContent(vbox);

            Platform.runLater(passwordField::requestFocus);

            setResultConverter(dialogButton -> {
                if (dialogButton == loginButtonType) {
                    return passwordField.getText();
                }
                return null;
            });
        }

        public void setPromptText(String text) {
            contentLabel.setText(text);
        }
    }
}
