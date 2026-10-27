package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.UserNotificationPreference;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public class NotificationPreferenceController {

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @FXML
    private CheckBox systemReceiveSystem;
    @FXML
    private CheckBox systemReceiveEmail;
    @FXML
    private CheckBox systemReceiveSms;
    @FXML
    private ComboBox<String> systemPriorityThreshold;

    @FXML
    private CheckBox businessReceiveSystem;
    @FXML
    private CheckBox businessReceiveEmail;
    @FXML
    private CheckBox businessReceiveSms;
    @FXML
    private ComboBox<String> businessPriorityThreshold;

    @FXML
    private CheckBox marketingReceiveSystem;
    @FXML
    private CheckBox marketingReceiveEmail;
    @FXML
    private CheckBox marketingReceiveSms;
    @FXML
    private ComboBox<String> marketingPriorityThreshold;

    @FXML
    private CheckBox quietModeCheckBox;
    @FXML
    private Spinner<Integer> quietStartHourSpinner;
    @FXML
    private Spinner<Integer> quietStartMinuteSpinner;
    @FXML
    private Spinner<Integer> quietEndHourSpinner;
    @FXML
    private Spinner<Integer> quietEndMinuteSpinner;

    private List<UserNotificationPreference> preferences;

    @FXML
    public void initialize() {
        initPriorityComboBoxes();
        initTimeSpinners();
        loadPreferences();
    }

    private void initPriorityComboBoxes() {
        String[] priorities = { "高", "中", "低" };
        systemPriorityThreshold.getItems().addAll(priorities);
        businessPriorityThreshold.getItems().addAll(priorities);
        marketingPriorityThreshold.getItems().addAll(priorities);
    }

    private void initTimeSpinners() {
        quietStartHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 22));
        quietStartMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        quietEndHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 7));
        quietEndMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
    }

    private void loadPreferences() {
        try {
            UserNotificationPreference[] arr = apiClient.get("/api/notifications/user/preferences", UserNotificationPreference[].class);
            preferences = new java.util.ArrayList<>();
            if (arr != null) {
                preferences.addAll(Arrays.asList(arr));
            }

            for (UserNotificationPreference pref : preferences) {
                switch (pref.getNotificationType()) {
                    case "SYSTEM":
                        systemReceiveSystem.setSelected(pref.getReceiveSystem());
                        systemReceiveEmail.setSelected(pref.getReceiveEmail());
                        systemReceiveSms.setSelected(pref.getReceiveSms());
                        systemPriorityThreshold.setValue(getPriorityText(pref.getPriorityThreshold()));
                        break;
                    case "BUSINESS":
                        businessReceiveSystem.setSelected(pref.getReceiveSystem());
                        businessReceiveEmail.setSelected(pref.getReceiveEmail());
                        businessReceiveSms.setSelected(pref.getReceiveSms());
                        businessPriorityThreshold.setValue(getPriorityText(pref.getPriorityThreshold()));
                        break;
                    case "MARKETING":
                        marketingReceiveSystem.setSelected(pref.getReceiveSystem());
                        marketingReceiveEmail.setSelected(pref.getReceiveEmail());
                        marketingReceiveSms.setSelected(pref.getReceiveSms());
                        marketingPriorityThreshold.setValue(getPriorityText(pref.getPriorityThreshold()));
                        break;
                }

                if (pref.getQuietMode() != null && pref.getQuietMode()) {
                    quietModeCheckBox.setSelected(true);
                    if (pref.getQuietStartTime() != null) {
                        quietStartHourSpinner.getValueFactory().setValue(pref.getQuietStartTime().getHour());
                        quietStartMinuteSpinner.getValueFactory().setValue(pref.getQuietStartTime().getMinute());
                    }
                    if (pref.getQuietEndTime() != null) {
                        quietEndHourSpinner.getValueFactory().setValue(pref.getQuietEndTime().getHour());
                        quietEndMinuteSpinner.getValueFactory().setValue(pref.getQuietEndTime().getMinute());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void savePreferences() {
        try {
            UserNotificationPreference systemPref = new UserNotificationPreference(null, "SYSTEM");
            systemPref.setReceiveSystem(systemReceiveSystem.isSelected());
            systemPref.setReceiveEmail(systemReceiveEmail.isSelected());
            systemPref.setReceiveSms(systemReceiveSms.isSelected());
            systemPref.setPriorityThreshold(getPriorityValue(systemPriorityThreshold.getValue()));
            systemPref.setQuietMode(quietModeCheckBox.isSelected());
            if (quietModeCheckBox.isSelected()) {
                systemPref.setQuietStartTime(
                        LocalTime.of(quietStartHourSpinner.getValue(), quietStartMinuteSpinner.getValue()));
                systemPref.setQuietEndTime(
                        LocalTime.of(quietEndHourSpinner.getValue(), quietEndMinuteSpinner.getValue()));
            }
            apiClient.put("/api/notifications/user/preferences", systemPref, Void.class);

            UserNotificationPreference businessPref = new UserNotificationPreference(null, "BUSINESS");
            businessPref.setReceiveSystem(businessReceiveSystem.isSelected());
            businessPref.setReceiveEmail(businessReceiveEmail.isSelected());
            businessPref.setReceiveSms(businessReceiveSms.isSelected());
            businessPref.setPriorityThreshold(getPriorityValue(businessPriorityThreshold.getValue()));
            businessPref.setQuietMode(quietModeCheckBox.isSelected());
            if (quietModeCheckBox.isSelected()) {
                businessPref.setQuietStartTime(
                        LocalTime.of(quietStartHourSpinner.getValue(), quietStartMinuteSpinner.getValue()));
                businessPref.setQuietEndTime(
                        LocalTime.of(quietEndHourSpinner.getValue(), quietEndMinuteSpinner.getValue()));
            }
            apiClient.put("/api/notifications/user/preferences", businessPref, Void.class);

            UserNotificationPreference marketingPref = new UserNotificationPreference(null, "MARKETING");
            marketingPref.setReceiveSystem(marketingReceiveSystem.isSelected());
            marketingPref.setReceiveEmail(marketingReceiveEmail.isSelected());
            marketingPref.setReceiveSms(marketingReceiveSms.isSelected());
            marketingPref.setPriorityThreshold(getPriorityValue(marketingPriorityThreshold.getValue()));
            marketingPref.setQuietMode(quietModeCheckBox.isSelected());
            if (quietModeCheckBox.isSelected()) {
                marketingPref.setQuietStartTime(
                        LocalTime.of(quietStartHourSpinner.getValue(), quietStartMinuteSpinner.getValue()));
                marketingPref.setQuietEndTime(
                        LocalTime.of(quietEndHourSpinner.getValue(), quietEndMinuteSpinner.getValue()));
            }
            apiClient.put("/api/notifications/user/preferences", marketingPref, Void.class);

            cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void cancel() {
        Stage stage = (Stage) systemReceiveSystem.getScene().getWindow();
        stage.close();
    }

    private String getPriorityText(String priority) {
        if (priority == null)
            return "低";
        switch (priority) {
            case "HIGH":
                return "高";
            case "MEDIUM":
                return "中";
            case "LOW":
            default:
                return "低";
        }
    }

    private String getPriorityValue(String text) {
        if (text == null)
            return "LOW";
        switch (text) {
            case "高":
                return "HIGH";
            case "中":
                return "MEDIUM";
            case "低":
            default:
                return "LOW";
        }
    }
}