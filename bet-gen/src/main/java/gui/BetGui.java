package gui;

import api.OddsApiClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.BetOption;
import model.Match;
import model.RiskLevel;
import service.BetLogicService;
import service.DataParser;
import service.HistoricalDataService;
import service.StatsService;
import java.util.ArrayList;
import java.util.List;

public class BetGui extends Application {
    private OddsApiClient client;
    private DataParser parser;
    private BetLogicService logicService;

    private final ListView<String> mainListView = new ListView<>();
    private final List<Match> allMatches = new ArrayList<>();
    private final Label totalOddLabel = new Label("TOTAL ODD: 0.00");

    @Override
    public void init() {
        try {
            client = new OddsApiClient();
            parser = new DataParser();
            logicService = new BetLogicService(new StatsService());
        } catch (Exception e) {
            System.err.println(">>> [CRITICAL ERROR]");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // font from resources
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_18pt-Medium.ttf"), 13);
        } catch (Exception e) {
            System.err.println("Nie udało się załadować czcionki: " + e.getMessage());
        }
        // -=- HEADER -=-
        Label titleLabel = new Label("\uD83D\uDCA5 Bet Krzysztof Generator \uD83D\uDCA5");
        titleLabel.getStyleClass().add("main-title");

        // -=- SETTINGS -=-
        Label settingsLabel = new Label("SETTINGS");
        settingsLabel.getStyleClass().add("sidebar-header");

        TextField oddInput = new TextField("5.0");
        oddInput.setPromptText("Target Odds");

        ComboBox<RiskLevel> riskCombo = new ComboBox<>();
        riskCombo.getItems().addAll(RiskLevel.SAFE, RiskLevel.MEDIUM, RiskLevel.RISKY);
        riskCombo.setValue(RiskLevel.SAFE);
        riskCombo.setMaxWidth(Double.MAX_VALUE);

        // -=- BUTTONS -=-
        Button fetchBtn = new Button("Download Data");
        Button analysisBtn = new Button("Show BTTS Analysis");
        Button couponBtn = new Button("Generate Smart Coupon");

        fetchBtn.getStyleClass().add("btn-secondary");
        analysisBtn.getStyleClass().add("btn-secondary");
        couponBtn.getStyleClass().add("btn-primary");

        totalOddLabel.getStyleClass().add("summary-label");
        totalOddLabel.setMaxWidth(Double.MAX_VALUE);

        fetchBtn.setMaxWidth(Double.MAX_VALUE);
        analysisBtn.setMaxWidth(Double.MAX_VALUE);
        couponBtn.setMaxWidth(Double.MAX_VALUE);

        // -=- LISTVIEW CARDS -=-
        mainListView.getStyleClass().add("results-list");
        setupListViewFactory(); // Custom method to build cards

        // -=- LOGIC FOR BUTTONS -=-
        fetchBtn.setOnAction(e -> {
            mainListView.getItems().setAll("INFO:Checking cache & downloading data...");

            new Thread(() -> {
                try {
                    List<String> leagueKeys = List.of("soccer_epl", "soccer_spain_la_liga", "soccer_italy_serie_a", "soccer_germany_bundesliga", "soccer_france_ligue_one");
                    allMatches.clear();

                    for (String key : leagueKeys) {
                        String data = loadResponseFromCache(key);

                        if (data == null) {
                            data = client.getRawData(key);
                            saveResponseToCache(key, data);
                        }

                        allMatches.addAll(parser.parseMatches(data));
                    }

                    Platform.runLater(() -> {
                        mainListView.getItems().clear();
                        mainListView.getItems().add("INFO:Success! Loaded " + allMatches.size() + " matches from Cache/API.");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        mainListView.getItems().clear();
                        mainListView.getItems().add("WARN:Error: " + ex.getMessage());
                    });
                }
            }).start();
        });

        analysisBtn.setOnAction(e -> {
            if (allMatches.isEmpty()) {
                mainListView.getItems().setAll("WARN:No data found. Please download data first!");
                return;
            }

            mainListView.getItems().clear();
            mainListView.getItems().add("HEADER:  FULL MATCH ANALYSIS & BTTS  ");

            String analysisRaw = logicService.getAllMatchesAnalysis(allMatches);
            String[] lines = analysisRaw.split("\n");

            for (String line : lines) {
                if (line.contains("|")) {
                    mainListView.getItems().add("ANALYSIS_CARD:" + line);
                }
            }
        });

        couponBtn.setOnAction(e -> {
            if (allMatches.isEmpty()) {
                mainListView.getItems().setAll("WARN:No data found. Please download data first!");
                return;
            }
            try {
                double target = Double.parseDouble(oddInput.getText().replace(",", "."));
                RiskLevel risk = riskCombo.getValue();
                List<BetOption> coupon = logicService.generateSmartCoupon(allMatches, target, risk);

                mainListView.getItems().clear();
                mainListView.getItems().add("HEADER:  YOUR SMART COUPON  ");

                double totalOdd = 1.0;
                for (BetOption b : coupon) {
                    mainListView.getItems().add("BET_CARD:" + b.getName() + "|" + b.getPrice());
                    totalOdd *= b.getPrice();
                }

                totalOddLabel.setText("TOTAL ODD: " + String.format("%.2f", totalOdd));
            } catch (Exception ex) {
                mainListView.getItems().setAll("Invalid input: " + ex.getMessage());
            }
        });

        // -=- LAYOUT -=-
        VBox sidebar = new VBox(15);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(25));
        sidebar.setPrefWidth(280);
        sidebar.getChildren().addAll(
                settingsLabel,
                new Label("Total Odds:"), oddInput,
                new Label("Risk Level:"), riskCombo,
                new Separator(),
                fetchBtn, analysisBtn, couponBtn
        );

