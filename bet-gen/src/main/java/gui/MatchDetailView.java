package gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import service.HistoricalDataService;
import service.StatsService;
import java.util.List;

public class MatchDetailView extends ScrollPane {

    public MatchDetailView(StackPane stack, VBox originalView, String itemData) {
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
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label dLabel = new Label("DATE  -  " + datePart);
        dLabel.getStyleClass().add("detail-date-label");
        dLabel.setMaxWidth(Double.MAX_VALUE);
        dLabel.setAlignment(Pos.CENTER_LEFT);

        Label tLabel = new Label(home.toUpperCase() + "  -  " + away.toUpperCase());
        tLabel.getStyleClass().addAll("header-main-text", "detail-title-text");
        tLabel.setAlignment(Pos.CENTER_LEFT);

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

        // predictBTTSProb already returns decimal
        double fairOdd = 1.0 / Math.max(probDecimal, 0.01);
        double expectedGoals = stats.predictMatchTotal(home, away);
        double bookieOdd; // bookie odds based on expected goals

        if (expectedGoals > 3.2) {
            bookieOdd = 1.45 + (Math.random() * 0.1); // very high chance of BTTS
        } else if (expectedGoals > 2.5) {
            bookieOdd = 1.65 + (Math.random() * 0.15); // standard match
        } else if (expectedGoals > 1.8) {
            bookieOdd = 1.90 + (Math.random() * 0.2); // low chance
        } else {
            bookieOdd = 2.20 + (Math.random() * 0.3); // very defensive teams
        }

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

        // --- SCROLLBARS FIX ---
        // Wrap detailLayout in a ScrollPane to allow vertical movement
        this.setContent(detailLayout);
        this.setFitToWidth(true); // Ensures the width fits the window
        this.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Hide horizontal scroll
        this.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        final String homeFinal = home;
        final String awayFinal = away;

        // Fetch form data in the background
        new Thread(() -> {
            HistoricalDataService history = new HistoricalDataService();
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

    private void addVisualRow(GridPane g, String label, String hVal, String aVal, int row) {
        VBox homeSide = createStatWithBar(hVal, true);
        VBox awaySide = createStatWithBar(aVal, false);
        Label midLabel = new Label(label);
        midLabel.getStyleClass().add("stats-row-label");
        midLabel.setAlignment(Pos.CENTER); midLabel.setMinWidth(120);
        g.add(homeSide, 0, row); g.add(midLabel, 1, row); g.add(awaySide, 2, row);
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

    private VBox createValueIndicator(String title, String value, String color) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 3px;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px; -fx-font-weight: 900; -fx-letter-spacing: 3px;");
        VBox box = new VBox(2, t, v);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}