package gui;

import api.OddsApiClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

import static api.CacheManager.loadCache;
import static api.CacheManager.saveCache;

public class BetGui extends Application {
    private OddsApiClient client;
    private DataParser parser;
    private BetLogicService logicService;

    private final ListView<String> mainListView = new ListView<>();
    private final List<Match> allMatches = new ArrayList<>();
    private final Label totalOddLabel = new Label("TOTAL ODD: 0.00");

    private final StackPane contentStack = new StackPane();
    private VBox listViewContainer;
    private SidebarComponent sidebar;

    // objects for chat expert
    private List<BetOption> lastGeneratedCoupon = new ArrayList<>();

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
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_18pt-Medium.ttf"), 13);
        } catch (Exception e) {
            System.err.println("Could not load any font: " + e.getMessage());
        }

        HeaderComponent titleContent = new HeaderComponent();
        sidebar = new SidebarComponent();

        // -=- LISTVIEW -=-
        mainListView.getStyleClass().add("results-list");
        mainListView.setCellFactory(new MatchCell());

        // -=- LOGIC FOR BUTTONS -=-
        sidebar.fetchBtn.setOnAction(e -> handleFetch());
        sidebar.analysisBtn.setOnAction(e -> handleAnalysis());
        sidebar.couponBtn.setOnAction(e -> handleCouponGeneration());
        sidebar.aiBotBtn.setOnAction(e -> contentStack.getChildren().setAll(new ChatView(contentStack, listViewContainer, lastGeneratedCoupon)));

        // -- TOTAL ODD BOX ---
        Image cashImg = new Image(getClass().getResourceAsStream("/images/money-bag.png"));
        ImageView cashIcon = new ImageView(cashImg);
        cashIcon.setFitHeight(40);
        cashIcon.setPreserveRatio(true);

        HBox totalOddBox = new HBox(5, cashIcon, totalOddLabel);
        totalOddBox.setAlignment(Pos.CENTER_RIGHT);
        totalOddBox.setMaxWidth(Double.MAX_VALUE);
        totalOddBox.getStyleClass().add("summary-label");
        totalOddLabel.setStyle("-fx-text-fill: #58a6ff; -fx-background-color: transparent; -fx-border-color: transparent; -fx-effect: none;");

        listViewContainer = new VBox(15);
        listViewContainer.getChildren().addAll(mainListView, totalOddBox);
        VBox.setVgrow(mainListView, Priority.ALWAYS);

        contentStack.getChildren().add(listViewContainer);

        // listener -> waits for clicking at the exact match
        mainListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && (newVal.startsWith("BET_CARD:") || newVal.startsWith("ANALYSIS_CARD:"))) {
                contentStack.getChildren().setAll(new MatchDetailView(contentStack, listViewContainer, newVal));
                Platform.runLater(() -> mainListView.getSelectionModel().clearSelection());
            }
        });

        BorderPane mainLayout = new BorderPane();
        HBox topBar = new HBox(titleContent);
        topBar.setPadding(new Insets(20, 20, 10, 20));

        topBar.setAlignment(Pos.CENTER_LEFT);

        mainLayout.setTop(topBar);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentStack);

        BorderPane.setMargin(contentStack, new Insets(0, 20, 20, 10));

        Scene scene = new Scene(mainLayout, 1000, 750);
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

    private void handleFetch() {
        mainListView.getItems().setAll("INFO:Checking cache & downloading data...");
        new Thread(() -> {
            try {
                List<String> leagueKeys = List.of("soccer_epl", "soccer_spain_la_liga", "soccer_italy_serie_a", "soccer_germany_bundesliga", "soccer_france_ligue_one");
                allMatches.clear();

                for (String key : leagueKeys) {
                    String data = client.getRawData(key);
                    allMatches.addAll(parser.parseMatches(data));
                }

                Platform.runLater(() -> {
                    mainListView.getItems().clear();
                    mainListView.getItems().add("INFO:Success! Loaded " + allMatches.size() + " matches.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    mainListView.getItems().clear();
                    mainListView.getItems().add("WARN:Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void handleAnalysis() {
        if (allMatches.isEmpty()) {
            mainListView.getItems().setAll("WARN:No data found. Please download data first!");
            return;
        }
        mainListView.getItems().clear();
        mainListView.getItems().add("HEADER:  FULL MATCH ANALYSIS & BTTS  ");
        String analysisRaw = logicService.getAllMatchesAnalysis(allMatches);
        for (String line : analysisRaw.split("\n")) {
            if (line.contains("|")) mainListView.getItems().add("ANALYSIS_CARD:" + line);
        }
    }

    private void handleCouponGeneration() {
        if (allMatches.isEmpty()) {
            mainListView.getItems().setAll("WARN:No data found. Please download data first!");
            return;
        }
        try {
            double target = Double.parseDouble(sidebar.oddInput.getText().replace(",", "."));
            RiskLevel risk = sidebar.riskCombo.getValue();
            List<BetOption> coupon = logicService.generateSmartCoupon(allMatches, target, risk);
            this.lastGeneratedCoupon = coupon;
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
    }

    public static void main(String[] args) { launch(args); }
}