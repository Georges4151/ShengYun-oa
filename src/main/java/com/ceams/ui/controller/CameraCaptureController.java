package com.ceams.ui.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class CameraCaptureController {

    @FXML
    private ImageView cameraPreview;

    @FXML
    private ComboBox<String> cameraComboBox;

    private Webcam webcam;
    private boolean isCapturing = false;
    private File capturedImageFile;
    private Thread captureThread;

    @FXML
    public void initialize() {
        initCameraList();
    }

    private void initCameraList() {
        List<Webcam> webcams = Webcam.getWebcams();
        cameraComboBox.getItems().clear();

        String defaultWebcamName = null;
        try {
            Webcam defaultWebcam = Webcam.getDefault();
            if (defaultWebcam != null) {
                defaultWebcamName = defaultWebcam.getName();
            }
        } catch (Exception e) {
            // WebcamManager not available, use first webcam
        }

        for (Webcam cam : webcams) {
            String name = cam.getName();
            if (name.equals(defaultWebcamName)) {
                name += " (默认)";
            }
            cameraComboBox.getItems().add(name);
        }

        if (!webcams.isEmpty()) {
            if (defaultWebcamName != null) {
                for (String item : cameraComboBox.getItems()) {
                    if (item.startsWith(defaultWebcamName)) {
                        cameraComboBox.setValue(item);
                        break;
                    }
                }
            }
            if (cameraComboBox.getValue() == null) {
                cameraComboBox.setValue(cameraComboBox.getItems().get(0));
            }
        }
    }

    @FXML
    private void startCapture() {
        if (webcam != null) {
            stopCapture();
        }

        String selectedCamera = cameraComboBox.getValue();
        if (selectedCamera == null) {
            showAlert("请选择摄像头");
            return;
        }

        try {
            // 移除 "(默认)" 后缀
            String cameraName = selectedCamera.replace(" (默认)", "");

            List<Webcam> webcams = Webcam.getWebcams();
            for (Webcam cam : webcams) {
                if (cam.getName().equals(cameraName)) {
                    webcam = cam;
                    break;
                }
            }

            if (webcam == null) {
                webcam = Webcam.getDefault();
            }

            if (webcam == null) {
                showAlert("未找到摄像头设备");
                return;
            }

            // 设置分辨率
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            webcam.open();

            isCapturing = true;
            startPreviewThread();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("摄像头启动失败: " + e.getMessage());
        }
    }

    private void startPreviewThread() {
        captureThread = new Thread(() -> {
            while (isCapturing && webcam != null && webcam.isOpen()) {
                try {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        Image fxImage = SwingFXUtils.toFXImage(image, null);
                        javafx.application.Platform.runLater(() -> {
                            cameraPreview.setImage(fxImage);
                        });
                    }
                    Thread.sleep(33); // 约30fps
                } catch (Exception e) {
                    // 忽略预览错误
                    break;
                }
            }
        });
        captureThread.setDaemon(true);
        captureThread.start();
    }

    @FXML
    private void capturePhoto() {
        if (webcam == null || !webcam.isOpen()) {
            showAlert("请先启动摄像头");
            return;
        }

        try {
            BufferedImage image = webcam.getImage();
            if (image == null) {
                showAlert("拍照失败，未获取到图像");
                return;
            }

            // 保存到临时文件
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            capturedImageFile = new File(tempDir, "camera_capture_" + System.currentTimeMillis() + ".png");
            ImageIO.write(image, "PNG", capturedImageFile);

            Image fxImage = SwingFXUtils.toFXImage(image, null);
            cameraPreview.setImage(fxImage);

            showAlert("拍照成功");

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("保存照片失败: " + e.getMessage());
        }
    }

    @FXML
    private void confirmAndClose() {
        if (capturedImageFile == null || !capturedImageFile.exists()) {
            showAlert("请先拍照");
            return;
        }
        stopCapture();
        closeWindow();
    }

    @FXML
    private void cancel() {
        stopCapture();
        capturedImageFile = null;
        closeWindow();
    }

    private void stopCapture() {
        isCapturing = false;
        if (webcam != null) {
            if (webcam.isOpen()) {
                webcam.close();
            }
            webcam = null;
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) cameraComboBox.getScene().getWindow();
        stage.close();
    }

    public File getCapturedImageFile() {
        return capturedImageFile;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}