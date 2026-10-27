package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.ElectronicSeal;
import com.ceams.model.dto.BatchStampResult;
import com.ceams.model.dto.BatchStampProgress;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FXBatchStampController implements Initializable {

    private RemoteApiClient remoteApiClient;
    private ClientSession clientSession;

    public void setRemoteApiClient(RemoteApiClient remoteApiClient) {
        this.remoteApiClient = remoteApiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TextField contractPathField;

    @FXML
    private ComboBox<ElectronicSeal> sealComboBox;

    @FXML
    private Slider xPositionSlider;

    @FXML
    private Label xPositionLabel;

    @FXML
    private Slider yPositionSlider;

    @FXML
    private Label yPositionLabel;

    @FXML
    private Slider scaleSlider;

    @FXML
    private Label scaleLabel;

    @FXML
    private TextField pageRangeField;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    private File selectedContract;
    private ExecutorService executorService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSliders();
        executorService = Executors.newSingleThreadExecutor();
        loadSeals();
    }

    private void loadSeals() {
        if (remoteApiClient == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "远程API客户端未初始化");
            return;
        }

        executorService.submit(() -> {
            try {
                ElectronicSeal[] sealsArray = remoteApiClient.get("/api/electronic-seal/seals", ElectronicSeal[].class);
                java.util.List<ElectronicSeal> seals = java.util.Arrays.asList(sealsArray);

                Platform.runLater(() -> {
                    sealComboBox.setItems(FXCollections.observableArrayList(seals));

                    sealComboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<ElectronicSeal>() {
                        @Override
                        protected void updateItem(ElectronicSeal seal, boolean empty) {
                            super.updateItem(seal, empty);
                            if (empty || seal == null) {
                                setText(null);
                            } else {
                                setText(seal.getSealName());
                            }
                        }
                    });

                    sealComboBox.setButtonCell(new javafx.scene.control.ListCell<ElectronicSeal>() {
                        @Override
                        protected void updateItem(ElectronicSeal seal, boolean empty) {
                            super.updateItem(seal, empty);
                            if (empty || seal == null) {
                                setText(null);
                            } else {
                                setText(seal.getSealName());
                            }
                        }
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", "加载印章列表失败: " + e.getMessage());
                });
            }
        });
    }

    private void setupSliders() {
        xPositionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            xPositionLabel.setText(String.format("%.0f%%", newVal.doubleValue()));
        });

        yPositionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            yPositionLabel.setText(String.format("%.0f%%", newVal.doubleValue()));
        });

        scaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            scaleLabel.setText(String.format("%.0f%%", newVal.doubleValue()));
        });
    }

    @FXML
    private void selectContract() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择合同文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF 文件", "*.pdf"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));

        Stage stage = (Stage) contractPathField.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedContract = file;
            contractPathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void startBatchStamp() {
        if (selectedContract == null || !selectedContract.exists()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先选择合同文件");
            return;
        }

        ElectronicSeal selectedSeal = sealComboBox.getValue();
        if (selectedSeal == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择印章");
            return;
        }

        String pageRange = pageRangeField.getText();
        final String finalPageRange = (pageRange == null || pageRange.trim().isEmpty()) ? "all" : pageRange;

        final float xPercent = (float) xPositionSlider.getValue();
        final float yPercent = (float) yPositionSlider.getValue();
        final float scale = (float) scaleSlider.getValue();

        if (clientSession == null || !clientSession.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "错误", "用户未登录");
            return;
        }

        progressBar.setProgress(0);
        progressLabel.setText("正在处理...");

        executorService.submit(() -> {
            try {
                Platform.runLater(() -> progressBar.setProgress(-1));

                Map<String, String> params = new HashMap<>();
                params.put("sealId", String.valueOf(selectedSeal.getId()));
                params.put("xPercent", String.valueOf(xPercent));
                params.put("yPercent", String.valueOf(yPercent));
                params.put("scale", String.valueOf(scale));
                params.put("pageRange", finalPageRange);

                BatchStampResult result = remoteApiClient.uploadFile(
                        "/api/electronic-seal/batch-stamp",
                        selectedContract,
                        "file",
                        params,
                        BatchStampResult.class);

                if (result != null && result.getTaskId() != null) {
                    pollProgress(result.getTaskId());
                } else {
                    Platform.runLater(() -> {
                        if (result != null && result.isSuccess()) {
                            progressBar.setProgress(1);
                            progressLabel.setText(String.format("完成！已盖章 %d 页", result.getStampedPages()));
                            showAlert(Alert.AlertType.INFORMATION, "成功",
                                    String.format("批量盖章成功！\n盖章页数: %d\n输出文件: %s",
                                            result.getStampedPages(), result.getOutputPath()));
                        } else {
                            progressBar.setProgress(0);
                            progressLabel.setText("失败");
                            showAlert(Alert.AlertType.ERROR, "失败",
                                    result != null ? result.getErrorMessage() : "未知错误");
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    progressLabel.setText("错误");
                    showAlert(Alert.AlertType.ERROR, "错误", "批量盖章失败: " + e.getMessage());
                });
            }
        });
    }

    private void pollProgress(String taskId) {
        executorService.submit(() -> {
            try {
                boolean completed = false;
                while (!completed) {
                    BatchStampProgress progress = remoteApiClient.get(
                            "/api/electronic-seal/batch-stamp/progress/" + taskId,
                            BatchStampProgress.class);

                    if (progress == null) {
                        Platform.runLater(() -> {
                            progressBar.setProgress(0);
                            progressLabel.setText("错误");
                            showAlert(Alert.AlertType.ERROR, "错误", "无法获取任务进度");
                        });
                        return;
                    }

                    final double progressValue = progress.getProgress();
                    final String status = progress.getStatus();

                    Platform.runLater(() -> {
                        progressBar.setProgress(progressValue);
                        progressLabel.setText(progress.getStatusMessage());
                    });

                    if ("COMPLETED".equalsIgnoreCase(status)) {
                        completed = true;
                        Platform.runLater(() -> {
                            progressBar.setProgress(1);
                            progressLabel.setText(String.format("完成！已盖章 %d 页", progress.getStampedPages()));
                            showAlert(Alert.AlertType.INFORMATION, "成功",
                                    String.format("批量盖章成功！\n盖章页数: %d\n输出文件: %s",
                                            progress.getStampedPages(), progress.getOutputPath()));
                        });
                    } else if ("FAILED".equalsIgnoreCase(status)) {
                        completed = true;
                        Platform.runLater(() -> {
                            progressBar.setProgress(0);
                            progressLabel.setText("失败");
                            showAlert(Alert.AlertType.ERROR, "失败", progress.getErrorMessage());
                        });
                    } else {
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    progressLabel.setText("已取消");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    progressLabel.setText("错误");
                    showAlert(Alert.AlertType.ERROR, "错误", "获取进度失败: " + e.getMessage());
                });
            }
        });
    }

    @FXML
    private void cancel() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        Stage stage = (Stage) contractPathField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
