package gui;

import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import javafx.util.Callback;
import javafx.util.Duration;

public class MatchCell implements Callback<ListView<String>, ListCell<String>> {
    @Override
    public ListCell<String> call(ListView<String> lv) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
                // --- SECTION HEADER ---
                else if (item.startsWith("HEADER:")) {
                    String title = item.replace("HEADER:", "").trim();

                    HBox headerBox = new HBox(10);
                    headerBox.getStyleClass().add("coupon-header-box");
                    headerBox.setAlignment(Pos.CENTER_LEFT);

                    Label bar = new Label("│");
                    bar.setStyle("-fx-text-fill: #2dd4bf; -fx-font-size: 16px; -fx-font-weight: 900;");

                    Label headerLabel = new Label(title);
                    headerLabel.getStyleClass().add("header-text");

                    headerBox.getChildren().addAll(bar, headerLabel);
                    setGraphic(headerBox);
                    setText(null);
                }
                // --- SCORIFY BET SLIP CARD ---
                else if (item.startsWith("BET_CARD:")) {
                    String content = item.replace("BET_CARD:", "");
                    String[] parts = content.split("\\|");
                    String fullInfo = parts[0];
                    String oddValue = (parts.length > 1) ? parts[1].trim() : "1.00";

                    // Parse team names
                    int dateStart = fullInfo.indexOf("(");
                    String matchTeams = (dateStart != -1) ? fullInfo.substring(0, dateStart).trim() : fullInfo;
                    String[] teams = matchTeams.split(" vs ");
                    String homeTeam = teams[0].trim();
                    String awayTeam = (teams.length > 1) ? teams[1].trim() : "";

                    // Parse date
                    String dateStr = "";
                    if (dateStart != -1 && fullInfo.indexOf(")") > dateStart) {
                        dateStr = fullInfo.substring(dateStart + 1, fullInfo.indexOf(")")).trim();
                    }

                    // Parse pick & flags
                    String pickStr = "";
                    boolean isSuperSafe = fullInfo.contains("{super safe}");
                    boolean isHighGoals = fullInfo.contains("{high goals}");

                    int bracketStart = fullInfo.indexOf("[");
                    int bracketEnd = fullInfo.indexOf("]");
                    if (bracketStart != -1 && bracketEnd > bracketStart) {
                        pickStr = fullInfo.substring(bracketStart + 1, bracketEnd)
                                .replace("{super safe}", "")
                                .replace("{high goals}", "")
                                .trim();
                    }

                    // Parse BTTS info
                    String bttsStr = "";
                    int bttsIndex = fullInfo.indexOf("[BTTS:");
                    if (bttsIndex != -1) {
                        int bttsEnd = fullInfo.indexOf("]", bttsIndex);
                        if (bttsEnd != -1) {
                            bttsStr = fullInfo.substring(bttsIndex + 1, bttsEnd).trim();
                        }
                    }

                    // Main card container (VBox in Scorify card anatomy)
                    VBox card = new VBox(10);
                    card.getStyleClass().add("coupon-card");

                    // 1. Top row: Centered Date/Time
                    HBox topRow = new HBox();
                    topRow.setAlignment(Pos.CENTER);
                    Label timeLabel = new Label(!dateStr.isEmpty() ? dateStr : "Upcoming Match");
                    timeLabel.getStyleClass().add("card-time-header");
                    topRow.getChildren().add(timeLabel);

                    // 2. Middle row: Team 1 (Monogram + Name)  -- Vs --  Team 2 (Name + Monogram)
                    HBox matchRow = new HBox(12);
                    matchRow.setAlignment(Pos.CENTER);

                    // Home side
                    StackPane homeBadge = createTeamBadge(getTeamMonogram(homeTeam));
                    Label homeLabel = new Label(homeTeam);
                    homeLabel.getStyleClass().add("card-team-name");
                    HBox homeBox = new HBox(10, homeBadge, homeLabel);
                    homeBox.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(homeBox, Priority.ALWAYS);

                    // Center Vs
                    Label vsLabel = new Label("Vs");
                    vsLabel.getStyleClass().add("card-vs");

                    // Away side
                    Label awayLabel = new Label(awayTeam);
                    awayLabel.getStyleClass().add("card-team-name");
                    StackPane awayBadge = createTeamBadge(getTeamMonogram(awayTeam));
                    HBox awayBox = new HBox(10, awayLabel, awayBadge);
                    awayBox.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setHgrow(awayBox, Priority.ALWAYS);

                    matchRow.getChildren().addAll(homeBox, vsLabel, awayBox);

                    // 3. Bottom row: Pick badge + stats + odds chip
                    HBox bottomRow = new HBox(10);
                    bottomRow.setAlignment(Pos.CENTER_LEFT);

                    if (!pickStr.isEmpty()) {
                        Label pickBadge = new Label("PICK: " + pickStr.toUpperCase());
                        pickBadge.getStyleClass().add("card-pick-badge");
                        bottomRow.getChildren().add(pickBadge);
                    }

                    if (!bttsStr.isEmpty()) {
                        Label bttsPill = new Label(bttsStr);
                        bttsPill.getStyleClass().add("card-meta-pill");
                        bottomRow.getChildren().add(bttsPill);
                    }

                    if (isSuperSafe) {
                        Label safeTag = new Label("HIGH WIN-RATE");
                        safeTag.getStyleClass().add("card-tag-badge");
                        bottomRow.getChildren().add(safeTag);
                    } else if (isHighGoals) {
                        Label goalsTag = new Label("HIGH GOALS");
                        goalsTag.getStyleClass().add("card-tag-badge");
                        bottomRow.getChildren().add(goalsTag);
                    }

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // Odds Chip on Right
                    VBox oddBox = new VBox(1);
                    oddBox.getStyleClass().add("card-odd-box");

                    Label oddCaption = new Label("ODDS");
                    oddCaption.getStyleClass().add("odd-caption");

                    Label oddValueLabel = new Label("@ " + oddValue);
                    oddValueLabel.getStyleClass().add("card-odd-val");

                    oddBox.getChildren().addAll(oddCaption, oddValueLabel);

                    bottomRow.getChildren().addAll(spacer, oddBox);

                    card.getChildren().addAll(topRow, matchRow, bottomRow);

                    // Smooth hover transition
                    setupHoverAnimation(card);

                    setGraphic(card);
                    setText(null);
                }
                // --- SCORIFY BTTS ANALYSIS CARD ---
                else if (item.startsWith("ANALYSIS_CARD:")) {
                    String raw = item.replace("ANALYSIS_CARD:", "").trim();
                    String[] parts = raw.split("\\|");

                    String matchPart = (parts.length > 0) ? parts[0].trim() : "";
                    String bttsPart = (parts.length > 1) ? parts[1].trim() : "BTTS: 50%";
                    String goalsPart = (parts.length > 2) ? parts[2].trim() : "Exp. Goals: 2.50";
                    String statusPart = (parts.length > 3) ? parts[3].trim() : "MEDIUM";

                    // Extract date
                    String dateStr = "";
                    if (matchPart.startsWith("[")) {
                        int end = matchPart.indexOf("]");
                        if (end != -1) {
                            dateStr = matchPart.substring(1, end).trim();
                            matchPart = matchPart.substring(end + 1).trim();
                        }
                    }

                    // Extract teams
                    String[] teams = matchPart.split(" vs ");
                    String homeTeam = teams[0].trim();
                    String awayTeam = (teams.length > 1) ? teams[1].trim() : "";

                    // Extract BTTS percentage
                    int bttsPercent = 50;
                    try {
                        String numOnly = bttsPart.replaceAll("[^0-9]", "");
                        if (!numOnly.isEmpty()) bttsPercent = Integer.parseInt(numOnly);
                    } catch (Exception ignored) {}

                    VBox card = new VBox(10);
                    card.getStyleClass().add("analysis-card");

                    // Top row: Date
                    HBox topRow = new HBox();
                    topRow.setAlignment(Pos.CENTER);
                    Label timeLabel = new Label(!dateStr.isEmpty() ? dateStr : "Match Analysis");
                    timeLabel.getStyleClass().add("card-time-header");
                    topRow.getChildren().add(timeLabel);

                    // Matchup row
                    HBox matchRow = new HBox(12);
                    matchRow.setAlignment(Pos.CENTER);

                    StackPane homeBadge = createTeamBadge(getTeamMonogram(homeTeam));
                    Label homeLabel = new Label(homeTeam);
                    homeLabel.getStyleClass().add("card-team-name");
                    HBox homeBox = new HBox(10, homeBadge, homeLabel);
                    homeBox.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(homeBox, Priority.ALWAYS);

                    Label vsLabel = new Label("Vs");
                    vsLabel.getStyleClass().add("card-vs");

                    Label awayLabel = new Label(awayTeam);
                    awayLabel.getStyleClass().add("card-team-name");
                    StackPane awayBadge = createTeamBadge(getTeamMonogram(awayTeam));
                    HBox awayBox = new HBox(10, awayLabel, awayBadge);
                    awayBox.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setHgrow(awayBox, Priority.ALWAYS);

                    matchRow.getChildren().addAll(homeBox, vsLabel, awayBox);

                    // Bottom analytics row
                    HBox bottomRow = new HBox(12);
                    bottomRow.setAlignment(Pos.CENTER_LEFT);

                    Label bttsLabel = new Label("BTTS: " + bttsPercent + "%");
                    bttsLabel.setStyle("-fx-text-fill: #2dd4bf; -fx-font-weight: 800; -fx-font-size: 11px;");

                    StackPane miniBarBg = new StackPane();
                    miniBarBg.setStyle("-fx-background-color: #0b1517; -fx-background-radius: 3px; -fx-min-height: 5px; -fx-max-height: 5px; -fx-min-width: 90px; -fx-max-width: 90px;");

                    Region fill = new Region();
                    String barColor = bttsPercent >= 60 ? "#00f5b8" : (bttsPercent <= 45 ? "#ef4444" : "#f59e0b");
                    fill.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 3px; -fx-min-height: 5px; -fx-max-height: 5px;");
                    double barWidth = Math.max(6, (bttsPercent / 100.0) * 90);
                    fill.setMinWidth(barWidth);
                    fill.setMaxWidth(barWidth);

                    miniBarBg.getChildren().add(fill);
                    StackPane.setAlignment(fill, Pos.CENTER_LEFT);

                    Label xgLabel = new Label(goalsPart);
                    xgLabel.getStyleClass().add("card-meta-pill");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    String riskText = statusPart.toUpperCase();
                    String displayRisk = switch (riskText) {
                        case "HIGH" -> "HIGH PROBABILITY";
                        case "LOW" -> "LOW PROBABILITY";
                        default -> "BALANCED";
                    };

                    Label riskBadge = new Label(displayRisk);
                    riskBadge.getStyleClass().addAll("risk-badge", "badge-" + riskText.toLowerCase());

                    bottomRow.getChildren().addAll(bttsLabel, miniBarBg, xgLabel, spacer, riskBadge);

                    card.getChildren().addAll(topRow, matchRow, bottomRow);

                    // Smooth hover transition
                    setupHoverAnimation(card);

                    setGraphic(card);
                    setText(null);
                }
                // --- SYSTEM NOTIFICATIONS ---
                else if (item.startsWith("INFO:") || item.startsWith("WARN:")) {
                    boolean isInfo = item.startsWith("INFO:");
                    String message = item.replace(isInfo ? "INFO:" : "WARN:", "").trim();

                    HBox statusBox = new HBox(12);
                    statusBox.setAlignment(Pos.CENTER_LEFT);
                    statusBox.getStyleClass().add(isInfo ? "download-info" : "download-warn");

                    Label tag = new Label(isInfo ? "[INFO]" : "[WARNING]");
                    tag.setStyle(isInfo ? "-fx-text-fill: #2dd4bf; -fx-font-weight: 900; -fx-font-size: 11px;" : "-fx-text-fill: #ef4444; -fx-font-weight: 900; -fx-font-size: 11px;");

                    Label msgLabel = new Label(message);
                    msgLabel.getStyleClass().add("download-message-text");

                    statusBox.getChildren().addAll(tag, msgLabel);
                    setGraphic(statusBox);
                    setText(null);
                }
                // --- DEFAULT LOG LINE ---
                else {
                    setGraphic(null);
                    setText(item);
                    setStyle("-fx-text-fill: #78999e; -fx-font-size: 12px; -fx-padding: 4 10;");
                }
            }
        };
    }

    private static StackPane createTeamBadge(String monogram) {
        StackPane circle = new StackPane();
        circle.getStyleClass().add("card-team-monogram");
        Label l = new Label(monogram);
        l.getStyleClass().add("card-team-monogram-label");
        circle.getChildren().add(l);
        return circle;
    }

    public static String getTeamMonogram(String name) {
        if (name == null || name.trim().isEmpty()) return "TM";
        String clean = name.trim().replaceAll("(?i)^(FC|CF|AC|AS|SC|HNK|REAL|SPORTING|INTER)\\s+", "").trim();
        String[] words = clean.split("\\s+");
        if (words.length >= 2) {
            String w1 = words[0].substring(0, Math.min(2, words[0].length()));
            String w2 = words[1].substring(0, Math.min(1, words[1].length()));
            return (w1 + w2).toUpperCase();
        }
        return clean.substring(0, Math.min(3, clean.length())).toUpperCase();
    }

    private static void setupHoverAnimation(VBox card) {
        card.setOnMouseEntered(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(120), card);
            tt.setToY(-2);
            tt.play();
        });
        card.setOnMouseExited(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(120), card);
            tt.setToY(0);
            tt.play();
        });
    }
}
