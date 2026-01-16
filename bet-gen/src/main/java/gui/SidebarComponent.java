package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.RiskLevel;

public class SidebarComponent extends VBox {
    public final TextField oddInput = new TextField("5.0");
    public final ComboBox<RiskLevel> riskCombo = new ComboBox<>();
    public final Button fetchBtn = new Button("Download Data");
    public final Button analysisBtn = new Button("Show BTTS Analysis");
    public final Button couponBtn = new Button("Generate Smart Coupon");
    public final Button aiBotBtn = new Button();
    public final Button surebetBtn = new Button("Find Surebets");

    public SidebarComponent() {
        // -=- LAYOUT -=-
        super(15);
        this.getStyleClass().add("sidebar");
        this.setPadding(new Insets(25));
        this.setPrefWidth(280);

        this.setAlignment(Pos.TOP_LEFT);

        // -=- SETTINGS and ICONS -=-

        // Settings Header
        Image gearImg = new Image(getClass().getResourceAsStream("/images/gear.png"));
        ImageView gearIcon = new ImageView(gearImg);
        gearIcon.setFitHeight(20);
        gearIcon.setPreserveRatio(true);

        gearIcon.setTranslateY(-5);

        Label settingsLabel = new Label("SETTINGS");
        settingsLabel.getStyleClass().add("sidebar-header");

        HBox settingsBox = new HBox(8, gearIcon, settingsLabel);
        settingsBox.setAlignment(Pos.CENTER_LEFT);

        // Total Odds
        Image bettingImg = new Image(getClass().getResourceAsStream("/images/betting.png"));
        ImageView bettingIcon = new ImageView(bettingImg);
        bettingIcon.setFitHeight(20);
        bettingIcon.setPreserveRatio(true);

        Label totalOddsTitle = new Label("Total Odds");
        HBox totalOddsBox = new HBox(8, bettingIcon, totalOddsTitle);
        totalOddsBox.setAlignment(Pos.CENTER_LEFT);

        oddInput.setPromptText("Target Odds");

        // Risk Level
        Image warningImg = new Image(getClass().getResourceAsStream("/images/risk.png"));
        ImageView warningIcon = new ImageView(warningImg);
        warningIcon.setFitHeight(20);
        warningIcon.setPreserveRatio(true);

        Label riskLevelTitle = new Label("Risk Level");
        HBox riskLevelBox = new HBox(8, warningIcon, riskLevelTitle);
        riskLevelBox.setAlignment(Pos.CENTER_LEFT);

        riskCombo.getItems().addAll(RiskLevel.SAFE, RiskLevel.MEDIUM, RiskLevel.RISKY);
        riskCombo.setValue(RiskLevel.SAFE);
        riskCombo.setMaxWidth(Double.MAX_VALUE);

        // -=- BUTTONS -=-
        fetchBtn.getStyleClass().add("btn-secondary");
        analysisBtn.getStyleClass().add("btn-secondary");
        couponBtn.getStyleClass().add("btn-primary");

        fetchBtn.setMaxWidth(Double.MAX_VALUE);
        analysisBtn.setMaxWidth(Double.MAX_VALUE);
        couponBtn.setMaxWidth(Double.MAX_VALUE);

        Label aiHeader = new Label("Chat Bot");
        aiHeader.getStyleClass().add("sidebar-header");

        aiHeader.setMaxWidth(Double.MAX_VALUE);
        aiHeader.setAlignment(Pos.CENTER_LEFT);

        Image robotImage = new Image(getClass().getResourceAsStream("/images/chatbot-1.png"));

        ImageView robotImageView = new ImageView(robotImage);

        // size of the icon
        robotImageView.setFitHeight(40);
        robotImageView.setFitWidth(40);
        robotImageView.setPreserveRatio(true); // leaving icon proportions

        aiBotBtn.setGraphic(robotImageView);
        aiBotBtn.getStyleClass().add("ai-button");

        VBox botBox = new VBox(5, aiHeader, aiBotBtn);
        botBox.setAlignment(Pos.TOP_LEFT);

        VBox.setMargin(fetchBtn, new Insets(28, 0, 0, 0));

        Region space = new Region();
        VBox.setVgrow(space, Priority.ALWAYS);

        this.getChildren().addAll(
                settingsBox,
                totalOddsBox,
                oddInput,
                riskLevelBox,
                riskCombo,
                new Separator(),
                fetchBtn, analysisBtn, couponBtn,
                space,
                new Separator(),
                botBox
        );
    }
}