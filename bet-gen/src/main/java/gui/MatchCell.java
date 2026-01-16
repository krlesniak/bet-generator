package gui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

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

                    subDetails = subDetails.replace("(", "").replace(")", "   |   ").replace("[", "").replace("]", "   |   ");
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
        };
    }
}