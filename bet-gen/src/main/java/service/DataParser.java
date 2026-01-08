package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.BetOption;
import model.Match;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class DataParser {

    private final ObjectMapper objectMapper;

    public DataParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public List<Match> parseMatches(String jsonRaw) {
        List<Match> matchList = new ArrayList<>();
        try {
            // checking error in API
            if (jsonRaw.contains("\"message\"")) {
                System.err.println("Warning: The cache file contains an API error. Please delete the .json files and try again..");
                return matchList;
            }

            JsonNode rootArray = objectMapper.readTree(jsonRaw);
            if (!rootArray.isArray()) return matchList;

            for (JsonNode matchNode : rootArray) {
                String homeTeam = matchNode.path("home_team").asText("");
                String awayTeam = matchNode.path("away_team").asText("");
                String sportTitle = matchNode.path("sport_title").asText("Unknown");
                String dateStr = matchNode.path("commence_time").asText("");

                if (homeTeam.isEmpty() || awayTeam.isEmpty() || dateStr.isEmpty()) continue;

                Match match = new Match();
                match.setHomeTeam(homeTeam);
                match.setAwayTeam(awayTeam);
                match.setSportTitle(sportTitle);

                try {
                    match.setTime(ZonedDateTime.parse(dateStr).toLocalDateTime());
                } catch (Exception e) {
                    match.setTime(LocalDateTime.now());
                }

                List<BetOption> bets = extractBestOdds(matchNode);
                match.setOdds(bets);

                matchList.add(match);
            }
        } catch (Exception e) {
            System.err.println("Parsing error: " + e.getMessage());
        }
        return matchList;
    }

    private List<BetOption> extractBestOdds(JsonNode matchNode) {
        List<BetOption> options = new ArrayList<>();
        JsonNode bookmakers = matchNode.get("bookmakers");

        if (bookmakers == null || !bookmakers.isArray()) return options;

        // flags
        boolean foundH2H = false;
        boolean foundTotals = false;

        // we search through betclic and pinnacle
        for (JsonNode bookie : bookmakers) {
            JsonNode markets = bookie.get("markets");
            if (markets == null || !markets.isArray()) continue;

            for (JsonNode market : markets) {
                String marketKey = market.get("key").asText();

                // if we found h2h in fisrt web
                if (marketKey.equals("h2h") && foundH2H) continue;
                // if we found total goals
                if (marketKey.equals("totals") && foundTotals) continue;

                JsonNode outcomes = market.get("outcomes");
                if (outcomes != null && outcomes.isArray()) {
                    for (JsonNode outcome : outcomes) {
                        String name = outcome.get("name").asText();
                        double price = outcome.get("price").asDouble();
                        Double point = outcome.has("point") ? outcome.get("point").asDouble() : null;

                        options.add(new BetOption(name, price, point));
                    }

                    // found
                    if (marketKey.equals("h2h")) foundH2H = true;
                    if (marketKey.equals("totals")) foundTotals = true;
                }
            }
        }
        return options;
    }
}