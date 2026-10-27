package com.ceams.ui.controller;

import com.ceams.client.ClientSession;
import com.ceams.client.RemoteApiClient;
import com.ceams.model.entity.Account;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class FXAccountVisualizationController implements Initializable {

    @FXML
    private Label totalIncomeLabel;

    @FXML
    private Label totalExpenseLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private Label transactionCountLabel;

    @FXML
    private Label changeRateLabel;

    @FXML
    private ComboBox<String> timeRangeComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private ComboBox<String> projectComboBox;

    @FXML
    private LineChart<String, Number> trendChart;

    @FXML
    private BarChart<String, Number> categoryBarChart;

    @FXML
    private PieChart categoryPieChart;

    @FXML
    private TableView<Account> transactionTable;

    @FXML
    private TableColumn<Account, String> dateColumn;

    @FXML
    private TableColumn<Account, String> typeColumn;

    @FXML
    private TableColumn<Account, String> categoryColumn;

    @FXML
    private TableColumn<Account, String> projectColumn;

    @FXML
    private TableColumn<Account, String> amountColumn;

    @FXML
    private TableColumn<Account, String> runningBalanceColumn;

    @FXML
    private TableColumn<Account, String> percentColumn;

    @FXML
    private TableColumn<Account, String> descriptionColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Pagination pagination;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private HBox summaryCardsBox;

    @FXML
    private VBox chartContainer;

    @FXML
    private VBox topIncomeBox;

    @FXML
    private VBox topExpenseBox;

    private RemoteApiClient apiClient;
    private ClientSession clientSession;

    private ObservableList<Account> accountList = FXCollections.observableArrayList();
    private List<Account> allAccounts = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
    private SimpleDateFormat dayFormat = new SimpleDateFormat("MM-dd");

    private BigDecimal totalPeriodIncome = BigDecimal.ZERO;
    private BigDecimal totalPeriodExpense = BigDecimal.ZERO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTimeRangeSelector();
        initCharts();
        initTable();
        initTopLists();
        loadData();
        applyModernStyles();
    }

    public void setApiClient(RemoteApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    private void initTimeRangeSelector() {
        timeRangeComboBox.getItems().addAll("今日", "本周", "本月", "本季度", "本年", "自定义");
        timeRangeComboBox.setValue("本月");
        timeRangeComboBox.setOnAction(e -> onTimeRangeChanged());

        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.withDayOfMonth(1));
        endDatePicker.setValue(now);

        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("自定义".equals(timeRangeComboBox.getValue())) {
                loadData();
            }
        });

        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("自定义".equals(timeRangeComboBox.getValue())) {
                loadData();
            }
        });

        categoryComboBox.getItems().addAll("全部", "收入", "支出");
        categoryComboBox.setValue("全部");
        categoryComboBox.setOnAction(e -> loadData());
    }

    private void initCharts() {
        trendChart.setAnimated(true);
        trendChart.setLegendVisible(true);
        trendChart.setCreateSymbols(true);
        trendChart.getXAxis().setLabel("时间");
        trendChart.getYAxis().setLabel("金额 (元)");

        categoryBarChart.setAnimated(true);
        categoryBarChart.setLegendVisible(false);
        CategoryAxis xAxisBar = (CategoryAxis) categoryBarChart.getXAxis();
        xAxisBar.setLabel("分类");
        xAxisBar.setTickLabelRotation(45);
        categoryBarChart.getYAxis().setLabel("金额 (元)");

        categoryPieChart.setAnimated(true);
        categoryPieChart.setLabelsVisible(true);
        categoryPieChart.setLegendVisible(true);
    }

    private void initTable() {
        dateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getTransactionDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        dateFormat.format(cellData.getValue().getTransactionDate()));
            }
            return new javafx.beans.property.SimpleStringProperty("-");
        });

        typeColumn.setCellValueFactory(cellData -> {
            String type = cellData.getValue().getType();
            if ("income".equals(type)) {
                return new javafx.beans.property.SimpleStringProperty("收入");
            } else if ("expense".equals(type)) {
                return new javafx.beans.property.SimpleStringProperty("支出");
            }
            return new javafx.beans.property.SimpleStringProperty(type != null ? type : "-");
        });
        typeColumn.setCellFactory(col -> {
            return new TableCell<Account, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        if ("收入".equals(item)) {
                            setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                        } else if ("支出".equals(item)) {
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        }
                    }
                }
            };
        });

        categoryColumn.setCellValueFactory(cellData -> {
            String category = getCategoryName(cellData.getValue());
            return new javafx.beans.property.SimpleStringProperty(category);
        });

        projectColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getProjectName() != null ? cellData.getValue().getProjectName() : "-"));
        projectColumn.setCellFactory(col -> {
            return new TableCell<Account, String>() {
                private Tooltip tooltip;

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                        this.tooltip = null;
                    } else {
                        String displayText = truncateString(item, 10);
                        setText(displayText);
                        this.tooltip = new Tooltip(item);
                        this.tooltip.setStyle("-fx-font-size: 12px;");
                        Tooltip.install(this, this.tooltip);
                    }
                }
            };
        });

        amountColumn.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getAmount();
            String type = cellData.getValue().getType();
            String prefix = "expense".equals(type) ? "-" : "+";
            return new javafx.beans.property.SimpleStringProperty(
                    prefix + (amount != null ? formatCurrency(amount) : "¥0.00"));
        });
        amountColumn.setCellFactory(col -> {
            return new TableCell<Account, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        Account account = getTableRow().getItem();
                        if (account != null) {
                            if ("income".equals(account.getType())) {
                                setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            }
                        }
                    }
                }
            };
        });

        runningBalanceColumn.setCellValueFactory(cellData -> {
            BigDecimal balance = cellData.getValue().getRunningBalance();
            return new javafx.beans.property.SimpleStringProperty(
                    balance != null ? formatCurrency(balance) : "-");
        });

        percentColumn.setCellValueFactory(cellData -> {
            BigDecimal percent = cellData.getValue().getPeriodPercent();
            if (percent != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        percent.setScale(1, RoundingMode.HALF_UP) + "%");
            }
            return new javafx.beans.property.SimpleStringProperty("-");
        });

        descriptionColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getDescription() != null ? cellData.getValue().getDescription() : "-"));

        transactionTable.setRowFactory(tv -> {
            TableRow<Account> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    if ("income".equals(newVal.getType())) {
                        row.setStyle("-fx-background-color: #fff5f5;");
                    } else {
                        row.setStyle("-fx-background-color: #f0fff4;");
                    }
                }
            });
            return row;
        });

        transactionTable.setItems(accountList);

        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            updateTablePage(newIndex.intValue());
        });
    }

    private void updateTablePage(int pageIndex) {
        int pageSize = 20;
        int fromIndex = pageIndex * pageSize;
        if (fromIndex >= allAccounts.size()) {
            return;
        }
        int toIndex = Math.min(fromIndex + pageSize, allAccounts.size());
        ObservableList<Account> pageData = FXCollections.observableArrayList(
                allAccounts.subList(fromIndex, toIndex));
        transactionTable.setItems(pageData);
    }

    private void initTopLists() {
        topIncomeBox.getChildren().clear();
        topExpenseBox.getChildren().clear();
    }

    private String getCategoryName(Account account) {
        if (account.getProjectName() != null && !account.getProjectName().isEmpty()) {
            return account.getProjectName();
        }
        if (account.getContractName() != null && !account.getContractName().isEmpty()) {
            return account.getContractName();
        }
        if (account.getPaymentType() != null && !account.getPaymentType().isEmpty()) {
            return account.getPaymentType();
        }
        return "未分类";
    }

    private void applyModernStyles() {
        String cardStyle = "-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-padding: 20;";

        summaryCardsBox.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle(cardStyle);
            }
        });
    }

    @FXML
    private void onTimeRangeChanged() {
        String selected = timeRangeComboBox.getValue();
        LocalDate now = LocalDate.now();

        switch (selected) {
            case "今日":
                startDatePicker.setValue(now);
                endDatePicker.setValue(now);
                break;
            case "本周":
                startDatePicker.setValue(now.minusDays(now.getDayOfWeek().getValue() - 1));
                endDatePicker.setValue(now);
                break;
            case "本月":
                startDatePicker.setValue(now.withDayOfMonth(1));
                endDatePicker.setValue(now);
                break;
            case "本季度":
                int quarter = (now.getMonthValue() - 1) / 3;
                startDatePicker.setValue(now.withMonth(quarter * 3 + 1).withDayOfMonth(1));
                endDatePicker.setValue(now);
                break;
            case "本年":
                startDatePicker.setValue(now.withDayOfYear(1));
                endDatePicker.setValue(now);
                break;
            case "自定义":
                return;
        }

        loadData();
    }

    private void loadData() {
        showLoading(true);

        new Thread(() -> {
            try {
                Date startDate = startDatePicker.getValue() != null ? java.sql.Date.valueOf(startDatePicker.getValue())
                        : null;
                Date endDate = endDatePicker.getValue() != null ? java.sql.Date.valueOf(endDatePicker.getValue())
                        : null;

                String categoryFilter = categoryComboBox.getValue();
                String typeFilter = "全部".equals(categoryFilter) ? null
                        : ("收入".equals(categoryFilter) ? "income" : "expense");

                // 构建查询参数
                Map<String, String> params = new HashMap<>();
                params.put("pageNum", "1");
                params.put("pageSize", "10000");
                if (typeFilter != null) {
                    params.put("type", typeFilter);
                }
                if (startDate != null) {
                    params.put("startDate", dateFormat.format(startDate));
                }
                if (endDate != null) {
                    params.put("endDate", dateFormat.format(endDate));
                }

                // 调用API获取账目列表
                Account[] accountsArray = apiClient.get("/api/accounts", params, Account[].class);
                List<Account> accounts = accountsArray != null ? Arrays.asList(accountsArray) : new ArrayList<>();

                accounts.sort(Comparator.comparing(Account::getTransactionDate,
                        Comparator.nullsLast(Comparator.naturalOrder())));

                Platform.runLater(() -> {
                    allAccounts.clear();
                    allAccounts.addAll(accounts);

                    calculateRunningBalance();

                    totalPeriodIncome = BigDecimal.ZERO;
                    totalPeriodExpense = BigDecimal.ZERO;
                    for (Account account : allAccounts) {
                        BigDecimal amount = account.getAmount() != null ? account.getAmount() : BigDecimal.ZERO;
                        if ("income".equals(account.getType())) {
                            totalPeriodIncome = totalPeriodIncome.add(amount);
                        } else {
                            totalPeriodExpense = totalPeriodExpense.add(amount);
                        }
                    }

                    calculatePeriodPercent();

                    int pageSize = 20;
                    int pageCount = (int) Math.ceil((double) allAccounts.size() / pageSize);
                    pageCount = Math.max(pageCount, 1);
                    pagination.setPageCount(pageCount);
                    pagination.setCurrentPageIndex(0);

                    updateTablePage(0);
                    updateSummaryCards();
                    updateCharts();
                    updateTopLists();
                    showLoading(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("加载数据失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private void calculateRunningBalance() {
        BigDecimal runningBalance = BigDecimal.ZERO;
        for (Account account : allAccounts) {
            BigDecimal amount = account.getAmount() != null ? account.getAmount() : BigDecimal.ZERO;
            if ("income".equals(account.getType())) {
                runningBalance = runningBalance.add(amount);
            } else {
                runningBalance = runningBalance.subtract(amount);
            }
            account.setRunningBalance(runningBalance);
        }
    }

    private void calculatePeriodPercent() {
        BigDecimal totalAmount = totalPeriodIncome.add(totalPeriodExpense);
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        for (Account account : allAccounts) {
            BigDecimal amount = account.getAmount() != null ? account.getAmount() : BigDecimal.ZERO;
            BigDecimal percent = amount.multiply(new BigDecimal("100")).divide(totalAmount, 2, RoundingMode.HALF_UP);
            account.setPeriodPercent(percent);
        }
    }

    private void updateSummaryCards() {
        BigDecimal balance = totalPeriodIncome.subtract(totalPeriodExpense);

        totalIncomeLabel.setText(formatCurrency(totalPeriodIncome));
        totalExpenseLabel.setText(formatCurrency(totalPeriodExpense));
        balanceLabel.setText(formatCurrency(balance));
        transactionCountLabel.setText(String.valueOf(allAccounts.size()));

        // 设置顶部卡片颜色：收入红色，支出绿色
        totalIncomeLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 20px; -fx-font-weight: bold;");
        totalExpenseLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 20px; -fx-font-weight: bold;");

        if (balance.compareTo(BigDecimal.ZERO) >= 0) {
            balanceLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else {
            balanceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 20px; -fx-font-weight: bold;");
        }

        BigDecimal changeRate = BigDecimal.ZERO;
        if (totalPeriodIncome.compareTo(BigDecimal.ZERO) > 0) {
            changeRate = balance.multiply(new BigDecimal("100")).divide(totalPeriodIncome, 1, RoundingMode.HALF_UP);
        }
        changeRateLabel.setText(changeRate.setScale(1, RoundingMode.HALF_UP) + "%");
        if (changeRate.compareTo(BigDecimal.ZERO) >= 0) {
            changeRateLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        } else {
            changeRateLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }

    private void updateCharts() {
        updateTrendChart();
        updateCategoryCharts();
    }

    private void updateTrendChart() {
        trendChart.getData().clear();

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("收入");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("支出");

        XYChart.Series<String, Number> balanceSeries = new XYChart.Series<>();
        balanceSeries.setName("余额");

        Map<String, BigDecimal> incomeByDay = new TreeMap<>();
        Map<String, BigDecimal> expenseByDay = new TreeMap<>();
        Map<String, BigDecimal> balanceByDay = new TreeMap<>();

        BigDecimal runningIncome = BigDecimal.ZERO;
        BigDecimal runningExpense = BigDecimal.ZERO;

        for (Account account : allAccounts) {
            String dayKey = account.getTransactionDate() != null ? dayFormat.format(account.getTransactionDate())
                    : "未知";
            BigDecimal amount = account.getAmount() != null ? account.getAmount() : BigDecimal.ZERO;

            if ("income".equals(account.getType())) {
                runningIncome = runningIncome.add(amount);
                incomeByDay.merge(dayKey, amount, BigDecimal::add);
            } else {
                runningExpense = runningExpense.add(amount);
                expenseByDay.merge(dayKey, amount, BigDecimal::add);
            }
            balanceByDay.put(dayKey, runningIncome.subtract(runningExpense));
        }

        Set<String> allDays = new TreeSet<>();
        allDays.addAll(incomeByDay.keySet());
        allDays.addAll(expenseByDay.keySet());
        allDays.addAll(balanceByDay.keySet());

        for (String day : allDays) {
            incomeSeries.getData().add(new XYChart.Data<>(day,
                    incomeByDay.getOrDefault(day, BigDecimal.ZERO)));
            expenseSeries.getData().add(new XYChart.Data<>(day,
                    expenseByDay.getOrDefault(day, BigDecimal.ZERO)));
            balanceSeries.getData().add(new XYChart.Data<>(day,
                    balanceByDay.getOrDefault(day, BigDecimal.ZERO)));
        }

        trendChart.getData().addAll(incomeSeries, expenseSeries, balanceSeries);

        // 设置趋势图数据点颜色
        Platform.runLater(() -> {
            for (XYChart.Series<String, Number> series : trendChart.getData()) {
                for (XYChart.Data<String, Number> data : series.getData()) {
                    Node node = data.getNode();
                    if (node != null) {
                        if ("收入".equals(series.getName())) {
                            node.setStyle("-fx-background-color: #c0392b;");
                        } else if ("支出".equals(series.getName())) {
                            node.setStyle("-fx-background-color: #27ae60;");
                        } else {
                            node.setStyle("-fx-background-color: #3498db;");
                        }
                    }
                }
            }
        });
    }

    private void updateCategoryCharts() {
        categoryBarChart.getData().clear();
        categoryPieChart.getData().clear();

        Map<String, BigDecimal> incomeByCategory = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();

        for (Account account : allAccounts) {
            String category = getCategoryName(account);
            BigDecimal amount = account.getAmount() != null ? account.getAmount() : BigDecimal.ZERO;

            if ("income".equals(account.getType())) {
                incomeByCategory.merge(category, amount, BigDecimal::add);
            } else {
                expenseByCategory.merge(category, amount, BigDecimal::add);
            }
        }

        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.setName("金额");

        List<Map.Entry<String, BigDecimal>> sortedExpense = expenseByCategory.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(8)
                .collect(Collectors.toList());

        // 保存完整名称列表，用于后面设置Tooltip
        List<String> fullNames = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : sortedExpense) {
            String fullName = entry.getKey();
            String shortName = truncateString(fullName, 8);

            // 添加柱状图数据
            XYChart.Data<String, Number> barData = new XYChart.Data<>(shortName, entry.getValue());
            barSeries.getData().add(barData);

            // 添加饼图数据
            PieChart.Data pieData = new PieChart.Data(
                    shortName + " (" + formatCurrency(entry.getValue()) + ")",
                    entry.getValue().doubleValue());
            categoryPieChart.getData().add(pieData);

            // 保存完整名称
            fullNames.add(fullName);
        }

        categoryBarChart.getData().add(barSeries);

        // 设置柱状图颜色和Tooltip
        Platform.runLater(() -> {
            for (int i = 0; i < barSeries.getData().size(); i++) {
                XYChart.Data<String, Number> data = barSeries.getData().get(i);
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-bar-fill: #27ae60;");
                    // 设置Tooltip显示完整项目名称
                    BigDecimal amount = (data.getYValue() instanceof BigDecimal)
                            ? (BigDecimal) data.getYValue()
                            : BigDecimal.valueOf(data.getYValue().doubleValue());
                    Tooltip barTooltip = new Tooltip(
                            "项目：" + fullNames.get(i) + "\n" +
                                    "金额：" + formatCurrency(amount));
                    Tooltip.install(node, barTooltip);
                }
            }
        });

        // 设置饼图颜色和Tooltip
        String[] colors = { "#27ae60", "#2ecc71", "#1abc9c", "#16a085", "#2980b9", "#3498db", "#9b59b6", "#8e44ad" };
        for (int i = 0; i < categoryPieChart.getData().size(); i++) {
            PieChart.Data data = categoryPieChart.getData().get(i);
            int colorIndex = i % colors.length;
            data.getNode().setStyle("-fx-pie-color: " + colors[colorIndex] + ";");

            // 设置Tooltip显示完整项目名称
            Tooltip pieTooltip = new Tooltip(
                    "项目：" + fullNames.get(i) + "\n" +
                            "金额：" + formatCurrency(BigDecimal.valueOf(data.getPieValue())));
            Tooltip.install(data.getNode(), pieTooltip);
        }
    }

    private String truncateString(String s, int maxLength) {
        if (s == null || s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, maxLength) + "...";
    }

    private void updateTopLists() {
        topIncomeBox.getChildren().clear();
        topExpenseBox.getChildren().clear();

        List<Map.Entry<String, BigDecimal>> topIncome = allAccounts.stream()
                .filter(a -> "income".equals(a.getType()))
                .collect(Collectors.groupingBy(
                        this::getCategoryName,
                        Collectors.reducing(BigDecimal.ZERO, Account::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        List<Map.Entry<String, BigDecimal>> topExpense = allAccounts.stream()
                .filter(a -> "expense".equals(a.getType()))
                .collect(Collectors.groupingBy(
                        this::getCategoryName,
                        Collectors.reducing(BigDecimal.ZERO, Account::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        BigDecimal maxIncome = topIncome.isEmpty() ? BigDecimal.ONE : topIncome.get(0).getValue();
        BigDecimal maxExpense = topExpense.isEmpty() ? BigDecimal.ONE : topExpense.get(0).getValue();

        for (int i = 0; i < topIncome.size(); i++) {
            Map.Entry<String, BigDecimal> entry = topIncome.get(i);
            HBox row = createTopListRow(i + 1, entry.getKey(), entry.getValue(), maxIncome, true);
            topIncomeBox.getChildren().add(row);
        }

        for (int i = 0; i < topExpense.size(); i++) {
            Map.Entry<String, BigDecimal> entry = topExpense.get(i);
            HBox row = createTopListRow(i + 1, entry.getKey(), entry.getValue(), maxExpense, false);
            topExpenseBox.getChildren().add(row);
        }

        if (topIncome.isEmpty()) {
            topIncomeBox.getChildren().add(new Label("暂无收入数据"));
        }
        if (topExpense.isEmpty()) {
            topExpenseBox.getChildren().add(new Label("暂无支出数据"));
        }
    }

    private HBox createTopListRow(int rank, String name, BigDecimal amount, BigDecimal maxAmount, boolean isIncome) {
        HBox row = new HBox(10);
        row.setStyle("-fx-padding: 5 0;");

        Label rankLabel = new Label("#" + rank);
        rankLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 30;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-min-width: 80;");

        BigDecimal percent = maxAmount.compareTo(BigDecimal.ZERO) > 0
                ? amount.multiply(new BigDecimal("100")).divide(maxAmount, 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ProgressBar bar = new ProgressBar(percent.doubleValue() / 100);
        bar.setPrefWidth(100);
        bar.setStyle(isIncome
                ? "-fx-accent: #c0392b; -fx-background-color: #ffebee;"
                : "-fx-accent: #27ae60; -fx-background-color: #e8f5e9;");

        Label amountLabel = new Label(formatCurrency(amount));
        amountLabel.setStyle(
                "-fx-font-weight: bold; -fx-min-width: 90; -fx-text-fill: " + (isIncome ? "#c0392b" : "#27ae60") + ";");

        row.getChildren().addAll(rankLabel, nameLabel, bar, amountLabel);
        return row;
    }

    private void filterByCategory(String category) {
        categoryComboBox.setValue("全部");
        searchField.setText(category);
        onSearch();
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            int pageSize = 20;
            int pageCount = (int) Math.ceil((double) allAccounts.size() / pageSize);
            pageCount = Math.max(pageCount, 1);
            pagination.setPageCount(pageCount);
            pagination.setCurrentPageIndex(0);
            updateTablePage(0);
            return;
        }

        List<Account> filteredList = allAccounts.stream()
                .filter(account -> {
                    String category = getCategoryName(account).toLowerCase();
                    return category.contains(keyword) ||
                            (account.getDescription() != null &&
                                    account.getDescription().toLowerCase().contains(keyword))
                            ||
                            (account.getProjectName() != null &&
                                    account.getProjectName().toLowerCase().contains(keyword));
                })
                .collect(Collectors.toList());

        int pageSize = 20;
        int pageCount = (int) Math.ceil((double) filteredList.size() / pageSize);
        pageCount = Math.max(pageCount, 1);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);

        int toIndex = Math.min(pageSize, filteredList.size());
        transactionTable.setItems(FXCollections.observableArrayList(filteredList.subList(0, toIndex)));
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        loadData();
    }

    @FXML
    private void onExportExcel() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("导出Excel");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
            fileChooser.setInitialFileName("账目流水_" +
                    new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx");

            Stage stage = (Stage) transactionTable.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                exportToExcel(file);
                showSuccess("导出成功！");
            }
        } catch (Exception e) {
            showError("导出失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onExportPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("导出PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF文件", "*.pdf"));
            fileChooser.setInitialFileName("账目流水_" +
                    new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf");

            Stage stage = (Stage) transactionTable.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                exportToPDF(file);
                showSuccess("导出成功！");
            }
        } catch (Exception e) {
            showError("导出失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) transactionTable.getScene().getWindow();
        stage.close();
    }

    private void exportToExcel(File file) throws Exception {
        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("账目流水");

        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = { "日期", "类型", "分类", "项目", "金额", "累计余额", "占比", "描述" };
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        int rowNum = 1;
        for (Account account : allAccounts) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(
                    account.getTransactionDate() != null ? dateFormat.format(account.getTransactionDate()) : "");
            row.createCell(1).setCellValue("income".equals(account.getType()) ? "收入" : "支出");
            row.createCell(2).setCellValue(getCategoryName(account));
            row.createCell(3).setCellValue(account.getProjectName() != null ? account.getProjectName() : "");
            row.createCell(4).setCellValue(account.getAmount() != null ? account.getAmount().doubleValue() : 0);
            row.createCell(5)
                    .setCellValue(account.getRunningBalance() != null ? account.getRunningBalance().doubleValue() : 0);
            row.createCell(6)
                    .setCellValue(account.getPeriodPercent() != null ? account.getPeriodPercent().doubleValue() : 0);
            row.createCell(7).setCellValue(account.getDescription() != null ? account.getDescription() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();
    }

    private void exportToPDF(File file) throws Exception {
        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 18,
                com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph(
                "账目流水报表", titleFont);
        title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        document.add(title);
        document.add(new com.itextpdf.text.Paragraph(" "));

        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(6);
        table.setWidthPercentage(100);

        String[] headers = { "日期", "类型", "分类", "金额", "累计余额", "描述" };
        for (String header : headers) {
            table.addCell(header);
        }

        for (Account account : allAccounts) {
            table.addCell(account.getTransactionDate() != null ? dateFormat.format(account.getTransactionDate()) : "");
            table.addCell("income".equals(account.getType()) ? "收入" : "支出");
            table.addCell(getCategoryName(account));
            table.addCell(account.getAmount() != null ? account.getAmount().toString() : "0");
            table.addCell(account.getRunningBalance() != null ? account.getRunningBalance().toString() : "0");
            table.addCell(account.getDescription() != null ? account.getDescription() : "");
        }

        document.add(table);
        document.close();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null)
            return "¥0.00";
        return "¥" + String.format("%,.2f", amount);
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
            loadingIndicator.setManaged(show);
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("成功");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
