package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class FXContractPreviewController implements Initializable {

    @FXML
    private ImageView pdfImageView;

    @FXML
    private Label pageLabel;

    @FXML
    private Label titleLabel;

    @FXML
    private Button prevPageButton;

    @FXML
    private Button nextPageButton;

    @FXML
    private Button zoomInButton;

    @FXML
    private Button zoomOutButton;

    @FXML
    private Button closeButton;

    @FXML
    private HBox navigationBox;

    private File contractFile;
    private String contractName;
    private PDDocument pdfDocument;
    private int currentPage = 0;
    private int totalPages = 0;
    private double zoomFactor = 1.0;
    private static final float RENDER_DPI = 100;

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updatePageNavigationButtons();
    }

    public void setContractFile(File file, String name) {
        this.contractFile = file;
        this.contractName = name;
        titleLabel.setText("合同预览 - " + name);
        loadPdfPreview();
    }

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    private void loadPdfPreview() {
        if (contractFile == null || !contractFile.exists()) {
            return;
        }

        try {
            if (pdfDocument != null) {
                pdfDocument.close();
            }

            pdfDocument = PDDocument.load(contractFile);
            totalPages = pdfDocument.getNumberOfPages();
            currentPage = 0;

            renderPdfPage(currentPage);
            updatePageLabel();
            updatePageNavigationButtons();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("无法加载PDF文件: " + e.getMessage());
        }
    }

    private void renderPdfPage(int pageIndex) {
        try {
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            float renderDpi = (float) (RENDER_DPI * zoomFactor);
            BufferedImage bufferedImage = renderer.renderImageWithDPI(pageIndex, renderDpi);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", baos);
            byte[] imageData = baos.toByteArray();
            Image image = new Image(new ByteArrayInputStream(imageData));

            pdfImageView.setImage(image);
            pdfImageView.setPreserveRatio(true);
            pdfImageView.setFitWidth(800 * zoomFactor);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("渲染PDF页面失败: " + e.getMessage());
        }
    }

    @FXML
    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPdfPage(currentPage);
            updatePageLabel();
            updatePageNavigationButtons();
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            renderPdfPage(currentPage);
            updatePageLabel();
            updatePageNavigationButtons();
        }
    }

    @FXML
    private void zoomIn() {
        if (zoomFactor < 2.0) {
            zoomFactor += 0.2;
            renderPdfPage(currentPage);
        }
    }

    @FXML
    private void zoomOut() {
        if (zoomFactor > 0.4) {
            zoomFactor -= 0.2;
            renderPdfPage(currentPage);
        }
    }

    @FXML
    private void close() {
        try {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void updatePageLabel() {
        pageLabel.setText(String.format("第 %d 页 / 共 %d 页", currentPage + 1, totalPages));
    }

    private void updatePageNavigationButtons() {
        prevPageButton.setDisable(currentPage <= 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1);
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}