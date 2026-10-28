package gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import model.BetOption;
import service.GeminiChatService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatView extends VBox {
    private final GeminiChatService chatService = new GeminiChatService();
    private final WebView webView = new WebView();

    public ChatView(StackPane stack, VBox originalView, List<BetOption> lastGeneratedCoupon) {
        super(14);
        this.getStyleClass().add("detail-container");
        this.setPadding(new Insets(20));

        // Top Navigation Bar
        Button backBtn = new Button("← Back to Dashboard");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> BetGui.animateTransition(stack, originalView));

        Label aiAdvisorTitle = new Label("GEMINI AI BETTING ADVISOR");
        aiAdvisorTitle.setStyle("-fx-text-fill: #2dd4bf; -fx-font-weight: 900; -fx-font-size: 13px; -fx-letter-spacing: 1.2px;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Label contextPill = new Label(lastGeneratedCoupon.isEmpty() ? "No Coupon Active" : "Active Coupon (" + lastGeneratedCoupon.size() + " Picks)");
        contextPill.setStyle(lastGeneratedCoupon.isEmpty()
                ? "-fx-background-color: #261a09; -fx-border-color: #f59e0b; -fx-text-fill: #fbbf24; -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-radius: 6; -fx-font-size: 10px; -fx-font-weight: 800;"
                : "-fx-background-color: #072620; -fx-border-color: #10b981; -fx-text-fill: #00f5b8; -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-radius: 6; -fx-font-size: 10px; -fx-font-weight: 800;");

        HBox navRow = new HBox(12, backBtn, topSpacer, aiAdvisorTitle, contextPill);
        navRow.setAlignment(Pos.CENTER_LEFT);

        // Scorify style WebView
        String styleCss = "<style>" +
                "body { " +
                "  background: #080e10; " +
                "  color: #d3e5e8; " +
                "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                "  padding: 16px 20px; " +
                "  line-height: 1.6; " +
                "  margin: 0; " +
                "}" +
                "::-webkit-scrollbar { width: 6px; } " +
                "::-webkit-scrollbar-thumb { background: #1a3134; border-radius: 8px; } " +
                ".message-container { " +
                "  display: flex; " +
                "  flex-direction: column; " +
                "  gap: 18px; " +
                "  max-width: 850px; " +
                "  margin: 0 auto; " +
                "}" +
                ".user-box { " +
                "  align-self: flex-end; " +
                "  background: linear-gradient(145deg, #101c1f 0%, #0a1315 100%); " +
                "  padding: 12px 18px; " +
                "  border-radius: 14px; " +
                "  border-bottom-right-radius: 2px; " +
                "  border: 1px solid #1e383b; " +
                "  color: #f1f7f8; " +
                "  font-size: 13.5px; " +
                "  max-width: 75%; " +
                "  box-shadow: 0 4px 12px rgba(0,0,0,0.4); " +
                "}" +
                ".ai-box { " +
                "  align-self: flex-start; " +
                "  background: linear-gradient(145deg, #0e1719 0%, #091012 100%); " +
                "  border: 1px solid #1a2f32; " +
                "  border-left: 3px solid #2dd4bf; " +
                "  border-radius: 14px; " +
                "  padding: 16px 20px; " +
                "  max-width: 90%; " +
                "  font-size: 13.5px; " +
                "  box-shadow: 0 4px 16px rgba(0,0,0,0.4); " +
                "}" +
                ".ai-label { " +
                "  color: #2dd4bf; " +
                "  font-weight: 800; " +
                "  font-size: 11px; " +
                "  text-transform: uppercase; " +
                "  letter-spacing: 1.2px; " +
                "  margin-bottom: 8px; " +
                "  display: block; " +
                "}" +
                "h3 { color: #ffffff; margin: 12px 0 8px 0; font-size: 15px; border-bottom: 1px solid #1a3134; padding-bottom: 4px; }" +
                "b { color: #00f5b8; font-weight: 700; }" +
                "li { margin-bottom: 6px; list-style-type: none; border-left: 2px solid #2dd4bf; padding-left: 8px; }" +
                "</style>";

        String baseHtml = "<html><head>" + styleCss + "</head><body>" +
                "<div id='chat-content' class='message-container'>" +
                "<div class='ai-box'><span class='ai-label'>Gemini Sports Advisor</span>" +
                "Witaj! Jestem Twoim analitykiem bukmacherskim AI. Mogę ocenić ryzyko wygenerowanego kuponu, przeanalizować pojedyncze mecze, sprawdzić value bet lub zaproponować bezpieczniejsze alternatywy. Zadaj pytanie poniżej lub kliknij jedną z szybkich opcji.</div>" +
                "</div><div id='scroll-anchor'></div></body></html>";

        webView.getEngine().loadContent(baseHtml);
        webView.setPrefHeight(450);
        VBox.setVgrow(webView, Priority.ALWAYS);

        // Quick Suggestion Chips
        HBox suggestionRow = new HBox(8);
        suggestionRow.setAlignment(Pos.CENTER_LEFT);

        Label promptHint = new Label("Quick Prompts:");
        promptHint.setStyle("-fx-text-fill: #78999e; -fx-font-size: 11px; -fx-font-weight: 700;");

        TextField userInput = new TextField();
        userInput.setPromptText("Ask AI sports advisor a question...");
        userInput.getStyleClass().add("text-field");
        HBox.setHgrow(userInput, Priority.ALWAYS);

        Runnable sendAction = () -> {
            String question = userInput.getText().trim();
            if (question.isEmpty()) return;
            userInput.clear();

            addMessageToHtml("user", question);

            StringBuilder context = new StringBuilder();
            if (lastGeneratedCoupon.isEmpty()) {
                context.append("Brak kuponu.");
            } else {
                context.append("Aktualny kupon:\n");
                for (BetOption b : lastGeneratedCoupon) {
                    context.append("- ").append(b.getName()).append(" (Kurs: ").append(b.getPrice()).append(")\n");
                }
            }

            new Thread(() -> {
                String aiAnswer = chatService.getAiResponse(question, context.toString());
                String formatted = formatMarkdownToHtml(aiAnswer);
                Platform.runLater(() -> streamAiResponse(formatted));
            }).start();
        };

        String[] quickPrompts = {
                "Zaaudytuj ryzyko tego kuponu",
                "Który mecz ma największe prawdopodobieństwo BTTS?",
                "Wskaż najsłabszy typ w kuponie"
        };

        suggestionRow.getChildren().add(promptHint);
        for (String q : quickPrompts) {
            Button chip = new Button(q);
            chip.getStyleClass().add("preset-chip");
            chip.setOnAction(e -> {
                userInput.setText(q);
                sendAction.run();
            });
            suggestionRow.getChildren().add(chip);
        }

        userInput.setOnAction(e -> sendAction.run());

        Button sendBtn = new Button("Send →");
        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setOnAction(e -> sendAction.run());

        HBox inputRow = new HBox(10, userInput, sendBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        this.getChildren().addAll(navRow, webView, suggestionRow, inputRow);
    }

    private void addMessageToHtml(String role, String text) {
        String divClass = role.equals("user") ? "user-box" : "ai-box";
        String label = role.equals("user") ? "" : "<span class='ai-label'>Gemini Sports Advisor</span>";

        String script = String.format(
                "var container = document.getElementById('chat-content');" +
                        "var div = document.createElement('div');" +
                        "div.className = '%s';" +
                        "div.innerHTML = \"%s%s\";" +
                        "container.appendChild(div);" +
                        "window.scrollTo(0, document.body.scrollHeight);",
                divClass, label, text.replace("\"", "\\\"").replace("\n", "<br>")
        );
        webView.getEngine().executeScript(script);
    }

    private void streamAiResponse(String fullHtml) {
        String uniqueId = "ai_" + System.currentTimeMillis();
        webView.getEngine().executeScript(
                "var container = document.getElementById('chat-content');" +
                        "var div = document.createElement('div');" +
                        "div.className = 'ai-box';" +
                        "div.innerHTML = \"<span class='ai-label'>Gemini Sports Advisor</span><span id='" + uniqueId + "'></span>\";" +
                        "container.appendChild(div);"
        );

        String[] parts = fullHtml.split("(?=<)|(?<=>)");
        AtomicInteger index = new AtomicInteger(0);

        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(30), e -> {
            if (index.get() < parts.length) {
                String part = parts[index.getAndIncrement()];
                String escapedPart = part.replace("\"", "\\\"").replace("\n", "");
                webView.getEngine().executeScript(
                        "document.getElementById('" + uniqueId + "').innerHTML += \"" + escapedPart + "\";" +
                                "window.scrollTo(0, document.body.scrollHeight);"
                );
            }
        });

        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(parts.length);
        timeline.play();
    }

    private String formatMarkdownToHtml(String markdown) {
        return markdown
                .replaceAll("(?m)^### (.*)$", "<h3>$1</h3>")
                .replaceAll("(?m)^- (.*)$", "<li>$1</li>")
                .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                .replace("\n", "<br>");
    }
}
