package gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;
import service.HistoricalDataService;
import service.StatsService;
import java.util.List;

public class MatchDetailView extends ScrollPane {

    public MatchDetailView(StackPane stack, VBox originalView, String itemData) {
        String home = "";
        String away = "";
        String datePart = "N/A";
        String matchName = "";

        if (itemData.startsWith("ANALYSIS_CARD:")) {
            String raw = itemData.replace("ANALYSIS_CARD:", "").trim();
            datePart = raw.contains("[") ? raw.substring(raw.indexOf("[") + 1, raw.indexOf("]")) : "N/A";
            String titlePart = raw.split("\\|")[0];
            matchName = titlePart.replace("[" + datePart + "]", "").trim();
        } else if (itemData.startsWith("BET_CARD:")) {
            String raw = itemData.replace("BET_CARD:", "").trim();
            matchName = raw.split("\\(")[0].trim();
            if (raw.contains("(") && raw.contains(")")) {
                datePart = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")")).trim();
            }
        }

        String[] teams = matchName.split(" vs ");
        home = teams[0].trim();
        away = (teams.length > 1) ? teams[1].trim() : "";

        // Main layout container
        VBox detailLayout = new VBox(20);
        detailLayout.getStyleClass().add("detail-container");
        detailLayout.setPadding(new Insets(20));

        // Top Navigation Bar
        Button backBtn = new Button("← Back to Dashboard");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            BetGui.animateTransition(stack, originalView);
        });

        // --- MATCH HERO BANNER (SCORIFY STYLE) ---
        VBox headerBox = new VBox(8);
        headerBox.getStyleClass().add("detail-header-box");
        headerBox.setAlignment(Pos.CENTER);

        Label dateLabel = new Label(datePart);
        dateLabel.getStyleClass().add("detail-date-label");

        HBox matchupRow = new HBox(16);
        matchupRow.setAlignment(Pos.CENTER);

        StackPane homeBadge = createTeamBadge(MatchCell.getTeamMonogram(home));
        Label hTeam = new Label(home.toUpperCase());
        hTeam.getStyleClass().add("detail-title-text");
        HBox homeSide = new HBox(10, homeBadge, hTeam);
        homeSide.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(homeSide, Priority.ALWAYS);

        Label vsPill = new Label("Vs");
        vsPill.setStyle("-fx-text-fill: #4d6c70; -fx-font-size: 16px; -fx-font-weight: 900;");

        Label aTeam = new Label(away.toUpperCase());
        aTeam.getStyleClass().add("detail-title-text");
        StackPane awayBadge = createTeamBadge(MatchCell.getTeamMonogram(away));
        HBox awaySide = new HBox(10, aTeam, awayBadge);
        awaySide.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(awaySide, Priority.ALWAYS);

        matchupRow.getChildren().addAll(homeSide, vsPill, awaySide);

        Label subHeader = new Label("Predictive modeling, expected goals, and value analysis");
        subHeader.setStyle("-fx-text-fill: #78999e; -fx-font-size: 11px; -fx-font-weight: 600;");

        headerBox.getChildren().addAll(dateLabel, matchupRow, subHeader);

        // --- 3 KPI CARDS ---
        StatsService stats = new StatsService();
        double probDecimal = stats.predictBTTSProb(home, away);
        double expectedGoals = stats.predictMatchTotal(home, away);
        double fairOdd = 1.0 / Math.max(probDecimal, 0.01);
        double bookieOdd;

        if (expectedGoals > 3.2) {
            bookieOdd = 1.45 + (Math.random() * 0.1);
        } else if (expectedGoals > 2.5) {
            bookieOdd = 1.65 + (Math.random() * 0.15);
        } else if (expectedGoals > 1.8) {
            bookieOdd = 1.90 + (Math.random() * 0.2);
        } else {
            bookieOdd = 2.20 + (Math.random() * 0.3);
        }
        double edge = (bookieOdd / fairOdd - 1) * 100;

        HBox kpiContainer = new HBox(16);
        kpiContainer.setAlignment(Pos.CENTER);

        // KPI 1: AI BTTS Model
        VBox kpi1 = new VBox(6);
        kpi1.getStyleClass().add("detail-kpi-card");
        HBox.setHgrow(kpi1, Priority.ALWAYS);
        kpi1.setAlignment(Pos.CENTER);

        Label kpi1Title = new Label("AI BTTS PROBABILITY");
        kpi1Title.getStyleClass().add("kpi-title");

        Label kpi1Val = new Label(String.format("%.1f%%", probDecimal * 100));
        kpi1Val.getStyleClass().add("kpi-value-prob");

        Label kpi1Sub = new Label(probDecimal >= 0.60 ? "High Goal Expectancy" : (probDecimal <= 0.45 ? "Low Goal Expectancy" : "Balanced Expectancy"));
        kpi1Sub.getStyleClass().add("kpi-subtext");

        kpi1.getChildren().addAll(kpi1Title, kpi1Val, kpi1Sub);

        // KPI 2: Market Value & Edge
        VBox kpi2 = new VBox(6);
        kpi2.getStyleClass().add("detail-kpi-card");
        HBox.setHgrow(kpi2, Priority.ALWAYS);
        kpi2.setAlignment(Pos.CENTER);

        Label kpi2Title = new Label("MARKET VALUE & EDGE");
        kpi2Title.getStyleClass().add("kpi-title");

        HBox oddsComp = new HBox(16);
        oddsComp.setAlignment(Pos.CENTER);
        oddsComp.getChildren().addAll(
                createMiniOdd("FAIR ODD", String.format("%.2f", fairOdd)),
                createMiniOdd("BOOKIE", String.format("%.2f", bookieOdd))
        );

        Label edgeBadge = new Label();
        if (bookieOdd > fairOdd) {
            edgeBadge.setText("VALUE: +" + String.format("%.1f%%", edge));
            edgeBadge.getStyleClass().add("edge-badge-profit");
        } else {
            edgeBadge.setText("NO EDGE: " + String.format("%.1f%%", edge));
            edgeBadge.getStyleClass().add("edge-badge-loss");
        }

        kpi2.getChildren().addAll(kpi2Title, oddsComp, edgeBadge);

        // KPI 3: Expected Goals (xG)
        VBox kpi3 = new VBox(6);
        kpi3.getStyleClass().add("detail-kpi-card");
        HBox.setHgrow(kpi3, Priority.ALWAYS);
        kpi3.setAlignment(Pos.CENTER);

        Label kpi3Title = new Label("PROJECTED TOTAL GOALS");
        kpi3Title.getStyleClass().add("kpi-title");

        Label kpi3Val = new Label(String.format("%.2f", expectedGoals));
        kpi3Val.setStyle("-fx-text-fill: #2dd4bf; -fx-font-size: 32px; -fx-font-weight: 900;");

        Label kpi3Sub = new Label(expectedGoals >= 2.5 ? "Over 2.5 Goals Favored" : "Under 2.5 Goals Favored");
        kpi3Sub.getStyleClass().add("kpi-subtext");

        kpi3.getChildren().addAll(kpi3Title, kpi3Val, kpi3Sub);

        kpiContainer.getChildren().addAll(kpi1, kpi2, kpi3);

        // --- RECENT FORM SECTION ---
        VBox formContainer = new VBox(10);
        formContainer.getStyleClass().add("form-section-box");
        formContainer.setAlignment(Pos.CENTER);

        Label loadingLabel = new Label("Loading recent match form...");
        loadingLabel.setStyle("-fx-text-fill: #78999e; -fx-font-weight: 700;");
        formContainer.getChildren().add(loadingLabel);

        // --- STATS COMPARISON MATRIX ---
        GridPane grid = new GridPane();
        grid.getStyleClass().add("stats-grid");
        grid.setAlignment(Pos.CENTER);

        addVisualRow(grid, "WIN RATE", stats.getWinRateString(home), stats.getWinRateString(away), 0);
        addVisualRow(grid, "AVG GOALS", stats.getTeamGF(home), stats.getTeamGF(away), 1);
        addVisualRow(grid, "BTTS HISTORY", stats.getBTTS(home), stats.getBTTS(away), 2);

        detailLayout.getChildren().addAll(backBtn, headerBox, kpiContainer, formContainer, grid);

        this.setContent(detailLayout);
        this.setFitToWidth(true);
        this.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        final String homeFinal = home;
        final String awayFinal = away;

        new Thread(() -> {
            HistoricalDataService history = new HistoricalDataService();
            List<HistoricalDataService.MatchResult> homeF = history.getTeamForm(homeFinal);
            List<HistoricalDataService.MatchResult> awayF = history.getTeamForm(awayFinal);

            Platform.runLater(() -> {
                formContainer.getChildren().clear();
                Label formTitle = new Label("RECENT FORM (LAST 5 MATCHES)");
                formTitle.setStyle("-fx-text-fill: #d3e5e8; -fx-font-weight: 900; -fx-font-size: 11px; -fx-letter-spacing: 1.5px; -fx-padding: 0 0 4 0;");

                if (homeF.isEmpty() && awayF.isEmpty()) {
                    Label err = new Label("Form data not cached (API Rate Limit or Offline Mode)");
                    err.setStyle("-fx-text-fill: #78999e; -fx-font-size: 12px;");
                    formContainer.getChildren().addAll(formTitle, err);
                } else {
                    formContainer.getChildren().addAll(
                            formTitle,
                            createFormRow(homeFinal, homeF.stream().map(HistoricalDataService.MatchResult::res).toList()),
                            createFormRow(awayFinal, awayF.stream().map(HistoricalDataService.MatchResult::res).toList())
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
        midLabel.setAlignment(Pos.CENTER);
        midLabel.setMinWidth(130);

        g.add(homeSide, 0, row);
        g.add(midLabel, 1, row);
        g.add(awaySide, 2, row);
    }

    private VBox createStatWithBar(String value, boolean leftAlign) {
        VBox box = new VBox(5);
        box.setAlignment(leftAlign ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        Label valLab = new Label(value);
        valLab.getStyleClass().add("stats-row-value");

        StackPane barBg = new StackPane();
        barBg.getStyleClass().add("stat-bar-bg");
        barBg.setMinWidth(130);
        barBg.setMaxWidth(130);

        Region fill = new Region();
        fill.getStyleClass().add("stat-bar-fill");

        double val = 0;
        try {
            val = Double.parseDouble(value.replace("%", "").replace(",", "."));
        } catch (Exception e) {
            val = 0;
        }

        double calculatedWidth = (val <= 10) ? val * 22 : (val / 100.0) * 130;
        double finalWidth = Math.min(Math.max(calculatedWidth, 6), 130);

        fill.setMinWidth(finalWidth);
        fill.setMaxWidth(finalWidth);
        fill.setPrefWidth(finalWidth);

        barBg.getChildren().add(fill);
        StackPane.setAlignment(fill, leftAlign ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        box.getChildren().addAll(valLab, barBg);
        return box;
    }

    private HBox createFormRow(String teamName, List<String> results) {
        HBox container = new HBox(12);
        container.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(teamName.toUpperCase());
        nameLabel.setStyle("-fx-text-fill: #f1f7f8; -fx-font-weight: 800; -fx-font-size: 13px;");
        nameLabel.setMinWidth(150);
        nameLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox circles = new HBox(8);
        for (String res : results) {
            StackPane circle = new StackPane();
            circle.getStyleClass().addAll("form-circle", "form-" + res.toLowerCase());
            Label l = new Label(res);
            String textColor = switch (res.toUpperCase()) {
                case "W" -> "#00f5b8";
                case "L" -> "#f87171";
                default -> "#d3e5e8";
            };
            l.setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: 900; -fx-font-size: 11px;");
            circle.getChildren().add(l);
            circles.getChildren().add(circle);
        }

        container.getChildren().addAll(nameLabel, circles);
        return container;
    }

    private static StackPane createTeamBadge(String monogram) {
        StackPane circle = new StackPane();
        circle.getStyleClass().add("card-team-monogram");
        Label l = new Label(monogram);
        l.getStyleClass().add("card-team-monogram-label");
        circle.getChildren().add(l);
        return circle;
    }

    private VBox createMiniOdd(String caption, String value) {
        Label c = new Label(caption);
        c.setStyle("-fx-text-fill: #78999e; -fx-font-size: 10px; -fx-font-weight: 800;");

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #f1f7f8; -fx-font-size: 16px; -fx-font-weight: 900;");

        VBox b = new VBox(2, c, v);
        b.setAlignment(Pos.CENTER);
        return b;
    }
}
