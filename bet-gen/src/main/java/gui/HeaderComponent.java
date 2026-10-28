package gui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class HeaderComponent extends HBox {
    public HeaderComponent() {
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(16);
        this.setMaxWidth(Double.MAX_VALUE);

        ImageView soccerIcon;
        try {
            Image soccerImg = new Image(getClass().getResourceAsStream("/images/soccer.png"));
            soccerIcon = new ImageView(soccerImg);
            soccerIcon.setFitHeight(34);
            soccerIcon.setPreserveRatio(true);
        } catch (Exception e) {
            soccerIcon = new ImageView();
        }

        Label titleText = new Label("BET GENERATOR");
        titleText.getStyleClass().add("main-title");

        Label badgeAI = new Label("SPORTS AI");
        badgeAI.getStyleClass().add("brand-badge");

        HBox titleRow = new HBox(10, titleText, badgeAI);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label("SPORTS PREDICTIVE MODELING & VALUE ANALYSIS");
        subtitle.getStyleClass().add("main-subtitle");

        VBox brandBox = new VBox(2, titleRow, subtitle);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        HBox leftBrand = new HBox(12, soccerIcon, brandBox);
        leftBrand.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label leagueBadge = new Label("8 LEAGUES ACTIVE");
        leagueBadge.getStyleClass().add("header-pill");

        Label dot = new Label("●");
        dot.getStyleClass().add("live-dot");

        Label liveText = new Label("MARKET READY");
        liveText.setStyle("-fx-text-fill: #e8f3f4; -fx-font-size: 11px; -fx-font-weight: 800;");

        HBox liveStatusBox = new HBox(6, dot, liveText);
        liveStatusBox.setAlignment(Pos.CENTER);
        liveStatusBox.getStyleClass().add("header-pill");

        HBox rightBar = new HBox(10, leagueBadge, liveStatusBox);
        rightBar.setAlignment(Pos.CENTER_RIGHT);

        this.getChildren().addAll(leftBrand, spacer, rightBar);
    }
}
