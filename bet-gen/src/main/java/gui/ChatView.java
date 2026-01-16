package gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.BetOption;
import service.GeminiChatService;
import java.util.List;

public class ChatView extends VBox {
    private final GeminiChatService chatService = new GeminiChatService();

    public ChatView(StackPane stack, VBox originalView, List<BetOption> lastGeneratedCoupon) {
        super(20);
        this.getStyleClass().add("detail-container");
        this.setPadding(new Insets(30));

        // --- HEADER AND BACK BUTTON  ---
        Button backBtn = new Button("← BACK TO DASHBOARD");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> stack.getChildren().setAll(originalView));

        VBox headerBox = new VBox(5);
        headerBox.getStyleClass().add("detail-header-box");
        Label titleLabel = new Label("BET EXPERT AI ANALYSIS");
        titleLabel.getStyleClass().add("detail-title-text");
        headerBox.getChildren().add(titleLabel);

        // --- CHAT TEXT AREA ---
        TextArea chatArea = new TextArea("Cześć! Jestem Twoim ekspertem AI. Przeanalizuję dla Ciebie ostatnio wygenerowany kupon" +
                " oraz odpowiem na pytania dotyczące meczy.\n");
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(400);
        chatArea.getStyleClass().add("chat-text");

        // --- QUESTION AREA ---
        TextField userInput = new TextField();
        userInput.setPromptText("Napisz wiadomość i naciśnij ENTER...");
        userInput.getStyleClass().add("text-field");

        // --- QUESTION WITH ENETR LOGIC ---
        userInput.setOnAction(e -> {
            String question = userInput.getText();
            if (question.isEmpty()) return;

            chatArea.appendText("\n\nTY: " + question);
            userInput.clear();

            // preparing data about last coupon
            StringBuilder context = new StringBuilder();
            if (lastGeneratedCoupon.isEmpty()) {
                context.append("Użytkownik nie wygenerował jeszcze kuponu.");
            } else {
                context.append("Aktualny kupon do analizy:\n");
                for (BetOption b : lastGeneratedCoupon) {
                    context.append("- ").append(b.getName()).append(" | Kurs: ").append(b.getPrice()).append("\n");
                }
            }

            // loading expert's answer
            chatArea.appendText("\n\nEKSPERT AI: Pisze...");

            new Thread(() -> {
                String aiAnswer = chatService.getAiResponse(question, context.toString());
                Platform.runLater(() -> {
                    String currentText = chatArea.getText();
                    chatArea.setText(currentText.substring(0, currentText.lastIndexOf("Pisze...")) + aiAnswer);
                    chatArea.setScrollTop(Double.MAX_VALUE); // automatic scrolling down
                });
            }).start();
        });

        this.getChildren().addAll(backBtn, headerBox, chatArea, userInput);
    }
}