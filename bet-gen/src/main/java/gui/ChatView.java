package gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
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
        super(15);
        this.getStyleClass().add("detail-container");
        this.setPadding(new Insets(20));

        // --- GEMINI UI STYLE ---
        String styleCss = "<style>" +
                "body { " +
                "  background: radial-gradient(circle at top, #1a1a1c 0%, #050505 100%); " +
                "  color: #e3e3e3; " +
                "  font-family: 'Inter', 'Segoe UI', sans-serif; " +
                "  padding: 20px; " +
                "  line-height: 1.6; " +
                "  scroll-behavior: smooth; " +
                "}" +
                "::-webkit-scrollbar { width: 6px; } " +
                "::-webkit-scrollbar-thumb { background: #333; border-radius: 10px; } " +
                ".message-container { " +
                "  display: flex; " +
                "  flex-direction: column; " +
                "  gap: 28px; " +
                "  max-width: 850px; " +
                "  margin: 0 auto; " +
                "}" +
                ".user-box { " +
                "  align-self: flex-end; " +
                "  background: linear-gradient(135deg, #2b2b2d 0%, #1c1c1e 100%); " +
                "  padding: 14px 22px; " +
                "  border-radius: 18px; " +
                "  max-width: 75%; " +
                "  border-bottom-right-radius: 4px; " +
                "  border: 1px solid #3a3a3c; " +
                "  color: #ffffff; " +
                "  font-size: 0.95em; " +
                "  box-shadow: 0 4px 15px rgba(0,0,0,0.4); " +
                "}" +
                ".ai-box { " +
                "  align-self: flex-start; " +
                "  max-width: 95%; " +
                "  font-size: 1.05em; " +
                "  border-left: 2px solid #8ab4f8; " +
                "  padding-left: 15px; " +
                "}" +
                ".ai-label { " +
                "  color: #8ab4f8; " +
                "  font-weight: 600; " +
                "  font-size: 0.85em; " +
                "  text-transform: uppercase; " +
                "  margin-bottom: 10px; " +
                "  display: block; " +
                "  letter-spacing: 1.2px; " +
                "  text-shadow: 0 0 10px rgba(138, 180, 248, 0.3); " +
                "}" +
                "h3 { color: #ffffff; margin: 18px 0 12px 0; font-size: 1.25em; border-bottom: 1px solid #333; padding-bottom: 5px; }" +
                "b { color: #8ab4f8; font-weight: 600; }" +
                "li { margin-bottom: 10px; list-style-type: none; border-left: 1px solid #333; padding-left: 10px; }" +
                "hr { border: 0; border-top: 1px solid #2d2d2d; margin: 25px 0; }" +
                "</style>";

        String baseHtml = "<html><head>" + styleCss + "</head><body>" +
                "<div id='chat-content' class='message-container'>" +
                "<div class='ai-box'><span class='ai-label'>Gemini Expert</span>Witaj! W czym mogę Ci dzisiaj pomóc?</div>" +
                "</div><div id='scroll-anchor'></div></body></html>";

        webView.getEngine().loadContent(baseHtml);
        webView.setPrefHeight(500);
        VBox.setVgrow(webView, Priority.ALWAYS);

        Button backBtn = new Button("← DASHBOARD");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> stack.getChildren().setAll(originalView));

        TextField userInput = new TextField();
        userInput.setPromptText("Zadaj pytanie ekspertowi...");
        userInput.getStyleClass().add("text-field");

        userInput.setOnAction(e -> {
            String question = userInput.getText();
            if (question.isEmpty()) return;
            userInput.clear();

            // user message
            addMessageToHtml("user", question);

            StringBuilder context = new StringBuilder();
            if (lastGeneratedCoupon.isEmpty()) {
                context.append("Brak kuponu.");
            } else {
                context.append("Dane kuponu:\n");
                for (BetOption b : lastGeneratedCoupon) {
                    context.append("- ").append(b.getName()).append(" (Kurs: ").append(b.getPrice()).append(")\n");
                }
            }

            new Thread(() -> {
                String aiAnswer = chatService.getAiResponse(question, context.toString());
                String formatted = formatMarkdownToHtml(aiAnswer);

                // typing effect
                Platform.runLater(() -> streamAiResponse(formatted));
            }).start();
        });

        this.getChildren().addAll(backBtn, webView, userInput);
    }

    private void addMessageToHtml(String role, String text) {
        String divClass = role.equals("user") ? "user-box" : "ai-box";
        String label = role.equals("user") ? "" : "<span class='ai-label'>Gemini Expert</span>";

        String script = String.format(
                "var container = document.getElementById('chat-content');" +
                        "var div = document.createElement('div');" +
                        "div.className = '%s';" +
                        "div.innerHTML = \"%s%s\";" +
                        "container.appendChild(div);" +
                        "window.scrollTo(0, document.body.scrollHeight);",
                divClass, label, text.replace("\"", "\\\"").replace("\n", "")
        );
        webView.getEngine().executeScript(script);
    }

    private void streamAiResponse(String fullHtml) {
        // box for AI answer
        String uniqueId = "ai_" + System.currentTimeMillis();
        webView.getEngine().executeScript(
                "var container = document.getElementById('chat-content');" +
                        "var div = document.createElement('div');" +
                        "div.className = 'ai-box';" +
                        "div.innerHTML = \"<span class='ai-label'>Gemini Expert</span><span id='" + uniqueId + "'></span>\";" +
                        "container.appendChild(div);"
        );

        // writing simulation
        String[] parts = fullHtml.split("(?=<)|(?<=>)");
        AtomicInteger index = new AtomicInteger(0);

        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(70), e -> {
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