        VBox listViewContainer = new VBox(15);
        listViewContainer.getChildren().addAll(mainListView, totalOddLabel);
        VBox.setVgrow(mainListView, Priority.ALWAYS);

        StackPane contentStack = new StackPane();
        contentStack.getChildren().add(listViewContainer);

        // listener -> waits for clicking at the exact match
        mainListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && (newVal.startsWith("BET_CARD:") || newVal.startsWith("ANALYSIS_CARD:"))) {
                showMatchDetails(contentStack, listViewContainer, newVal);
            }
        });

        BorderPane mainLayout = new BorderPane();
        HBox topBar = new HBox(titleLabel);
        topBar.setPadding(new Insets(20, 20, 10, 20));

        mainLayout.setTop(topBar);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentStack);

        BorderPane.setMargin(contentStack, new Insets(0, 20, 20, 10));

        Scene scene = new Scene(mainLayout, 1000, 700);

        scene.getStylesheets().addAll(
                getClass().getResource("/base.css").toExternalForm(),
                getClass().getResource("/sidebar.css").toExternalForm(),
                getClass().getResource("/components.css").toExternalForm(),
                getClass().getResource("/details.css").toExternalForm()
        );

        primaryStage.setScene(scene);
        primaryStage.setTitle("Betting Dashboard");
        primaryStage.show();
    }

    // -=- CELL FACTORY -=-
    private void setupListViewFactory() {
        mainListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");

                    // -=- HEADER -=-
                } else if (item.startsWith("HEADER:")) {
                    String title = item.replace("HEADER:", "");
                    Label headerLabel = new Label(title);
                    headerLabel.getStyleClass().add("header-text");

                    HBox headerBox = new HBox(headerLabel);
                    headerBox.getStyleClass().add("coupon-header-box");
                    headerBox.setAlignment(Pos.CENTER);

                    setGraphic(headerBox);
                    setText(null);
                    // -=- COUPON -=-
                } else if (item.startsWith("BET_CARD:")) {
                    String content = item.replace("BET_CARD:", "");
                    String[] parts = content.split("\\|");

                    HBox card = new HBox(15);
                    card.getStyleClass().add("coupon-card");
                    card.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("⚽");
                    icon.getStyleClass().add("card-icon");

                    String fullInfo = parts[0];
                    int splitIndex = fullInfo.indexOf("(");

                    String matchName = (splitIndex != -1) ? fullInfo.substring(0, splitIndex).trim() : fullInfo;
                    String subDetails = (splitIndex != -1) ? fullInfo.substring(splitIndex).trim() : "";

                    subDetails = subDetails.replace("(", "")
                            .replace(")", "   |   ")
                            .replace("[", "")
                            .replace("]", "   |   ");

                    subDetails = subDetails.substring(0, subDetails.length() - 4);

                    VBox textContainer = new VBox(2);
                    textContainer.setAlignment(Pos.CENTER_LEFT);

                    Label nameLabel = new Label(matchName);
                    nameLabel.getStyleClass().add("card-name");

                    Label statsLabel = new Label(subDetails);
                    statsLabel.getStyleClass().add("card-stats");

                    textContainer.getChildren().addAll(nameLabel, statsLabel);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label odd = new Label(parts[1]);
                    odd.getStyleClass().add("card-odd");

                    card.getChildren().addAll(icon, textContainer, spacer, odd);
                    setGraphic(card);
                    setText(null);

                    // -=- BTTS ANALYSIS -=-
                } else if (item.startsWith("ANALYSIS_CARD:")) {
                    String raw = item.replace("ANALYSIS_CARD:", "");
                    String[] parts = raw.split("\\|");

                    HBox card = new HBox(12);
                    card.getStyleClass().add("analysis-card");
                    card.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("📊");

                    VBox textContainer = new VBox(3);
                    textContainer.setMinWidth(Region.USE_PREF_SIZE + 5);

                    Label matchLabel = new Label(parts[0].trim());
                    matchLabel.getStyleClass().add("card-name-small");

                    String statsStr = (parts.length >= 3) ? parts[1].trim() + "   |   " + parts[2].trim()+ "   " : "";
                    Label statsLabel = new Label(statsStr);
                    statsLabel.getStyleClass().add("card-stats");

                    statsLabel.setMinWidth(Region.USE_PREF_SIZE);

                    textContainer.getChildren().addAll(matchLabel, statsLabel);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    String riskText = raw.contains("HIGH") ? "HIGH" : (raw.contains("LOW") ? "LOW" : "MEDIUM");
                    Label riskBadge = new Label(riskText);
                    riskBadge.getStyleClass().addAll("risk-badge", "badge-" + riskText.toLowerCase());

                    card.getChildren().addAll(icon, textContainer, spacer, riskBadge);
                    setGraphic(card);
                    setText(null);

                    // -=- DOWNLOAD INFO -=-
                } else if (item.startsWith("INFO:") || item.startsWith("WARN:")) {
                    boolean isInfo = item.startsWith("INFO:");
                    String message = item.replace(isInfo ? "INFO:" : "WARN:", "");

                    HBox statusBox = new HBox(10);
                    statusBox.setAlignment(Pos.CENTER);
                    statusBox.getStyleClass().add(isInfo ? "download-info" : "download-warn");

                    Label msgLabel = new Label(message);
                    msgLabel.getStyleClass().add("download-message-text");

                    statusBox.getChildren().addAll(msgLabel);
                    setGraphic(statusBox);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(item);
                    getStyleClass().add("log-line");
                }
            }
        });
    }

    private String getCachePath(String leagueKey) {
        String userHome = System.getProperty("user.home");
        java.nio.file.Path folder = java.nio.file.Paths.get(userHome, ".betaidashboard");
        try {
            if (!java.nio.file.Files.exists(folder)) {
                java.nio.file.Files.createDirectories(folder);
            }
        } catch (java.io.IOException e) { e.printStackTrace(); }
        return folder.resolve("cache_" + leagueKey + ".json").toString();
    }

    private void saveResponseToCache(String leagueKey, String data) {
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get(getCachePath(leagueKey)), data);
        } catch (java.io.IOException e) {
            System.err.println("Failed to save cache for: " + leagueKey);
        }
    }

    private String loadResponseFromCache(String leagueKey) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(getCachePath(leagueKey));
            if (java.nio.file.Files.exists(path)) {
                long lastModified = java.nio.file.Files.getLastModifiedTime(path).toMillis();
                if (System.currentTimeMillis() - lastModified < 12 * 60 * 60 * 1000) {
                    return java.nio.file.Files.readString(path);
                }
            }
        } catch (java.io.IOException e) {
            return null;
        }
        return null;
    }

    // -=- DETAILED MATCH VIEW (FIXED) -=-
    private void showMatchDetails(StackPane stack, VBox originalView, String itemData) {
        String home = "";
        String away = "";
        String datePart = "N/A";
        String matchName = "";

        // Smart parsing for both formats (verified logic)
        if (itemData.startsWith("ANALYSIS_CARD:")) {
            String raw = itemData.replace("ANALYSIS_CARD:", "");
            datePart = raw.contains("[") ? raw.substring(raw.indexOf("[") + 1, raw.indexOf("]")) : "N/A";
            String titlePart = raw.split("\\|")[0];
            matchName = titlePart.replace("[" + datePart + "]", "").trim();
        } else if (itemData.startsWith("BET_CARD:")) {
            String raw = itemData.replace("BET_CARD:", "");
            matchName = raw.split("\\(")[0].trim();
            if (raw.contains("(") && raw.contains(")")) {
                datePart = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")")).trim();
            }
        }

        String[] teams = matchName.split(" vs ");
        home = teams[0].trim();
        away = (teams.length > 1) ? teams[1].trim() : "";

        // Main container for details
        VBox detailLayout = new VBox(25);
        detailLayout.getStyleClass().add("detail-container");
        detailLayout.setPadding(new Insets(20));

        // Navigation Components
        Button backBtn = new Button("← BACK TO DASHBOARD");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> { stack.getChildren().setAll(originalView); });

        VBox headerBox = new VBox(5);
        headerBox.getStyleClass().add("detail-header-box");
        Label dLabel = new Label("DATE  -  " + datePart);
        dLabel.getStyleClass().add("detail-date-label");
        Label tLabel = new Label(home.toUpperCase() + "  -  " + away.toUpperCase());
        tLabel.getStyleClass().addAll("header-main-text", "detail-title-text");
        headerBox.getChildren().addAll(dLabel, tLabel);

        // --- PREDICTION CARD ---
        VBox predictionBox = new VBox(5);
        predictionBox.getStyleClass().add("prediction-card");

        StatsService stats = new StatsService();
        double probDecimal = stats.predictBTTSProb(home, away); // Returns value like 0.60

        Label probValue = new Label(String.format("%.1f%%", probDecimal * 100));
        Label probTitle = new Label("AI BTTS PROBABILITY");
        probTitle.getStyleClass().add("prediction-title-label");
        probValue.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 32px; -fx-font-weight: 900;");
        predictionBox.getChildren().addAll(probTitle, probValue);

        // --- VALUE ANALYSIS  ---
        VBox valueAnalysisBox = new VBox(10);
        valueAnalysisBox.getStyleClass().add("value-analysis-card");
        valueAnalysisBox.setAlignment(Pos.CENTER);

        // MATHEMATICS FIX: predictBTTSProb already returns decimal (e.g., 0.60)
        double fairOdd = 1.0 / Math.max(probDecimal, 0.01);
        double bookieOdd = 1.85; // Default for now
        double edge = (bookieOdd / fairOdd - 1) * 100;

        Label valueTitle = new Label("BTTS PROFITABILITY ANALYSIS");
        valueTitle.getStyleClass().add("prediction-title-label");


        HBox oddsComparison = new HBox(30);
        oddsComparison.setAlignment(Pos.CENTER);
        oddsComparison.getChildren().addAll(
                createValueIndicator("FAIR ODD", String.format("%.2f", fairOdd), "#58a6ff"),
                createValueIndicator("BOOKIE ODD", String.format("%.2f", bookieOdd), "#58a6ff")
        );

        Label profitLabel = new Label();
        if (bookieOdd > fairOdd) {
            profitLabel.setText("PROFITABLE BET:   +" + String.format("%.2f%%", edge));
            profitLabel.setStyle("-fx-text-fill: #238636; -fx-font-weight: bold; -fx-font-size: 16px; -fx-letter-spacing: 3px;");
        } else {
            profitLabel.setText("NOT PROFITABLE:   " + String.format("%.2f%%", edge));
            profitLabel.setStyle("-fx-text-fill: #da3633; -fx-font-weight: bold; -fx-font-size: 16px; -fx-letter-spacing: 3px;");
        }
        valueAnalysisBox.getChildren().addAll(valueTitle, oddsComparison, profitLabel);

        // --- FORM SECTION (ASYNC) ---
        VBox formContainer = new VBox(10);
        formContainer.setAlignment(Pos.CENTER);
        Label loadingLabel = new Label("Loading team form from API...");
        loadingLabel.setStyle("-fx-text-fill: #8b949e;");
        formContainer.getChildren().add(loadingLabel);

        // Stats Grid
        GridPane grid = new GridPane();
        grid.getStyleClass().add("stats-grid");
        grid.setAlignment(Pos.CENTER);
        addVisualRow(grid, "WIN RATE", stats.getWinRateString(home), stats.getWinRateString(away), 0);
        addVisualRow(grid, "AVG GOALS", stats.getTeamGF(home), stats.getTeamGF(away), 1);
        addVisualRow(grid, "BTTS HISTORY", stats.getBTTS(home), stats.getBTTS(away), 2);

        // Combine everything in the layout
        detailLayout.getChildren().addAll(backBtn, headerBox, predictionBox, valueAnalysisBox, formContainer, grid);

        // --- SCROLLBAR FIX ---
        // Wrap detailLayout in a ScrollPane to allow vertical movement
        ScrollPane scrollWrapper = new ScrollPane(detailLayout);
        scrollWrapper.setFitToWidth(true); // Ensures the width fits the window
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Hide horizontal scroll
        scrollWrapper.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Set the scrollable view to the main stack
        stack.getChildren().setAll(scrollWrapper);

        final String homeFinal = home;
        final String awayFinal = away;

        // Fetch form data in the background
        new Thread(() -> {
            HistoricalDataService history = new HistoricalDataService();
            // Use final copies here
            List<HistoricalDataService.MatchResult> homeF = history.getTeamForm(homeFinal);
            List<HistoricalDataService.MatchResult> awayF = history.getTeamForm(awayFinal);

            Platform.runLater(() -> {
                formContainer.getChildren().clear();
                Label formTitle = new Label("LAST 5 MATCHES PERFORMANCE");
                formTitle.setStyle("-fx-text-fill: #c9d1d9; -fx-font-weight: 800; -fx-font-size: 13px;");

                if (homeF.isEmpty() && awayF.isEmpty()) {
                    Label err = new Label("No data available (Rate Limit reached?)");
                    err.setStyle("-fx-text-fill: #cf222e;");
                    formContainer.getChildren().addAll(formTitle, err);
                } else {
                    formContainer.getChildren().addAll(formTitle,
                            createFormRow(homeFinal, homeF.stream().map(r -> r.res()).toList()),
                            createFormRow(awayFinal, awayF.stream().map(r -> r.res()).toList())
                    );
                }
            });
        }).start();
    }

    private VBox createStatWithBar(String value, boolean leftAlign) {
        VBox box = new VBox(5);
        box.setAlignment(leftAlign ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        Label valLab = new Label(value);
        valLab.getStyleClass().add("stats-row-value");

        StackPane barBg = new StackPane();
        barBg.getStyleClass().add("stat-bar-bg");
        barBg.setMinWidth(100); barBg.setMaxWidth(100);

        Region fill = new Region();
        fill.getStyleClass().add("stat-bar-fill");

        double val = 0;
        try { val = Double.parseDouble(value.replace("%", "").replace(",", ".")); } catch (Exception e) { val = 0; }

        double calculatedWidth = (val <= 10) ? val * 20 : val;
        double finalWidth = Math.min(calculatedWidth, 100);
        fill.setMinWidth(finalWidth); fill.setMaxWidth(finalWidth); fill.setPrefWidth(finalWidth);

        barBg.getChildren().add(fill);
        StackPane.setAlignment(fill, leftAlign ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        box.getChildren().addAll(valLab, barBg);
        return box;
    }

    private void addVisualRow(GridPane g, String label, String hVal, String aVal, int row) {
        VBox homeSide = createStatWithBar(hVal, true);
        VBox awaySide = createStatWithBar(aVal, false);
        Label midLabel = new Label(label);
        midLabel.getStyleClass().add("stats-row-label");
        midLabel.setAlignment(Pos.CENTER); midLabel.setMinWidth(120);
        g.add(homeSide, 0, row); g.add(midLabel, 1, row); g.add(awaySide, 2, row);
    }

    private HBox createFormRow(String teamName, List<String> results) {
        HBox container = new HBox(12);
        container.setAlignment(Pos.CENTER);
        Label nameLabel = new Label(teamName.toUpperCase());
        nameLabel.getStyleClass().add("stats-row-label");
        nameLabel.setMinWidth(140); nameLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox circles = new HBox(8);
        for (String res : results) {
            StackPane circle = new StackPane();
            circle.getStyleClass().addAll("form-circle", "form-" + res.toLowerCase());
            circle.setMinWidth(28); circle.setMinHeight(28);
            Label l = new Label(res);
            l.setStyle("-fx-text-fill: white; -fx-font-weight: 900;");
            circle.getChildren().add(l);
            circles.getChildren().add(circle);
        }
        container.getChildren().addAll(nameLabel, circles);
        return container;
    }

    // helper method for pretty icons for compairng bets with mathematics
    private VBox createValueIndicator(String title, String value, String color) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 3px;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px; -fx-font-weight: 900; -fx-letter-spacing: 3px;");
        VBox box = new VBox(2, t, v);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public static void main(String[] args) { launch(args); }
}