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

    public SidebarComponent() {
        super(16);
        this.getStyleClass().add("sidebar");
        this.setPrefWidth(285);
        this.setMinWidth(280);
        this.setAlignment(Pos.TOP_LEFT);

        // --- SETTINGS HEADER ---
        ImageView gearIcon = createIcon("/images/gear.png", 18);
        Label settingsLabel = new Label("SETTINGS");
        settingsLabel.getStyleClass().add("sidebar-header");

        HBox settingsBox = new HBox(8, gearIcon, settingsLabel);
        settingsBox.setAlignment(Pos.CENTER_LEFT);

        // --- TOTAL ODDS SECTION ---
        ImageView bettingIcon = createIcon("/images/betting.png", 18);
        Label totalOddsTitle = new Label("Total Odds");
        totalOddsTitle.getStyleClass().add("sidebar-section-title");

        HBox totalOddsBox = new HBox(8, bettingIcon, totalOddsTitle);
        totalOddsBox.setAlignment(Pos.CENTER_LEFT);

        oddInput.setPromptText("Target Odds (e.g. 5.0)");
        oddInput.setMaxWidth(Double.MAX_VALUE);

        // Quick Preset Chips for target odds
        HBox presetBox = new HBox(6);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        String[] presets = {"2.0x", "3.5x", "5.0x", "10x", "25x"};
        for (String p : presets) {
            Button chip = new Button(p);
            chip.getStyleClass().add("preset-chip");
            chip.setOnAction(e -> oddInput.setText(p.replace("x", "")));
            presetBox.getChildren().add(chip);
        }

        VBox oddsGroup = new VBox(6, totalOddsBox, oddInput, presetBox);

        // --- RISK LEVEL SECTION ---
        ImageView warningIcon = createIcon("/images/risk.png", 18);
        Label riskLevelTitle = new Label("Risk Level");
        riskLevelTitle.getStyleClass().add("sidebar-section-title");

        HBox riskLevelBox = new HBox(8, warningIcon, riskLevelTitle);
        riskLevelBox.setAlignment(Pos.CENTER_LEFT);

        riskCombo.getItems().addAll(RiskLevel.SAFE, RiskLevel.MEDIUM, RiskLevel.RISKY);
        riskCombo.setValue(RiskLevel.SAFE);
        riskCombo.setMaxWidth(Double.MAX_VALUE);

        // Clean professional cell text (no cheesy emojis)
        riskCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RiskLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case SAFE -> setText("Safe (High Win-Rate)");
                        case MEDIUM -> setText("Medium (Balanced Value)");
                        case RISKY -> setText("Risky (High Multiplier)");
                    }
                }
            }
        });
        riskCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(RiskLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case SAFE -> setText("Safe (High Win-Rate)");
                        case MEDIUM -> setText("Medium (Balanced Value)");
                        case RISKY -> setText("Risky (High Multiplier)");
                    }
                }
            }
        });

        VBox riskGroup = new VBox(6, riskLevelBox, riskCombo);

        // --- ACTION BUTTONS ---
        couponBtn.getStyleClass().add("btn-primary");
        couponBtn.setMaxWidth(Double.MAX_VALUE);
        couponBtn.setMinHeight(42);

        analysisBtn.getStyleClass().add("btn-secondary");
        analysisBtn.setMaxWidth(Double.MAX_VALUE);
        analysisBtn.setMinHeight(38);

        fetchBtn.getStyleClass().add("btn-secondary");
        fetchBtn.setMaxWidth(Double.MAX_VALUE);
        fetchBtn.setMinHeight(38);

        VBox buttonsBox = new VBox(10, couponBtn, analysisBtn, fetchBtn);

        // Spacer pushes AI section to bottom
        Region space = new Region();
        VBox.setVgrow(space, Priority.ALWAYS);

        // --- AI EXPERT CARD (Scorify style) ---
        VBox aiCard = new VBox(8);
        aiCard.getStyleClass().add("ai-sidebar-card");

        ImageView robotIcon = createIcon("/images/chatbot-1.png", 26);

        Label aiTitle = new Label("Gemini AI Advisor");
        aiTitle.getStyleClass().add("ai-card-title");

        Label aiSub = new Label("Coupon & risk consultation");
        aiSub.getStyleClass().add("ai-card-subtitle");

        VBox aiText = new VBox(2, aiTitle, aiSub);
        HBox aiHeader = new HBox(10, robotIcon, aiText);
        aiHeader.setAlignment(Pos.CENTER_LEFT);

        Button openChatBtn = new Button("Open AI Chat →");
        openChatBtn.getStyleClass().add("ai-card-btn");
        openChatBtn.setMaxWidth(Double.MAX_VALUE);
        openChatBtn.setOnAction(e -> aiBotBtn.fire());

        aiCard.getChildren().addAll(aiHeader, openChatBtn);

        this.getChildren().addAll(
                settingsBox,
                oddsGroup,
                riskGroup,
                new Separator(),
                buttonsBox,
                space,
                aiCard
        );
    }

    private ImageView createIcon(String path, double size) {
        try {
            Image img = new Image(getClass().getResourceAsStream(path));
            ImageView iv = new ImageView(img);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            return iv;
        } catch (Exception e) {
            return new ImageView();
        }
    }
}
