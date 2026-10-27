package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.ApprovalRecord;
import com.ceams.model.entity.Receipt;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class FXApprovalHistoryDetailController implements Initializable {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private TextField receiptIdField;

    @FXML
    private TextField reporterNameField;

    @FXML
    private TextField projectNameField;

    @FXML
    private TextField amountField;

    @FXML
    private TableView<ApprovalRecord> historyTable;

    private Receipt receipt;
    private ObservableList<ApprovalRecord> historyList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        historyTable.setItems(historyList);
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
        displayReceiptInfo();
        loadApprovalHistory();
    }

    private void displayReceiptInfo() {
        if (receipt == null) {
            return;
        }
        if (receiptIdField != null) {
            receiptIdField.setText(String.valueOf(receipt.getId()));
        }
        if (reporterNameField != null) {
            reporterNameField.setText(receipt.getReporterName() != null ? receipt.getReporterName() : "");
        }
        if (projectNameField != null) {
            projectNameField.setText(receipt.getProjectName() != null ? receipt.getProjectName() : "");
        }
        if (amountField != null) {
            amountField.setText(receipt.getAmount() != null ? receipt.getAmount().toString() : "");
        }
    }

    private void loadApprovalHistory() {
        if (receipt != null && apiClient != null) {
            try {
                ApprovalRecord[] records = apiClient.get("/api/approval/history/" + receipt.getId(), ApprovalRecord[].class);
                historyList.clear();
                if (records != null) {
                    historyList.addAll(Arrays.asList(records));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) receiptIdField.getScene().getWindow();
        stage.close();
    }
}