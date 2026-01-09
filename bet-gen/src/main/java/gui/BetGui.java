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
            mainListView.getItems().setAll("INFO:Downloading data from API... Please wait.");

            new Thread(() -> {
                try {
                    List<String> leagueKeys = List.of("soccer_epl", "soccer_spain_la_liga", "soccer_italy_serie_a", "soccer_germany_bundesliga", "soccer_france_ligue_one");
                    allMatches.clear();
                    for (String key : leagueKeys) {
                        allMatches.addAll(parser.parseMatches(client.getRawData(key)));
                    }

                    Platform.runLater(() -> {
                        mainListView.getItems().clear();
                        mainListView.getItems().add("INFO:Success! Loaded " + allMatches.size() + " matches.");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        mainListView.getItems().clear();
                        mainListView.getItems().add("WARN:Error while fetching data: " + ex.getMessage());
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
            // header of the btts analysis
            mainListView.getItems().add("HEADER:  FULL MATCH ANALYSIS & BTTS  ");

            // downloading data about matches
            String analysisRaw = logicService.getAllMatchesAnalysis(allMatches);
            String[] lines = analysisRaw.split("\n");

            for (String line : lines) {
                if (line.contains("|")) {
                    // changing format
                    // ANALYSIS_CARD:date|match|btts|goals|risk
                    String formatted = line.replace("[", "").replace("]", "|")
                            .replace(" | ", "|")
                            .replace(":", "|")
                            .replace("Exp. Goals", "");
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
                    // We add a special prefix to detect bets in CellFactory
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
        sidebar.getChildren().addAll(settingsLabel, new Label("Total Odds:"), oddInput, new Label("Risk Level:"), riskCombo, new Separator(), fetchBtn, analysisBtn, couponBtn);

        VBox centerArea = new VBox(15);
        centerArea.getChildren().addAll(mainListView, totalOddLabel);
        VBox.setVgrow(mainListView, Priority.ALWAYS);

        BorderPane mainLayout = new BorderPane();
        HBox topBar = new HBox(titleLabel);
        topBar.setPadding(new Insets(20, 20, 10, 20));
        mainLayout.setTop(topBar);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(centerArea);
        BorderPane.setMargin(centerArea, new Insets(0, 20, 20, 10));

        Scene scene = new Scene(mainLayout, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

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
                    headerBox.setAlignment(Pos.CENTER); // Wyśrodkowanie napisu

                    setGraphic(headerBox);
                    setText(null);

                    // -=- COUPON -=-
                } else if (item.startsWith("BET_CARD:")) {
                    String content = item.replace("BET_CARD:", "");
                    String[] parts = content.split("\\|"); // Separates info from odd price

                    HBox card = new HBox(15);
                    card.getStyleClass().add("coupon-card");
                    card.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("⚽");
                    icon.getStyleClass().add("card-icon");

                    String fullInfo = parts[0];
                    int splitIndex = fullInfo.indexOf("("); // Finds where date/btts starts

                    String matchName = (splitIndex != -1) ? fullInfo.substring(0, splitIndex).trim() : fullInfo;
                    String subDetails = (splitIndex != -1) ? fullInfo.substring(splitIndex).trim() : "";

                    subDetails = subDetails.replace("(", "")
                            .replace(")", "   -   ")
                            .replace("[", "")
                            .replace("]", "   -   ")
                            .replace("{", "")
                            .replace("}", "");

                    VBox textContainer = new VBox(2);
                    textContainer.setAlignment(Pos.CENTER_LEFT);

                    // Line 1: Bold Match Name
                    Label nameLabel = new Label(matchName);
                    nameLabel.getStyleClass().add("card-name");

                    // Line 2: Small Grey Stats (Date, Bet Type, BTTS)
                    Label statsLabel = new Label(subDetails);
                    statsLabel.getStyleClass().add("card-stats"); // Using the same class as in analysis

                    textContainer.getChildren().addAll(nameLabel, statsLabel);
                    // ----------------------------------------------------

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
                    String[] parts = raw.split("\\|"); // Split by the pipe character

                    HBox card = new HBox(12); // Slightly less spacing
                    card.getStyleClass().add("analysis-card");
                    card.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("📊");

                    // Vertical container to stack text and reduce width
                    VBox textContainer = new VBox(3);

                    textContainer.setMinWidth(Region.USE_PREF_SIZE + 5);

                    // LMatch and Date
                    Label matchLabel = new Label(parts[0].trim());
                    matchLabel.getStyleClass().add("card-name-small");

                    // Stats (BTTS & Goals)
                    String statsStr = (parts.length >= 3) ? parts[1].trim() + "    -    " + parts[2].trim()+ "   " : "";
                    Label statsLabel = new Label(statsStr);
                    statsLabel.getStyleClass().add("card-stats");

                    statsLabel.setMinWidth(Region.USE_PREF_SIZE);

                    textContainer.getChildren().addAll(matchLabel, statsLabel);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // Risk detection for the badge
                    String riskText = raw.contains("HIGH") ? "HIGH" : (raw.contains("LOW") ? "LOW" : "MEDIUM");
                    Label riskBadge = new Label(riskText);
                    riskBadge.getStyleClass().addAll("risk-badge", "badge-" + riskText.toLowerCase());

                    card.getChildren().addAll(icon, textContainer, spacer, riskBadge);
                    setGraphic(card);
                    setText(null);

                    // -=- DOWNLOADING DATA INFO -=-
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
                    // Standard log or analysis line
                    setGraphic(null);
                    setText(item);
                    getStyleClass().add("log-line");
                }
            }
        });
    }

    public static void main(String[] args) { launch(args); }
}