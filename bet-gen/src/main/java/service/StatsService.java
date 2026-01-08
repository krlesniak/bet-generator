package service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StatsService {

    public record TeamStats(double avgTotalGoals, double bttsRate, double winRate) {}

    private final Map<String, TeamStats> statsMap = new HashMap<>();
    private static final String FILE_NAME = "stats.csv";

    public StatsService() {
        loadStats();
    }

    // clearing names so they match the api names
    private String cleanName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replace("fc ", "")
                .replace(" fc", "")
                .replace("cf ", "")
                .replace("ac ", "")
                .replace("as ", "")
                .replace("real ", "")
                .replace("sporting ", "")
                .replace("inter ", "")
                .trim();
    }

    private void loadStats() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (is == null) {
                System.out.println("No stats.csv file found - using default data.");
                return;
            }
            // try-with-resources
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                br.readLine(); // skip header
                while ((line = br.readLine()) != null) {
                    try {
                        String[] v = line.split(",");
                        if (v.length >= 4) {
                            // team name is the key
                            String cleanKey = cleanName(v[0]);
                            statsMap.put(cleanKey, new TeamStats(
                                    Double.parseDouble(v[1]), // avgGoals
                                    Double.parseDouble(v[2]), // btts
                                    Double.parseDouble(v[3])  // winRate
                            ));
                        }
                    } catch (Exception e) {
                        // ignore bad lines
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TeamStats getStatsForTeam(String teamName) {
        String key = cleanName(teamName);

        // if nothing found we return avg stats for team in league
        return statsMap.getOrDefault(key, new TeamStats(2.5, 0.50, 0.35));
    }

    // predicted goals in match
    public double predictMatchTotal(String home, String away) {
        TeamStats h = getStatsForTeam(home);
        TeamStats a = getStatsForTeam(away);
        return (h.avgTotalGoals() + a.avgTotalGoals()) / 2.0;
    }

    // possibility of BTTS
    public double predictBTTSProb(String home, String away) {
        TeamStats h = getStatsForTeam(home);
        TeamStats a = getStatsForTeam(away);

        // Poisson distribution
        double homeLambda = h.avgTotalGoals() / 2.0;
        double awayLambda = a.avgTotalGoals() / 2.0;

        // P(BTTS) = ( 1 - e^(-λ1) ) * (1 - e^(-λ2) )
        double pHomeScores = 1.0 - Math.exp(-homeLambda);
        double pAwayScores = 1.0 - Math.exp(-awayLambda);

        double poisson = pHomeScores * pAwayScores;
        double historical = (h.bttsRate() + a.bttsRate()) / 2.0;

        // mix of team's goals in the past and Poisson distribution
        return 0.7 * poisson + 0.3 * historical;
    }

    public double getWinRate(String teamName) {
        return getStatsForTeam(teamName).winRate();
    }
}
