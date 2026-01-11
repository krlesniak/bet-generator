package service;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class HistoricalDataService {
    public record MatchResult(String res, String score, String opponent, long timestamp) {}

    private final String apiKey = "cb0eba30e31501d7cc861a7e9f3b0e6e";
    private final String baseUrl = "https://v3.football.api-sports.io/";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // dynamic paths
    private static String getWorkDir() {
        String path = System.getProperty("user.home") + "/.betaidashboard/";
        java.io.File dir = new java.io.File(path);
        if (!dir.exists()) dir.mkdirs();
        return path;
    }

    private static Map<String, Integer> teamIdCache = new HashMap<>();
    private static JSONObject formCache = new JSONObject();

    private static final String ID_CACHE_FILE = getWorkDir() + "team_ids_cache.json";
    private static final String FORM_CACHE_FILE = getWorkDir() + "team_form_cache.json";;

    public HistoricalDataService() {
        loadCaches();
    }

    private void loadCaches() {
        try {
            if (Files.exists(Paths.get(ID_CACHE_FILE))) {
                JSONObject json = new JSONObject(Files.readString(Paths.get(ID_CACHE_FILE)));
                for (String key : json.keySet()) teamIdCache.put(key, json.getInt(key));
            }
            if (Files.exists(Paths.get(FORM_CACHE_FILE))) {
                formCache = new JSONObject(Files.readString(Paths.get(FORM_CACHE_FILE)));
            }
        } catch (Exception e) { System.err.println(">>> Cache load error"); }
    }

    public List<MatchResult> getTeamForm(String teamName) {
        String cacheKey = teamName.toLowerCase().trim();
        List<MatchResult> results = new ArrayList<>();

        //  check cache
        if (formCache.has(cacheKey)) {
            JSONObject entry = formCache.getJSONObject(cacheKey);
            if (System.currentTimeMillis() - entry.getLong("timestamp") < 12 * 60 * 60 * 1000) {
                JSONArray cachedData = entry.getJSONArray("data");
                for (int i = 0; i < cachedData.length(); i++) {
                    JSONObject m = cachedData.getJSONObject(i);
                    results.add(new MatchResult(m.getString("res"), m.getString("score"), m.getString("opp"), 0));
                }
                return results;
            }
        }

        try {
            int teamId = getTeamId(teamName);
            if (teamId == -1) return results;

            // download data first from 2025 then 2024
            results = fetchFixturesFromApi(teamId, 2025);
            if (results.isEmpty()) {
                System.out.println(">>> Season 2025 empty for " + teamName + ", trying 2024...");
                results = fetchFixturesFromApi(teamId, 2024);
            }

            // save to the file
            if (!results.isEmpty()) {
                JSONArray matchesToCache = new JSONArray();
                for (MatchResult mr : results) {
                    JSONObject mObj = new JSONObject();
                    mObj.put("res", mr.res()); mObj.put("score", mr.score()); mObj.put("opp", mr.opponent());
                    matchesToCache.put(mObj);
                }
                JSONObject newEntry = new JSONObject();
                newEntry.put("timestamp", System.currentTimeMillis());
                newEntry.put("data", matchesToCache);
                formCache.put(cacheKey, newEntry);
                Files.writeString(Paths.get(FORM_CACHE_FILE), formCache.toString(2));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return results;
    }

    private List<MatchResult> fetchFixturesFromApi(int teamId, int season) throws Exception {
        List<MatchResult> results = new ArrayList<>();
        String url = baseUrl + "fixtures?team=" + teamId + "&season=" + season;
        // String url = baseUrl + "fixtures?team=" + teamId + "&last=10";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url)).header("x-apisports-key", apiKey).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());

        if (json.has("response")) {
            JSONArray fixtures = json.getJSONArray("response");
            List<MatchResult> finished = new ArrayList<>();

            for (int i = 0; i < fixtures.length(); i++) {
                JSONObject f = fixtures.getJSONObject(i);
                if (f.getJSONObject("fixture").getJSONObject("status").getString("short").equals("FT")) {
                    JSONObject g = f.getJSONObject("goals");
                    JSONObject t = f.getJSONObject("teams");
                    int hG = g.isNull("home") ? 0 : g.getInt("home");
                    int aG = g.isNull("away") ? 0 : g.getInt("away");
                    boolean isHome = t.getJSONObject("home").getInt("id") == teamId;
                    String res = (hG == aG) ? "D" : ((isHome && hG > aG) || (!isHome && aG > hG)) ? "W" : "L";
                    finished.add(new MatchResult(res, hG + ":" + aG,
                            isHome ? t.getJSONObject("away").getString("name") : t.getJSONObject("home").getString("name"),
                            f.getJSONObject("fixture").getLong("timestamp")));
                }
            }
            return finished.stream()
                    .sorted(Comparator.comparingLong(MatchResult::timestamp).reversed())
                    .limit(5).collect(Collectors.toList());
        } else {
            System.err.println(">>> API Error Response: " + response.body());
        }
        return results;
    }

    private int getTeamId(String name) throws Exception {
        String cleanName = name.toLowerCase().trim();
        if (teamIdCache.containsKey(cleanName)) return teamIdCache.get(cleanName);

        // easier looking for a name
        String query = cleanName.replace(" munich", "").replace("bc", "").replace("sv", "").replace("fc", "").replace("cf", "").trim().replace(" ", "%20");
        String url = baseUrl + "teams?search=" + query;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url)).header("x-apisports-key", apiKey).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());

        if (json.has("response") && json.getJSONArray("response").length() > 0) {
            int id = json.getJSONArray("response").getJSONObject(0).getJSONObject("team").getInt("id");
            teamIdCache.put(cleanName, id);
            Files.writeString(Paths.get(ID_CACHE_FILE), new JSONObject(teamIdCache).toString());
            return id;
        }
        return -1;
    }
}