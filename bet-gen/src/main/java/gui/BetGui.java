package gui;

import api.OddsApiClient;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
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

    // Smart Bet Slip Summary Ribbon components
    private final Label selectionsCountLabel = new Label("0 Selections");
    private final Label totalOddLabel = new Label("Total Odds: 1.00");
    private final TextField stakeInput = new TextField("100");
    private final Label payoutLabel = new Label("Est. Payout: 0.00 PLN");
    private double currentTotalOdd = 1.0;

    private final StackPane contentStack = new StackPane();
    private VBox listViewContainer;
    private SidebarComponent sidebar;

    private List<BetOption> lastGeneratedCoupon = new ArrayList<>();

    public static void animateTransition(StackPane stack, Node newView) {
        newView.setOpacity(0);
        newView.setTranslateY(12);
        stack.getChildren().setAll(newView);

        FadeTransition ft = new FadeTransition(Duration.millis(200), newView);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(200), newView);
        tt.setToY(0);

        new ParallelTransition(ft, tt).play();
    }

    @Override
    public void init() {
        try {
            client = new OddsApiClient();
            parser = new DataParser();
            logicService = new BetLogicService(new StatsService());
        } catch (Exception e) {
            System.err.println(">>> [CRITICAL ERROR] Initializing services: " + e.getMessage());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_18pt-Medium.ttf"), 13);
        } catch (Exception e) {
            System.err.println("Could not load font: " + e.getMessage());
        }

        HeaderComponent titleContent = new HeaderComponent();
        sidebar = new SidebarComponent();

        // --- LIST VIEW SETUP ---
        mainListView.getStyleClass().add("results-list");
        mainListView.setCellFactory(new MatchCell());

        // --- BUTTON ACTIONS ---
        sidebar.fetchBtn.setOnAction(e -> handleFetch(null));
        sidebar.analysisBtn.setOnAction(e -> handleAnalysis());
        sidebar.couponBtn.setOnAction(e -> handleCouponGeneration());
        sidebar.aiBotBtn.setOnAction(e -> animateTransition(
                contentStack,
                new ChatView(contentStack, listViewContainer, lastGeneratedCoupon)
        ));

        // --- SCORIFY SUMMARY RIBBON ---
        HBox summaryRibbon = buildSummaryRibbon();

        listViewContainer = new VBox(14);
        listViewContainer.getChildren().addAll(mainListView, summaryRibbon);
        VBox.setVgrow(mainListView, Priority.ALWAYS);

        contentStack.getChildren().add(listViewContainer);

        // Click listener -> smooth animated transition to match details
        mainListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && (newVal.startsWith("BET_CARD:") || newVal.startsWith("ANALYSIS_CARD:"))) {
                animateTransition(contentStack, new MatchDetailView(contentStack, listViewContainer, newVal));
                Platform.runLater(() -> mainListView.getSelectionModel().clearSelection());
            }
        });

        // Initial welcome state
        showWelcomeState();

        // Background preload
        autoPreloadMatches();

        // Main Layout
        BorderPane mainLayout = new BorderPane();

        HBox topBar = new HBox(titleContent);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        mainLayout.setTop(topBar);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentStack);

        BorderPane.setMargin(contentStack, new Insets(16, 22, 16, 14));

        Scene scene = new Scene(mainLayout, 1140, 780);
        scene.getStylesheets().addAll(
                getClass().getResource("/base.css").toExternalForm(),
                getClass().getResource("/sidebar.css").toExternalForm(),
                getClass().getResource("/components.css").toExternalForm(),
                getClass().getResource("/details.css").toExternalForm()
        );

        primaryStage.setScene(scene);
        primaryStage.setTitle("Bet Generator Pro - Sports Betting Intelligence");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(680);
        primaryStage.show();
    }

    private HBox buildSummaryRibbon() {
        HBox ribbon = new HBox(16);
        ribbon.getStyleClass().add("summary-bar");
        ribbon.setAlignment(Pos.CENTER_LEFT);

        selectionsCountLabel.setStyle("-fx-text-fill: #78999e; -fx-font-weight: 800; -fx-font-size: 12px;");

        totalOddLabel.setStyle("-fx-text-fill: #00f5b8; -fx-font-weight: 900; -fx-font-size: 16px;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label stakeLabel = new Label("STAKE:");
        stakeLabel.setStyle("-fx-text-fill: #78999e; -fx-font-weight: 800; -fx-font-size: 11px;");

        stakeInput.getStyleClass().add("stake-field");
        stakeInput.textProperty().addListener((obs, oldVal, newVal) -> updatePayout());

        Label currLabel = new Label("PLN");
        currLabel.setStyle("-fx-text-fill: #4e6f73; -fx-font-weight: 800; -fx-font-size: 11px;");

        HBox stakeBox = new HBox(6, stakeLabel, stakeInput, currLabel);
        stakeBox.setAlignment(Pos.CENTER);

        payoutLabel.setStyle("-fx-text-fill: #2dd4bf; -fx-font-weight: 900; -fx-font-size: 15px;");

        Button copySlipBtn = new Button("Copy Slip");
        copySlipBtn.getStyleClass().add("preset-chip");
        copySlipBtn.setOnAction(e -> copySlipToClipboard());

        ribbon.getChildren().addAll(
                selectionsCountLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                totalOddLabel,
                spacer1,
                stakeBox,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                payoutLabel,
                copySlipBtn
        );

        return ribbon;
    }

    private void updatePayout() {
        try {
            double stake = Double.parseDouble(stakeInput.getText().replace(",", ".").trim());
            double payout = stake * currentTotalOdd;
            payoutLabel.setText(String.format("Est. Payout: %.2f PLN", payout));
        } catch (Exception e) {
            payoutLabel.setText("Est. Payout: 0.00 PLN");
        }
    }

    private void showWelcomeState() {
        mainListView.getItems().setAll(
                "HEADER:  BET GENERATOR PRO — SPORTS INTELLIGENCE  ",
                "INFO:Engine Initialized. 8 European Leagues supported (EPL, La Liga, Serie A, Bundesliga, Ligue 1, UCL, UEL, UECL).",
                "INFO:Quick Start: Choose target odds and risk profile on the left, then click 'Generate Smart Coupon'.",
                "INFO:Market Scanner: Click 'Show BTTS Analysis' to view goal probabilities across all upcoming matches."
        );
    }

    private void autoPreloadMatches() {
        new Thread(() -> {
            try {
                List<String> leagueKeys = List.of(
                        "soccer_epl",
                        "soccer_spain_la_liga",
                        "soccer_italy_serie_a",
                        "soccer_germany_bundesliga",
                        "soccer_france_ligue_one",
                        "soccer_uefa_champs_league",
                        "soccer_uefa_europa_league",
                        "soccer_uefa_europa_conference_league"
                );
                List<Match> loaded = new ArrayList<>();
                for (String key : leagueKeys) {
                    try {
                        String data = client.getRawData(key);
                        if (data != null && !data.isEmpty()) {
                            loaded.addAll(parser.parseMatches(data));
                        }
                    } catch (Exception ignored) {}
                }

                if (!loaded.isEmpty()) {
                    synchronized (allMatches) {
                        allMatches.clear();
                        allMatches.addAll(loaded);
                    }
                    Platform.runLater(() -> {
                        mainListView.getItems().add("INFO:Cache Synced: " + allMatches.size() + " upcoming matches loaded and ready.");
                    });
                }
            } catch (Exception e) {
                System.err.println("Preload info: " + e.getMessage());
            }
        }).start();
    }

    private void handleFetch(Runnable onComplete) {
        mainListView.getItems().setAll("INFO:Connecting to sports odds providers & verifying local cache...");
        new Thread(() -> {
            try {
                List<String> leagueKeys = List.of(
                        "soccer_epl",
                        "soccer_spain_la_liga",
                        "soccer_italy_serie_a",
                        "soccer_germany_bundesliga",
                        "soccer_france_ligue_one",
                        "soccer_uefa_champs_league",
                        "soccer_uefa_europa_league",
                        "soccer_uefa_europa_conference_league"
                );
                List<Match> loaded = new ArrayList<>();

                for (String key : leagueKeys) {
                    String data = client.getRawData(key);
                    loaded.addAll(parser.parseMatches(data));
                }

                synchronized (allMatches) {
                    allMatches.clear();
                    allMatches.addAll(loaded);
                }

                Platform.runLater(() -> {
                    mainListView.getItems().clear();
                    mainListView.getItems().add("INFO:Success! Market data updated with " + allMatches.size() + " matches across 8 leagues.");
                    if (onComplete != null) onComplete.run();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    mainListView.getItems().clear();
                    mainListView.getItems().add("WARN:Data synchronization error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void handleAnalysis() {
        if (allMatches.isEmpty()) {
            handleFetch(this::handleAnalysis);
            return;
        }
        mainListView.getItems().clear();
        mainListView.getItems().add("HEADER:  FULL MATCH ANALYSIS & BTTS PROBABILITY  ");
        String analysisRaw = logicService.getAllMatchesAnalysis(allMatches);
        for (String line : analysisRaw.split("\n")) {
            if (line.contains("|")) mainListView.getItems().add("ANALYSIS_CARD:" + line);
        }
    }

    private void handleCouponGeneration() {
        if (allMatches.isEmpty()) {
            handleFetch(this::handleCouponGeneration);
            return;
        }
        try {
            double target = Double.parseDouble(sidebar.oddInput.getText().replace(",", ".").trim());
            RiskLevel risk = sidebar.riskCombo.getValue();
            List<BetOption> coupon = logicService.generateSmartCoupon(allMatches, target, risk);
            this.lastGeneratedCoupon = coupon;

            mainListView.getItems().clear();
            mainListView.getItems().add("HEADER:  YOUR SMART ACCUMULATOR SLIP (" + coupon.size() + " SELECTIONS)  ");

            double totalOdd = 1.0;
            for (BetOption b : coupon) {
                mainListView.getItems().add("BET_CARD:" + b.getName() + "|" + b.getPrice());
                totalOdd *= b.getPrice();
            }

            this.currentTotalOdd = totalOdd;
            selectionsCountLabel.setText(coupon.size() + " Selections");
            totalOddLabel.setText(String.format("Total Odds: %.2f", totalOdd));
            updatePayout();

            if (coupon.isEmpty()) {
                mainListView.getItems().add("WARN:No matches found for target odds. Try increasing target or changing risk profile.");
            }
        } catch (Exception ex) {
            mainListView.getItems().setAll("WARN:Invalid input: " + ex.getMessage());
        }
    }

    private void copySlipToClipboard() {
        if (lastGeneratedCoupon.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("=== BET GENERATOR PRO SLIP ===\n");
        sb.append(String.format("Total Odds: %.2f | Selections: %d\n\n", currentTotalOdd, lastGeneratedCoupon.size()));
        for (BetOption b : lastGeneratedCoupon) {
            sb.append("• ").append(b.getName()).append(" @ ").append(b.getPrice()).append("\n");
        }
        try {
            double stake = Double.parseDouble(stakeInput.getText().replace(",", "."));
            sb.append(String.format("\nStake: %.2f PLN | Est. Payout: %.2f PLN\n", stake, stake * currentTotalOdd));
        } catch (Exception ignored) {}

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);

        mainListView.getItems().add(0, "INFO:Bet slip copied to clipboard.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
