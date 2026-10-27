package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Project;
import com.ceams.model.entity.Subcontractor;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.ceams.ui.util.WindowUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FXSubcontractorProjectController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private Label subcontractorNameLabel;

    @FXML
    private ListView<Project> allProjectsListView;

    @FXML
    private ListView<Project> selectedProjectsListView;

    private Subcontractor subcontractor;
    private List<Project> allProjects;
    private List<Integer> selectedProjectIds;

    public void setSubcontractor(Subcontractor subcontractor) {
        this.subcontractor = subcontractor;
        if (subcontractorNameLabel != null) {
            subcontractorNameLabel.setText("分包商: " + subcontractor.getName());
        }
        loadProjects();
    }

    @FXML
    public void initialize() {
        // 初始化在 setSubcontractor 中完成
    }

    private void loadProjects() {
        if (subcontractor == null || apiClient == null) {
            return;
        }

        try {
            Project[] projects = apiClient.get("/api/subcontractors/projects/all", Project[].class);
            allProjects = Arrays.asList(projects);

            Integer[] ids = apiClient.get("/api/subcontractors/" + subcontractor.getId() + "/projects", Integer[].class);
            selectedProjectIds = new java.util.ArrayList<>();
            if (ids != null) {
                selectedProjectIds.addAll(Arrays.asList(ids));
            }

            updateListViews();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateListViews() {
        if (allProjects == null || selectedProjectIds == null) {
            return;
        }

        List<Project> selectedProjects = allProjects.stream()
                .filter(p -> selectedProjectIds.contains(p.getId()))
                .collect(Collectors.toList());

        List<Project> availableProjects = allProjects.stream()
                .filter(p -> !selectedProjectIds.contains(p.getId()))
                .collect(Collectors.toList());

        allProjectsListView.setItems(FXCollections.observableArrayList(availableProjects));
        selectedProjectsListView.setItems(FXCollections.observableArrayList(selectedProjects));
    }

    @FXML
    private void addProject() {
        Project selected = allProjectsListView.getSelectionModel().getSelectedItem();
        if (selected != null && !selectedProjectIds.contains(selected.getId())) {
            selectedProjectIds.add(selected.getId());
            updateListViews();
        }
    }

    @FXML
    private void removeProject() {
        Project selected = selectedProjectsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedProjectIds.remove(selected.getId());
            updateListViews();
        }
    }

    @FXML
    private void save() {
        if (subcontractor == null || apiClient == null) {
            return;
        }

        try {
            apiClient.put("/api/subcontractors/" + subcontractor.getId() + "/projects", selectedProjectIds, Void.class);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("成功");
            alert.setHeaderText(null);
            alert.setContentText("项目分配保存成功");
            alert.showAndWait();

            Stage stage = (Stage) allProjectsListView.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("保存失败: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void cancel() {
        Stage stage = (Stage) allProjectsListView.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void openContractWindow() {
        if (subcontractor == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/subcontractor/contract.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            try {
                java.lang.reflect.Method setApiClient = controller.getClass().getMethod("setApiClient", RemoteApiClient.class);
                setApiClient.invoke(controller, apiClient);
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {}
            try {
                java.lang.reflect.Method setClientSession = controller.getClass().getMethod("setClientSession", ClientSession.class);
                setClientSession.invoke(controller, clientSession);
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {}
            try {
                java.lang.reflect.Method setSubcontractor = controller.getClass().getMethod("setSubcontractor", Subcontractor.class);
                setSubcontractor.invoke(controller, subcontractor);
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {}

            Stage stage = new Stage();
            stage.setTitle("分包商合同管理 - " + subcontractor.getName());
            stage.setScene(new Scene(root));
            WindowUtils.setStageIcon(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}