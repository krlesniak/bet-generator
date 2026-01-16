package api;

import service.ConfigLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OddsApiClient {
    private static final String API_KEY = ConfigLoader.getProperty("bet.api.key");

    // &bookmakers=betclic_fr,pinnacle, betclic is being taken first
    private static final String BASE_URL = "https://api.the-odds-api.com/v4/sports/%s/odds/" +
            "?regions=eu" +
            "&markets=h2h,totals" +
            "&bookmakers=betclic_fr,pinnacle" +
            "&apiKey=" + API_KEY;

    public String getRawData(String sportKey) throws Exception {
        // will appear when out tokens are not being used
        if (CacheManager.isCacheValid(sportKey)) {
            System.out.println(">>> [CACHE] Loading " + sportKey + " from local disk (0 tokens used).");
            return CacheManager.loadCache(sportKey);
        }

        // will appear if our tokens are being taken to download the data from API
        System.out.println(">>> [API] Fetching " + sportKey + " (Betclic & Pinnacle)...");
        String fullUrl = String.format(BASE_URL, sportKey);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error: Status " + response.statusCode());
        }

        // info about remaining tokens
        String used = response.headers().firstValue("x-requests-used").orElse("?");
        String remaining = response.headers().firstValue("x-requests-remaining").orElse("?");

        System.out.println(">>> [TOKENS] Used: " + used + " | Remaining: " + remaining);

        // we save it to cache not to use our tokens many times
        CacheManager.saveCache(sportKey, response.body());

        return response.body();
    }
}