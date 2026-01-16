package gui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class HeaderComponent extends HBox {
    public HeaderComponent() {
        // -=- HEADER -=-
        // --- icon loading ---
        Image soccerImg = new Image(getClass().getResourceAsStream("/images/soccer.png"));
        ImageView soccerIcon = new ImageView(soccerImg);
        soccerIcon.setFitHeight(50);
        soccerIcon.setPreserveRatio(true);

        // --- title ---
        Label titleText = new Label("Bet Krzysztof Generator");
        titleText.getStyleClass().add("main-title");

        // --- connecting text and icon ---
        this.setSpacing(15);
        this.getChildren().addAll(soccerIcon, titleText);
        this.setAlignment(Pos.CENTER_LEFT);
    }
}