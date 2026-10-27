package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.PageResult;
import com.ceams.model.Pagination;
import com.ceams.model.entity.Material;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import com.ceams.CeamsApplication;
import javafx.fxml.FXMLLoader;

public class FXMaterialController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    @FXML
    private TextField nameField;

    @FXML
    private javafx.scene.control.Pagination pagination;

    @FXML
    private TextField codeField;

    @FXML
    private TextField specField;

    // 添加材料所需字段
    @FXML
    private TextField materialNameField;

    @FXML
    private TextField materialCodeField;

    @FXML
    private TextField specificationField;

    @FXML
    private TextField unitField;

    @FXML
    private TextField currentPriceField;

    @FXML
    private TextField supplierIdField;

    @FXML
    private TextField categoryIdField;

    @FXML
    private TableView<Material> materialTable;

    @FXML
    private TableColumn<Material, Integer> idColumn;

    @FXML
    private TableColumn<Material, String> materialNameColumn;

    @FXML
    private TableColumn<Material, String> materialCodeColumn;

    @FXML
    private TableColumn<Material, String> specificationColumn;

    @FXML
    private TableColumn<Material, String> unitColumn;

    @FXML
    private TableColumn<Material, Double> currentPriceColumn;

    @FXML
    private TableColumn<Material, Integer> supplierIdColumn;

    @FXML
    private TableColumn<Material, Integer> categoryIdColumn;

    private int currentPage = 1;
    private int pageSize = 10;
    private String currentName = "";
    private String currentCode = "";
    private String currentSpec = "";

    // 用于编辑的材料对象
    private Material editingMaterial;

    /**
     * 设置RemoteApiClient
     */
    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * 设置ClientSession
     */
    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化表格列
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        materialNameColumn.setCellValueFactory(new PropertyValueFactory<>("materialName"));
        materialCodeColumn.setCellValueFactory(new PropertyValueFactory<>("materialCode"));
        specificationColumn.setCellValueFactory(new PropertyValueFactory<>("specification"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        currentPriceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        supplierIdColumn.setCellValueFactory(new PropertyValueFactory<>("supplierId"));
        categoryIdColumn.setCellValueFactory(new PropertyValueFactory<>("categoryId"));

        // 初始化分页控件
        initPagination();

        // 加载材料数据
        loadMaterials();
    }

    /**
     * 初始化分页控件
     */
    private void initPagination() {
        pagination.setPageFactory(this::createPage);
    }

    /**
     * 创建分页页面
     */
    private Node createPage(int pageIndex) {
        currentPage = pageIndex + 1;
        loadMaterials();
        return materialTable;
    }

    /**
     * 加载所有材料数据
     */
    private void loadMaterials() {
        try {
            // 构建查询参数
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("pageNum", String.valueOf(currentPage));
            queryParams.put("pageSize", String.valueOf(pageSize));
            
            if (!currentName.isEmpty()) {
                queryParams.put("materialName", currentName);
            }
            if (!currentCode.isEmpty()) {
                queryParams.put("materialCode", currentCode);
            }
            if (!currentSpec.isEmpty()) {
                queryParams.put("specification", currentSpec);
            }

            // 调用API获取材料列表
            PageResult<Material> pageResult = apiClient.get("/api/materials", queryParams, PageResult.class);

            if (pageResult != null && pageResult.getList() != null) {
                List<Material> materials = pageResult.getList();
                ObservableList<Material> data = FXCollections.observableArrayList(materials);
                materialTable.setItems(data);

                // 更新分页控件
                this.pagination.setCurrentPageIndex(currentPage - 1);
                this.pagination.setPageCount(pageResult.getPages());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("加载材料数据失败: " + e.getMessage());
            showError("加载失败", "无法加载材料数据: " + e.getMessage());
        }
    }

    /**
     * 刷新材料列表
     */
    @FXML
    private void refresh() {
        nameField.clear();
        codeField.clear();
        specField.clear();
        currentName = "";
        currentCode = "";
        currentSpec = "";
        currentPage = 1;
        loadMaterials();
    }

    /**
     * 查询材料
     */
    @FXML
    private void search() {
        // 获取查询参数
        currentName = nameField.getText().trim();
        currentCode = codeField.getText().trim();
        currentSpec = specField.getText().trim();
        currentPage = 1;

        // 调用服务层方法进行模糊查询
        loadMaterials();

        System.out.println("查询完成，条件：名称=" + currentName + "，编码=" + currentCode + "，规格=" + currentSpec);
    }

    /**
     * 添加材料
     */
    @FXML
    private void addMaterial() {
        try {
            System.out.println("添加材料");
            // 打开添加材料窗口
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/material/add.fxml"));
            loader.setControllerFactory(clazz -> {
                if (clazz == FXMaterialController.class) {
                    FXMaterialController controller = new FXMaterialController();
                    controller.setApiClient(this.apiClient);
                    controller.setClientSession(this.clientSession);
                    return controller;
                }
                return null;
            });
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("添加材料");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            // 刷新材料列表
            loadMaterials();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("打开添加材料窗口时发生错误: " + e.getMessage());
            showError("打开窗口失败", "无法打开添加材料窗口: " + e.getMessage());
        }
    }

    /**
     * 编辑材料
     */
    @FXML
    private void editMaterial() {
        try {
            System.out.println("编辑材料");
            // 获取选中的材料
            Material selectedMaterial = materialTable.getSelectionModel().getSelectedItem();
            if (selectedMaterial == null) {
                System.out.println("请选择要编辑的材料");
                return;
            }

            // 打开编辑材料窗口
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/material/edit.fxml"));
            loader.setControllerFactory(clazz -> {
                if (clazz == FXMaterialController.class) {
                    FXMaterialController controller = new FXMaterialController();
                    controller.setApiClient(this.apiClient);
                    controller.setClientSession(this.clientSession);
                    return controller;
                }
                return null;
            });
            Parent root = loader.load();

            // 获取控制器并设置编辑数据
            FXMaterialController controller = loader.getController();
            controller.setEditingMaterial(selectedMaterial);

            // 显示窗口
            Stage stage = new Stage();
            stage.setTitle("编辑材料");
            stage.initModality(Modality.WINDOW_MODAL);
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // 刷新材料列表
            loadMaterials();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("打开编辑材料窗口时发生错误: " + e.getMessage());
            showError("打开窗口失败", "无法打开编辑材料窗口: " + e.getMessage());
        }
    }

    /**
     * 删除材料
     */
    @FXML
    private void deleteMaterial() {
        // 这里可以实现删除逻辑
        Material selectedMaterial = materialTable.getSelectionModel().getSelectedItem();
        if (selectedMaterial != null) {
            // 弹出确认对话框
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText("删除材料");
            alert.setContentText("确定要删除ID为" + selectedMaterial.getId() + "的材料吗？此操作不可恢复。");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    System.out.println("删除材料: " + selectedMaterial.getId());
                    apiClient.delete("/api/materials/" + selectedMaterial.getId(), Void.class);
                    // 刷新材料列表
                    loadMaterials();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("删除材料失败: " + e.getMessage());
                    showError("删除失败", "无法删除材料: " + e.getMessage());
                }
            }
        } else {
            System.out.println("请选择要删除的材料");
        }
    }

    /**
     * 查看材料报表
     */
    @FXML
    private void viewReport() {
        try {
            System.out.println("查看材料报表");
            // 打开材料报表窗口
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/material/report.fxml"));
            loader.setControllerFactory(clazz -> {
                if (clazz == FXMaterialController.class) {
                    FXMaterialController controller = new FXMaterialController();
                    controller.setApiClient(this.apiClient);
                    controller.setClientSession(this.clientSession);
                    return controller;
                }
                return null;
            });
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("材料报表");
            stage.initModality(Modality.WINDOW_MODAL);
            WindowUtils.setStageIcon(stage);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("打开材料报表窗口时发生错误: " + e.getMessage());
            showError("打开窗口失败", "无法打开材料报表窗口: " + e.getMessage());
        }
    }

    /**
     * 生成报表
     */
    @FXML
    private void generateReport() {
        // 这里可以实现生成报表逻辑
        System.out.println("生成材料报表");
    }

    /**
     * 导出PDF
     */
    @FXML
    private void exportPDF() {
        // 这里可以实现导出PDF逻辑
        System.out.println("导出材料报表PDF");
    }

    /**
     * 导出Excel
     */
    @FXML
    private void exportExcel() {
        // 这里可以实现导出Excel逻辑
        System.out.println("导出材料报表Excel");
    }

    /**
     * 保存材料
     */
    @FXML
    private void save() {
        try {
            System.out.println("保存材料");
            // 创建新的材料对象
            Material material = new Material();
            material.setMaterialName(materialNameField.getText());
            material.setMaterialCode(materialCodeField.getText());
            material.setSpecification(specificationField.getText());
            material.setUnit(unitField.getText());
            material.setCurrentPrice(new java.math.BigDecimal(currentPriceField.getText()));
            material.setSupplierId(Integer.parseInt(supplierIdField.getText()));
            material.setCategoryId(Integer.parseInt(categoryIdField.getText()));
            // 通过API保存到后端
            apiClient.post("/api/materials", material, Material.class);
            System.out.println("材料保存成功");
            // 关闭当前窗口
            Stage stage = (Stage) materialNameField.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("保存材料失败: " + e.getMessage());
            showError("保存失败", "无法保存材料: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("保存材料时发生错误: " + e.getMessage());
            showError("保存错误", "保存材料时发生错误: " + e.getMessage());
        }
    }

    /**
     * 取消添加
     */
    @FXML
    private void cancel() {
        // 关闭当前窗口
        Stage stage = (Stage) materialNameField.getScene().getWindow();
        stage.close();
    }

    /**
     * 设置要编辑的材料
     */
    public void setEditingMaterial(Material material) {
        this.editingMaterial = material;
        // 填充表单数据
        if (material != null) {
            materialNameField.setText(material.getMaterialName());
            materialCodeField.setText(material.getMaterialCode());
            specificationField.setText(material.getSpecification());
            unitField.setText(material.getUnit());
            currentPriceField.setText(material.getCurrentPrice().toString());
            supplierIdField.setText(String.valueOf(material.getSupplierId()));
            categoryIdField.setText(String.valueOf(material.getCategoryId()));
        }
    }

    /**
     * 保存材料（编辑模式）
     */
    @FXML
    private void saveMaterial() {
        try {
            System.out.println("保存材料");
            if (editingMaterial != null) {
                // 更新现有材料
                editingMaterial.setMaterialName(materialNameField.getText());
                editingMaterial.setMaterialCode(materialCodeField.getText());
                editingMaterial.setSpecification(specificationField.getText());
                editingMaterial.setUnit(unitField.getText());
                editingMaterial.setCurrentPrice(new java.math.BigDecimal(currentPriceField.getText()));
                editingMaterial.setSupplierId(Integer.parseInt(supplierIdField.getText()));
                editingMaterial.setCategoryId(Integer.parseInt(categoryIdField.getText()));
                // 通过API更新到后端
                apiClient.put("/api/materials/" + editingMaterial.getId(), editingMaterial, Material.class);
                System.out.println("材料更新成功");
            } else {
                // 保存新材料
                save();
            }
            // 关闭当前窗口
            Stage stage = (Stage) materialNameField.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("保存材料时发生错误: " + e.getMessage());
            showError("保存失败", "无法保存材料: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("保存材料时发生错误: " + e.getMessage());
            showError("保存错误", "保存材料时发生错误: " + e.getMessage());
        }
    }

    /**
     * 取消编辑
     */
    @FXML
    private void cancelEdit() {
        // 关闭当前窗口
        Stage stage = (Stage) materialNameField.getScene().getWindow();
        stage.close();
    }

    /**
     * 返回
     */
    @FXML
    private void back() {
        // 关闭当前窗口
        Stage stage = (Stage) materialTable.getScene().getWindow();
        stage.close();
    }

    /**
     * 显示错误信息
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}