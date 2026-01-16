package service;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiChatService {
    private final String apiKey = ConfigLoader.getProperty("gemini.api.key");
    // 2-5-flash model
    private final String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String getAiResponse(String userPrompt, String context) {
        try {
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            String fullPrompt = "Jesteś ekspertem bukmacherskim. Niech twoja odpowiedz nie bedzie zbyt dluga," +
                    "ma byc czytelna, łatwa do przeanalizowania i konkretnie napisana. Oto dane o aktualnym kuponie: \n" + context
                    + "\n\nUżytkownik pyta: " + userPrompt;

            part.put("text", fullPrompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(">>> RAW API RESPONSE: " + response.body());

            JSONObject jsonResponse = new JSONObject(response.body());

            // checking candidates
            if (jsonResponse.has("candidates")) {
                return jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            } else if (jsonResponse.has("error")) {
                // error
                return "Błąd API Google: " + jsonResponse.getJSONObject("error").getString("message");
            } else {
                return "Nieoczekiwana odpowiedź serwera. Sprawdź konsolę IntelliJ.";
            }

        } catch (Exception e) {
            return "Błąd techniczny: " + e.getMessage();
        }
    }
}