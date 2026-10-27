package com.ceams.ui.controller;

import com.ceams.client.RemoteApiClient;
import com.ceams.client.ClientSession;
import com.ceams.model.entity.ElectronicSeal;
import com.ceams.model.entity.ElectronicSealStamp;
import com.ceams.model.entity.Project;
import com.ceams.model.entity.StampApproval;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FXElectronicSealStampController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private ComboBox<Project> projectComboBox;

    @FXML
    private Label contractNameLabel;

    @FXML
    private ComboBox<ElectronicSeal> sealComboBox;

    @FXML
    private TableView<ElectronicSealStamp> stampTable;

    @FXML
    private TableColumn<ElectronicSealStamp, String> sealNameColumn;

    @FXML
    private TableColumn<ElectronicSealStamp, String> projectNameColumn;

    @FXML
    private TableColumn<ElectronicSealStamp, String> contractNameColumn;

    @FXML
    private TableColumn<ElectronicSealStamp, String> positionColumn;

    @FXML
    private TableColumn<ElectronicSealStamp, String> stampTimeColumn;

    @FXML
    private Button closeButton;

    @FXML
    private Button selectFileButton;

    @FXML
    private Button stampButton;

    @FXML
    private Button exportPdfButton;

    @FXML
    private Button prevPageButton;

    @FXML
    private Button nextPageButton;

    @FXML
    private Label fileNameLabel;

    @FXML
    private Label pageLabel;

    @FXML
    private Pane previewPane;

    @FXML
    private ImageView pdfImageView;

    @FXML
    private ImageView sealImageView;

    @FXML
    private Label positionLabel;

    private File selectedFile;
    private String selectedFilePath;
    private PDDocument pdfDocument;
    private int currentPage = 0;
    private int totalPages = 0;

    // PDF实际页面尺寸（以点为单位，1点=1/72英寸）
    private float pdfPageWidth = 0;
    private float pdfPageHeight = 0;

    // 渲染DPI
    private static final float RENDER_DPI = 100;

    // 显示缩放比例
    private double displayScale = 1.0;

    // 渲染后的图像尺寸
    private double renderedWidth = 0;
    private double renderedHeight = 0;

    // 印章在PDF中的目标尺寸（以点为单位，约50毫米）
    private static final double SEAL_SIZE_IN_PDF = 142.0; // 50mm = 142点

    // 印章拖动相关
    private double sealOffsetX = 0;
    private double sealOffsetY = 0;
    private double sealX = 0;
    private double sealY = 0;
    private boolean isDragging = false;

    // 多印章位置列表
    private static class StampPosition {
        ElectronicSeal seal;
        int pageNum;
        double x;
        double y;
        double size;

        StampPosition(ElectronicSeal seal, int pageNum, double x, double y, double size) {
            this.seal = seal;
            this.pageNum = pageNum;
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }

    private List<StampPosition> stampPositions = new java.util.ArrayList<>();

    private ObservableList<Project> projectList;
    private ObservableList<ElectronicSeal> sealList;
    private ObservableList<ElectronicSealStamp> stampList;
    private List<Project> allProjects;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @FXML
    private Label totalCountLabel;

    @FXML
    private ListView<String> stampPositionListView;

    @FXML
    private TextField pageRangeField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化下拉框
        loadProjects();
        loadSeals();

        // 初始化表格
        initTable();

        // 加载盖章记录
        loadStamps();

        // 设置项目选择监听
        projectComboBox.setOnAction(event -> {
            // 项目选择改变时，可以执行其他操作
        });

        // 设置印章选择监听
        sealComboBox.setOnAction(event -> loadSealImage());

        // 初始化印章拖动
        initSealDrag();

        // 初始化页面导航按钮状态
        updatePageNavigationButtons();

        // 初始化印章位置列表
        stampPositionListView.setItems(FXCollections.observableArrayList());
    }

    private void loadProjects() {
        try {
            allProjects = apiClient.get("/api/projects", List.class);
            projectList = FXCollections.observableArrayList(allProjects);
            projectComboBox.setItems(projectList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "加载项目列表失败: " + e.getMessage());
        }

        projectComboBox.setCellFactory(param -> new ListCell<Project>() {
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

        projectComboBox.setButtonCell(new ListCell<Project>() {
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
    }

    private void loadSeals() {
        try {
            List<ElectronicSeal> seals = apiClient.get("/api/electronic-seal/seals", List.class);
            sealList = FXCollections.observableArrayList(seals);
            sealComboBox.setItems(sealList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "加载电子印章失败: " + e.getMessage());
        }

        sealComboBox.setCellFactory(param -> new ListCell<ElectronicSeal>() {
            @Override
            protected void updateItem(ElectronicSeal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String storageTag = item.isUsbStorage() ? " [U盘]" : "";
                    setText(item.getSealName() + storageTag);
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
                    String storageTag = item.isUsbStorage() ? " [U盘]" : "";
                    setText(item.getSealName() + storageTag);
                }
            }
        });
    }

    private void loadSealImage() {
        ElectronicSeal selectedSeal = sealComboBox.getValue();
        if (selectedSeal == null) {
            return;
        }

        if (selectedSeal.isUsbStorage()) {
            handleUsbSeal(selectedSeal);
        } else {
            loadLocalSealImage(selectedSeal);
        }
    }

    private void handleUsbSeal(ElectronicSeal seal) {
        try {
            // 调用后端API检测U盘
            Map<String, Object> usbInfo = apiClient.get("/api/usb-detection/detect", Map.class);
            List<?> usbDrives = (List<?>) usbInfo.get("usbDrives");

            System.out.println("=== U盘印章验证 ===");
            System.out.println("检测到的U盘数量: " + (usbDrives != null ? usbDrives.size() : 0));

            if (usbDrives == null || usbDrives.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "U盘未插入",
                        "该电子章存储在U盘中，请先插入U盘后再使用。");
                sealComboBox.setValue(null);
                sealImageView.setImage(null);
                return;
            }

            String sealFileId = seal.getSealFileId();
            System.out.println("电子章文件ID: " + sealFileId);

            if (sealFileId == null || sealFileId.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "错误", "电子章文件ID无效");
                sealComboBox.setValue(null);
                sealImageView.setImage(null);
                return;
            }

            // 调用后端API从U盘导入印章
            Map<String, Object> importRequest = new HashMap<>();
            importRequest.put("sealFileId", sealFileId);

            Map<String, Object> importResult = apiClient.post("/api/electronic-seal/import", importRequest, Map.class);

            if (importResult == null || !importResult.containsKey("sealImage")) {
                showAlert(Alert.AlertType.ERROR, "导入失败", "无法从U盘导入电子章");
                sealComboBox.setValue(null);
                sealImageView.setImage(null);
                return;
            }

            String sealImageBase64 = (String) importResult.get("sealImage");
            byte[] imageBytes = Base64.getDecoder().decode(sealImageBase64);

            Image image = new Image(new ByteArrayInputStream(imageBytes));
            sealImageView.setImage(image);

            updateSealDisplaySize();

            if (pdfImageView.getImage() != null) {
                resetSealPosition();
            }

            seal.setSealImage(sealImageBase64);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误",
                    "U盘印章处理失败: " + e.getMessage());
            sealComboBox.setValue(null);
            sealImageView.setImage(null);
        }
    }

    private void loadLocalSealImage(ElectronicSeal seal) {
        if (seal.getSealImage() != null) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(seal.getSealImage());
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                sealImageView.setImage(image);

                updateSealDisplaySize();

                if (pdfImageView.getImage() != null) {
                    resetSealPosition();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateSealDisplaySize() {
        // 计算印章在预览中的显示尺寸
        // 印章在PDF中的尺寸（点）-> 渲染尺寸（像素）-> 显示尺寸（像素）
        double sealRenderedSize = SEAL_SIZE_IN_PDF * RENDER_DPI / 72.0;
        double sealDisplaySize = sealRenderedSize * displayScale;

        sealImageView.setFitWidth(sealDisplaySize);
        sealImageView.setFitHeight(sealDisplaySize);
        sealImageView.setPreserveRatio(true);
    }

    private void resetSealPosition() {
        // 计算PDF预览区域的中心位置
        double pdfX = pdfImageView.getLayoutX();
        double pdfY = pdfImageView.getLayoutY();
        double pdfWidth = pdfImageView.getFitWidth();
        double pdfHeight = pdfImageView.getFitHeight();

        // 计算印章位置（相对于PDF预览区域）
        sealX = (pdfWidth - sealImageView.getFitWidth()) / 2;
        sealY = (pdfHeight - sealImageView.getFitHeight()) / 2;

        // 设置印章在previewPane中的绝对位置
        sealImageView.setLayoutX(pdfX + sealX);
        sealImageView.setLayoutY(pdfY + sealY);

        updatePositionLabel();
    }

    private void initSealDrag() {
        sealImageView.setOnMousePressed(this::onSealPressed);
        sealImageView.setOnMouseDragged(this::onSealDragged);
        sealImageView.setOnMouseReleased(this::onSealReleased);
    }

    private void onSealPressed(MouseEvent event) {
        if (sealImageView.getImage() == null)
            return;

        isDragging = true;
        // 记录鼠标在印章内的偏移（相对于印章本身）
        sealOffsetX = event.getX();
        sealOffsetY = event.getY();
        sealImageView.setOpacity(0.7);
    }

    private void onSealDragged(MouseEvent event) {
        if (!isDragging || sealImageView.getImage() == null)
            return;

        // 计算鼠标在previewPane中的绝对位置
        double mouseX = sealImageView.getLayoutX() + event.getX() - sealOffsetX;
        double mouseY = sealImageView.getLayoutY() + event.getY() - sealOffsetY;

        // 计算PDF预览区域的边界（实时获取）
        double pdfX = pdfImageView.getLayoutX();
        double pdfY = pdfImageView.getLayoutY();
        double pdfWidth = pdfImageView.getFitWidth();
        double pdfHeight = pdfImageView.getFitHeight();

        // 计算印章在PDF预览区域内的边界
        double minX = pdfX;
        double minY = pdfY;
        double maxX = pdfX + pdfWidth - sealImageView.getFitWidth();
        double maxY = pdfY + pdfHeight - sealImageView.getFitHeight();

        // 限制印章在PDF预览区域内
        double newX = Math.max(minX, Math.min(mouseX, maxX));
        double newY = Math.max(minY, Math.min(mouseY, maxY));

        // 更新印章位置
        sealImageView.setLayoutX(newX);
        sealImageView.setLayoutY(newY);

        // 计算印章相对于PDF预览区域的位置（用于坐标转换）
        sealX = newX - pdfX;
        sealY = newY - pdfY;

        updatePositionLabel();
    }

    private void onSealReleased(MouseEvent event) {
        isDragging = false;
        sealImageView.setOpacity(1.0);
    }

    private void updatePositionLabel() {
        if (pdfImageView.getImage() != null) {
            double relativeX = sealX / pdfImageView.getFitWidth() * 100;
            double relativeY = sealY / pdfImageView.getFitHeight() * 100;
            positionLabel.setText(String.format("位置: X=%.1f%%, Y=%.1f%%", relativeX, relativeY));
        }
    }

    private void initTable() {
        sealNameColumn.setCellValueFactory(new PropertyValueFactory<>("sealName"));
        projectNameColumn.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        contractNameColumn.setCellValueFactory(new PropertyValueFactory<>("contractName"));
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("stampPosition"));
        stampTimeColumn.setCellValueFactory(new PropertyValueFactory<>("stampTime"));
    }

    private void loadStamps() {
        try {
            List<ElectronicSealStamp> stamps = apiClient.get("/api/electronic-seal/stamps/my", List.class);
            stampList = FXCollections.observableArrayList(stamps);
            stampTable.setItems(stampList);
            totalCountLabel.setText("共 " + stampList.size() + " 条记录");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要盖章的PDF文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF文件", "*.pdf"));

        Stage stage = (Stage) previewPane.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedFile = file;
            selectedFilePath = file.getAbsolutePath();
            fileNameLabel.setText(file.getName());

            // 从文件名中提取合同名称（去掉.pdf扩展名）
            String fileName = file.getName();
            if (fileName.toLowerCase().endsWith(".pdf")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }
            contractNameLabel.setText(fileName);

            loadPdfPreview(file);
        }
    }

    private void loadPdfPreview(File file) {
        try {
            // 关闭之前的文档
            if (pdfDocument != null) {
                pdfDocument.close();
            }

            pdfDocument = PDDocument.load(file);
            totalPages = pdfDocument.getNumberOfPages();
            currentPage = 0;

            renderPdfPage(currentPage);
            updatePageLabel();
            updatePageNavigationButtons();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载PDF文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renderPdfPage(int pageIndex) {
        try {
            // 获取PDF页面的实际尺寸（以点为单位）
            PDRectangle pageSize = pdfDocument.getPage(pageIndex).getMediaBox();
            pdfPageWidth = pageSize.getWidth();
            pdfPageHeight = pageSize.getHeight();

            System.out.println("=== PDF页面信息 ===");
            System.out.println("PDF页面尺寸: " + pdfPageWidth + " x " + pdfPageHeight + " 点");

            // 使用指定DPI渲染PDF页面
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            BufferedImage bufferedImage = renderer.renderImageWithDPI(pageIndex, RENDER_DPI);

            // 转换为JavaFX Image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", baos);
            byte[] imageData = baos.toByteArray();
            Image image = new Image(new ByteArrayInputStream(imageData));

            // 计算渲染后的图像尺寸
            renderedWidth = image.getWidth();
            renderedHeight = image.getHeight();

            System.out.println("渲染图像尺寸: " + renderedWidth + " x " + renderedHeight + " 像素");
            System.out.println(
                    "理论渲染尺寸: " + (pdfPageWidth * RENDER_DPI / 72) + " x " + (pdfPageHeight * RENDER_DPI / 72) + " 像素");

            // 计算显示缩放比例（将渲染图像适配到预览区域）
            double maxWidth = previewPane.getWidth() - 20;
            double maxHeight = previewPane.getHeight() - 20;
            displayScale = Math.min(maxWidth / renderedWidth, maxHeight / renderedHeight);

            System.out.println("显示缩放比例: " + displayScale);

            // 计算显示尺寸
            double displayWidth = renderedWidth * displayScale;
            double displayHeight = renderedHeight * displayScale;

            // 居中显示PDF预览
            double pdfX = (previewPane.getWidth() - displayWidth) / 2;
            double pdfY = (previewPane.getHeight() - displayHeight) / 2;

            pdfImageView.setImage(image);
            pdfImageView.setFitWidth(displayWidth);
            pdfImageView.setFitHeight(displayHeight);
            pdfImageView.setPreserveRatio(true);
            pdfImageView.setLayoutX(pdfX);
            pdfImageView.setLayoutY(pdfY);

            // 更新印章显示尺寸
            if (sealImageView.getImage() != null) {
                updateSealDisplaySize();
                resetSealPosition();
            }

        } catch (Exception e) {
            e.printStackTrace();
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

    private void updatePageLabel() {
        pageLabel.setText(String.format("第 %d / %d 页", currentPage + 1, totalPages));
    }

    private void updatePageNavigationButtons() {
        prevPageButton.setDisable(currentPage <= 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1);
    }

    @FXML
    private void addStampPosition() {
        if (selectedFile == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先选择要盖章的PDF文件");
            return;
        }

        ElectronicSeal selectedSeal = sealComboBox.getValue();
        if (selectedSeal == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择电子章");
            return;
        }

        if (sealImageView.getImage() == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "电子章图片未加载");
            return;
        }

        try {
            double pdfX = pdfImageView.getLayoutX();
            double pdfY = pdfImageView.getLayoutY();

            double sealAbsoluteX = sealImageView.getLayoutX();
            double sealAbsoluteY = sealImageView.getLayoutY();

            double relativeX = sealAbsoluteX - pdfX;
            double relativeY = sealAbsoluteY - pdfY;

            double renderedX = relativeX / displayScale;
            double renderedY = relativeY / displayScale;

            double pdfStampX = renderedX * 72.0 / RENDER_DPI;
            double pdfStampY = renderedY * 72.0 / RENDER_DPI;

            double pdfYFromBottom = pdfPageHeight - pdfStampY - SEAL_SIZE_IN_PDF;

            int targetPage = currentPage + 1;

            stampPositions.removeIf(pos -> pos.pageNum == targetPage && pos.seal.getId().equals(selectedSeal.getId()));

            StampPosition position = new StampPosition(
                    selectedSeal,
                    targetPage,
                    pdfStampX,
                    pdfYFromBottom,
                    SEAL_SIZE_IN_PDF);

            stampPositions.add(position);

            rebuildStampListView();

            showAlert(Alert.AlertType.INFORMATION, "成功",
                    "印章位置已" + (stampPositions.size() > 0 ? "更新" : "添加") + "！\n\n" +
                            "印章：" + selectedSeal.getSealName() + "\n" +
                            "页码：第" + targetPage + "页\n" +
                            "位置：(" + String.format("%.1f", pdfStampX) + ", " + String.format("%.1f", pdfYFromBottom)
                            + ")\n\n" +
                            "当前共 " + stampPositions.size() + " 个印章位置");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "添加印章位置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void clearStampPositions() {
        stampPositions.clear();
        stampPositionListView.getItems().clear();
        showAlert(Alert.AlertType.INFORMATION, "成功", "所有印章位置已清除");
    }

    private void rebuildStampListView() {
        stampPositionListView.getItems().clear();
        for (StampPosition pos : stampPositions) {
            String positionText = String.format("[%s] 第%d页 - X:%.1f, Y:%.1f",
                    pos.seal.getSealName(),
                    pos.pageNum,
                    pos.x,
                    pos.y);
            stampPositionListView.getItems().add(positionText);
        }
    }

    @FXML
    private void applyToAllPages() {
        if (selectedFile == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先选择要盖章的PDF文件");
            return;
        }

        ElectronicSeal selectedSeal = sealComboBox.getValue();
        if (selectedSeal == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择电子章");
            return;
        }

        if (sealImageView.getImage() == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先拖动印章到指定位置");
            return;
        }

        try {
            double sealSize = SEAL_SIZE_IN_PDF;

            double pdfX = pdfImageView.getLayoutX();
            double pdfY = pdfImageView.getLayoutY();

            double sealAbsoluteX = sealImageView.getLayoutX();
            double sealAbsoluteY = sealImageView.getLayoutY();

            double relativeX = sealAbsoluteX - pdfX;
            double relativeY = sealAbsoluteY - pdfY;

            double renderedX = relativeX / displayScale;
            double renderedY = relativeY / displayScale;

            double pdfStampX = renderedX * 72.0 / RENDER_DPI;
            double pdfStampY = renderedY * 72.0 / RENDER_DPI;
            double pdfYFromBottom = pdfPageHeight - pdfStampY - sealSize;

            stampPositions.removeIf(pos -> pos.seal.getId().equals(selectedSeal.getId()));

            for (int page = 0; page < totalPages; page++) {
                StampPosition pos = new StampPosition(selectedSeal, page + 1, pdfStampX, pdfYFromBottom,
                        sealSize);
                stampPositions.add(pos);
            }

            rebuildStampListView();

            showAlert(Alert.AlertType.INFORMATION, "成功",
                    String.format("印章位置已应用到所有 %d 页！\n\n印章：%s\n位置：X=%.1f%%, Y=%.1f%%",
                            totalPages,
                            selectedSeal.getSealName(),
                            renderedX / renderedWidth * 100,
                            renderedY / renderedHeight * 100));

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "应用到所有页面失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void applyToPageRange() {
        if (selectedFile == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先选择要盖章的PDF文件");
            return;
        }

        ElectronicSeal selectedSeal = sealComboBox.getValue();
        if (selectedSeal == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择电子章");
            return;
        }

        if (sealImageView.getImage() == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先拖动印章到指定位置");
            return;
        }

        String pageRange = pageRangeField.getText();
        if (pageRange == null || pageRange.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请输入页码范围\n例如：1,3,5-10");
            return;
        }

        try {
            double sealSize = SEAL_SIZE_IN_PDF;

            double pdfX = pdfImageView.getLayoutX();
            double pdfY = pdfImageView.getLayoutY();

            double sealAbsoluteX = sealImageView.getLayoutX();
            double sealAbsoluteY = sealImageView.getLayoutY();

            double relativeX = sealAbsoluteX - pdfX;
            double relativeY = sealAbsoluteY - pdfY;

            double renderedX = relativeX / displayScale;
            double renderedY = relativeY / displayScale;

            double pdfStampX = renderedX * 72.0 / RENDER_DPI;
            double pdfStampY = renderedY * 72.0 / RENDER_DPI;
            double pdfYFromBottom = pdfPageHeight - pdfStampY - sealSize;

            List<Integer> pages = parsePageRange(pageRange, totalPages);
            if (pages.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "错误", "无效的页码范围，请检查输入格式");
                return;
            }

            stampPositions
                    .removeIf(pos -> pages.contains(pos.pageNum) && pos.seal.getId().equals(selectedSeal.getId()));

            for (int page : pages) {
                StampPosition pos = new StampPosition(selectedSeal, page, pdfStampX, pdfYFromBottom,
                        sealSize);
                stampPositions.add(pos);
            }

            rebuildStampListView();

            showAlert(Alert.AlertType.INFORMATION, "成功",
                    String.format("印章位置已应用到指定页面！\n\n页码：%s\n印章：%s\n位置：X=%.1f%%, Y=%.1f%%",
                            pageRange,
                            selectedSeal.getSealName(),
                            renderedX / renderedWidth * 100,
                            renderedY / renderedHeight * 100));

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "应用到指定页面失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Integer> parsePageRange(String pageRange, int totalPages) {
        List<Integer> pages = new ArrayList<>();

        if (pageRange == null || pageRange.trim().isEmpty()) {
            return pages;
        }

        String[] parts = pageRange.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        for (int i = Math.max(1, start); i <= Math.min(totalPages, end); i++) {
                            pages.add(i);
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            } else {
                try {
                    int pageNum = Integer.parseInt(part);
                    if (pageNum >= 1 && pageNum <= totalPages) {
                        pages.add(pageNum);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        return pages;
    }

    @FXML
    private void showPageSelectionDialog() {
        if (selectedFile == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先选择要盖章的PDF文件");
            return;
        }

        ElectronicSeal selectedSeal = sealComboBox.getValue();
        if (selectedSeal == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择电子章");
            return;
        }

        if (sealImageView.getImage() == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先拖动印章到指定位置");
            return;
        }

        Dialog<List<Integer>> dialog = new Dialog<>();
        dialog.setTitle("选择盖章页面");
        dialog.setHeaderText("请勾选需要盖章的页面（共 " + totalPages + " 页）");
        dialog.getDialogPane().setMinWidth(450);
        dialog.getDialogPane().setMinHeight(500);

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(15));

        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        Button selectAllBtn = new Button("全选");
        selectAllBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 15;");

        Button deselectAllBtn = new Button("取消全选");
        deselectAllBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 5 15;");

        Button selectOddBtn = new Button("选择奇数页");
        selectOddBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-padding: 5 15;");

        Button selectEvenBtn = new Button("选择偶数页");
        selectEvenBtn.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-padding: 5 15;");

        buttonBar.getChildren().addAll(selectAllBtn, deselectAllBtn, selectOddBtn, selectEvenBtn);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        GridPane pageGrid = new GridPane();
        pageGrid.setPadding(new Insets(10));
        pageGrid.setHgap(10);
        pageGrid.setVgap(8);

        List<CheckBox> checkBoxes = new ArrayList<>();
        int columns = 4;

        for (int i = 0; i < totalPages; i++) {
            CheckBox cb = new CheckBox("第 " + (i + 1) + " 页");
            cb.setUserData(i + 1);
            cb.setStyle("-fx-font-size: 13px;");

            for (StampPosition pos : stampPositions) {
                if (pos.pageNum == i + 1 && pos.seal.getId().equals(selectedSeal.getId())) {
                    cb.setSelected(true);
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    break;
                }
            }

            checkBoxes.add(cb);

            int row = i / columns;
            int col = i % columns;
            pageGrid.add(cb, col, row);
        }

        scrollPane.setContent(pageGrid);

        Label infoLabel = new Label("提示：绿色加粗表示已添加盖章位置的页面");
        infoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        mainContainer.getChildren().addAll(buttonBar, scrollPane, infoLabel);

        selectAllBtn.setOnAction(e -> {
            for (CheckBox cb : checkBoxes) {
                cb.setSelected(true);
            }
        });

        deselectAllBtn.setOnAction(e -> {
            for (CheckBox cb : checkBoxes) {
                cb.setSelected(false);
            }
        });

        selectOddBtn.setOnAction(e -> {
            for (int i = 0; i < checkBoxes.size(); i++) {
                checkBoxes.get(i).setSelected((i + 1) % 2 == 1);
            }
        });

        selectEvenBtn.setOnAction(e -> {
            for (int i = 0; i < checkBoxes.size(); i++) {
                checkBoxes.get(i).setSelected((i + 1) % 2 == 0);
            }
        });

        dialog.getDialogPane().setContent(mainContainer);

        ButtonType confirmButtonType = new ButtonType("确认选择", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                List<Integer> selectedPages = new ArrayList<>();
                for (CheckBox cb : checkBoxes) {
                    if (cb.isSelected()) {
                        selectedPages.add((Integer) cb.getUserData());
                    }
                }
                return selectedPages;
            }
            return null;
        });

        Optional<List<Integer>> result = dialog.showAndWait();

        result.ifPresent(selectedPages -> {
            if (selectedPages.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "提示", "未选择任何页面");
                return;
            }

            applyStampToSelectedPages(selectedPages, selectedSeal);
        });
    }

    private void applyStampToSelectedPages(List<Integer> selectedPages, ElectronicSeal selectedSeal) {
        try {
            double sealSize = SEAL_SIZE_IN_PDF;

            double pdfX = pdfImageView.getLayoutX();
            double pdfY = pdfImageView.getLayoutY();

            double sealAbsoluteX = sealImageView.getLayoutX();
            double sealAbsoluteY = sealImageView.getLayoutY();

            double relativeX = sealAbsoluteX - pdfX;
            double relativeY = sealAbsoluteY - pdfY;

            double renderedX = relativeX / displayScale;
            double renderedY = relativeY / displayScale;

            double pdfStampX = renderedX * 72.0 / RENDER_DPI;
            double pdfStampY = renderedY * 72.0 / RENDER_DPI;
            double pdfYFromBottom = pdfPageHeight - pdfStampY - sealSize;

            stampPositions.removeIf(
                    pos -> selectedPages.contains(pos.pageNum) && pos.seal.getId().equals(selectedSeal.getId()));

            for (int page : selectedPages) {
                StampPosition pos = new StampPosition(selectedSeal, page, pdfStampX, pdfYFromBottom,
                        sealSize);
                stampPositions.add(pos);
            }

            rebuildStampListView();

            showAlert(Alert.AlertType.INFORMATION, "成功",
                    String.format("印章位置应用成功！\n\n已应用 %d 个页面\n印章：%s\n位置：X=%.1f%%, Y=%.1f%%",
                            selectedPages.size(),
                            selectedSeal.getSealName(),
                            renderedX / renderedWidth * 100,
                            renderedY / renderedHeight * 100));

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "应用印章位置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void stampAndExport() {
        if (selectedFile == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先选择要盖章的PDF文件");
            return;
        }

        if (stampPositions.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先添加至少一个印章位置");
            return;
        }

        Project selectedProject = projectComboBox.getValue();
        if (selectedProject == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择项目");
            return;
        }

        for (StampPosition pos : stampPositions) {
            if (pos.seal.isUsbStorage()) {
                try {
                    Map<String, Object> usbInfo = apiClient.get("/api/usb-detection/detect", Map.class);
                    List<?> usbDrives = (List<?>) usbInfo.get("usbDrives");
                    if (usbDrives == null || usbDrives.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "U 盘未插入",
                                "电子章 \"" + pos.seal.getSealName() + "\" 存储在 U 盘中，请插入 U 盘。");
                        return;
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "U盘检测失败", "无法检测U盘: " + e.getMessage());
                    return;
                }
            }
        }

        try {
            String contractName = contractNameLabel.getText();
            String originalFilePath = selectedFilePath;

            List<Map<String, Object>> positionsData = new ArrayList<>();
            for (StampPosition pos : stampPositions) {
                Map<String, Object> posMap = new HashMap<>();
                posMap.put("sealId", pos.seal.getId());
                posMap.put("sealName", pos.seal.getSealName());
                posMap.put("sealCode", pos.seal.getSealType());
                posMap.put("pageNum", pos.pageNum);
                posMap.put("x", pos.x);
                posMap.put("y", pos.y);
                posMap.put("size", pos.size);
                posMap.put("sealImage", pos.seal.getSealImage());
                positionsData.add(posMap);
            }

            ObjectMapper mapper = new ObjectMapper();
            String positionsJson = mapper.writeValueAsString(positionsData);

            StampApproval approval = new StampApproval();
            approval.setContractName(contractName);
            approval.setOriginalContractPath(originalFilePath);
            approval.setStampPositions(positionsJson);
            approval.setSealId(stampPositions.get(0).seal.getId());
            approval.setSealName(stampPositions.stream()
                    .map(p -> p.seal.getSealName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            approval.setSealCode(stampPositions.get(0).seal.getSealType());
            approval.setSealPermission(stampPositions.get(0).seal.getDescription());
            approval.setProjectId(selectedProject.getId());
            approval.setProjectName(selectedProject.getProjectName());
            if (clientSession != null && clientSession.getCurrentUser() != null) {
                approval.setApplicantId(clientSession.getCurrentUser().getUserId());
                approval.setApplicantName(clientSession.getCurrentUser().getUsername());
            }
            approval.setStatus("pending");

            apiClient.post("/api/electronic-seal/stamp", approval, String.class);

            showAlert(Alert.AlertType.INFORMATION, "提交成功",
                    "盖章审批已提交！\n\n" +
                            "合同名称：" + contractName + "\n" +
                            "印章数量：" + stampPositions.size() + " 个\n" +
                            "项目：" + selectedProject.getProjectName() + "\n\n" +
                            "请等待经理审批通过后，在\"盖章审批管理\"中查看并导出盖章后的文件。");

            stampPositions.clear();
            stampPositionListView.getItems().clear();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "提交盖章审批失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Integer getCurrentUserId() {
        return 1;
    }

    private String getCurrentUserName() {
        return "当前用户";
    }

    private void saveStampRecord(ElectronicSeal seal, String outputPath, String position) {
        Project selectedProject = projectComboBox.getValue();
        String contractName = contractNameLabel.getText();

        if (selectedProject != null && contractName != null && !contractName.equals("未选择文件")) {
            ElectronicSealStamp stamp = new ElectronicSealStamp();
            stamp.setSealId(seal.getId());
            stamp.setSealName(seal.getSealName());
            stamp.setProjectId(selectedProject.getId());
            stamp.setProjectName(selectedProject.getProjectName());
            stamp.setContractName(contractName);
            stamp.setContractPath(outputPath);
            stamp.setStampPosition(position);
            stamp.setStampTime(new java.util.Date());

            try {
                apiClient.post("/api/electronic-seal/stamp", stamp, String.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void exportPdf() {
        if (selectedFile == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "请先选择文件");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("导出PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF文件", "*.pdf"));
            fileChooser.setInitialFileName(selectedFile.getName());

            Stage stage = (Stage) previewPane.getScene().getWindow();
            File outputFile = fileChooser.showSaveDialog(stage);

            if (outputFile != null) {
                java.nio.file.Files.copy(selectedFile.toPath(), outputFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                showAlert(Alert.AlertType.INFORMATION, "成功",
                        "PDF导出成功！\n保存位置: " + outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "PDF导出失败: " + e.getMessage());
            e.printStackTrace();
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
