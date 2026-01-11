package service;

import model.BetOption;
import model.Match;
import model.RiskLevel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;

public class BetLogicService {
    private final StatsService statsService;

    public BetLogicService(StatsService statsService) {
        this.statsService = statsService;
    }

    public List<BetOption> generateSmartCoupon(List<Match> matches, double targetOdd, RiskLevel riskLevel) {
        List<SmartOption> candidates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate threeDays = today.plusDays(2);

        System.out.println(">>> Starting analysis " + matches.size() + " matches (3 days)...");

        for (Match match : matches) {
            LocalDate matchDate = match.getTime().toLocalDate();
            if (matchDate.isBefore(today) || matchDate.isAfter(threeDays)) {
                continue;
            }

            for (BetOption option : match.getOdds()) {
                if (!isOddInRiskRange(option.getPrice(), riskLevel)) {
                    continue;
                }

                double score = calculateSafetyScore(match, option, riskLevel);

                // odds after taxes
                if (score > 0.65) {
                    candidates.add(new SmartOption(match, option, score));
                }
            }
        }

        System.out.println(">>> Found " + candidates.size() + " candidates. Choosing the best...");

        // sorting for super safe bets
        candidates.sort(Comparator.comparingDouble(SmartOption::score).reversed());

        return buildCouponFromCandidates(candidates, targetOdd);
    }

    private double calculateSafetyScore(Match match, BetOption option, RiskLevel riskLevel) {
        String home = match.getHomeTeam();
        String away = match.getAwayTeam();
        double odds = option.getPrice();
        String betName = option.getName().toLowerCase();

        double winRateHome = statsService.getWinRate(home);
        double winRateAway = statsService.getWinRate(away);
        double predictedGoals = statsService.predictMatchTotal(home, away);
        double bttsProbStats = statsService.predictBTTSProb(home, away);

        double baseScore = 0.0;

        // --- winner h2h ---
        if (checkNameMatch(betName, home) || checkNameMatch(betName, away)) {
            boolean isHome = checkNameMatch(betName, home);
            double winRate = isHome ? winRateHome : winRateAway;
            double expectedValue = winRate * odds;

            if (winRate > 0.70) {
                baseScore = expectedValue * 1.5; // we boost odds for safe bet
            } else if (odds > 3.0) {
                baseScore = expectedValue * 0.5; // for very high odds
            } else {
                baseScore = expectedValue * 1.1;
            }

            // if predicted winner plays at home
            if (isHome && winRate > 0.60) baseScore *= 1.1;
        }

        // ---  goals (Over) ---
        else if (betName.startsWith("over") && option.getPoint() != null) {
            double line = option.getPoint();

            // goals close to over 2,5
            if (line >= 2.0 && line <= 2.75) {
                // 60% of predicted goals and 40% of btts chance
                double goalFactor = (predictedGoals / 2.5) * 0.6 + (bttsProbStats * 0.4);
                baseScore = goalFactor * odds;

                if (riskLevel == RiskLevel.SAFE) baseScore *= 0.9;
            }
            // over 1,5 goals very safe
            else if (line < 2.0) {
                baseScore = (predictedGoals / 1.5) * odds * 0.4;
            }
            // over 3,5 goals
            else if (line >= 3.0 && predictedGoals > 3.5) {
                baseScore = (predictedGoals / line) * odds * 0.7;
            }
        }

        // --- SAFETY BOOST  ---
        // if something is VERY safe we boost its odds so they will appear
        if (baseScore > 0) {
            if (odds <= 1.35) {
                baseScore = baseScore * 1.7; // before 2.2
            }
            if (odds <= 1.60) {
                baseScore = baseScore * 1.3; // before 1.6
            }
        }

        return baseScore;
    }

    private boolean checkNameMatch(String betName, String teamName) {
        String t = teamName.toLowerCase();
        return betName.equals(t) || t.contains(betName) || betName.contains(t);
    }

    private List<BetOption> buildCouponFromCandidates(List<SmartOption> candidates, double targetOdd) {
        List<BetOption> coupon = new ArrayList<>();
        double currentTotalOdd = 1.0;
        List<String> usedMatches = new ArrayList<>();

        for (SmartOption smart : candidates) {
            if (currentTotalOdd * smart.option().getPrice() > targetOdd * 1.25) continue;

            String matchKey = smart.match().getHomeTeam() + " vs " + smart.match().getAwayTeam();

            if (!usedMatches.contains(matchKey)) {
                String dateStr = smart.match().getTime().toLocalDate().toString();
                String displayType = smart.option().getName();
                if (smart.option().getPoint() != null) displayType += " " + smart.option().getPoint();

                // BTTS info
                double bttsProb = statsService.predictBTTSProb(smart.match().getHomeTeam(), smart.match().getAwayTeam());
                int bttsPercent = (int) (bttsProb * 100);
                String bttsInfo = " [BTTS: " + bttsPercent + "%]";

                // super safe and high goals info in good matches
                if (smart.option().getPrice() < 1.40) displayType += "  {super safe}";
                else if (displayType.contains("Over 2.5") && smart.score > 3.0) displayType += "  {high goals}";

                String finalDisplayName = matchKey + " (" + dateStr + ") [" + displayType + "]" + bttsInfo;

                BetOption betToAdd = new BetOption(
                        finalDisplayName,
                        smart.option().getPrice(),
                        smart.option().getPoint()
                );

                coupon.add(betToAdd);
                currentTotalOdd *= betToAdd.getPrice();
                usedMatches.add(matchKey);

                if (currentTotalOdd >= targetOdd * 0.95) break;
            }
        }
        return coupon;
    }

    private boolean isOddInRiskRange(double price, RiskLevel level) {
        return switch (level) {
            // SAFE
            case SAFE -> price >= 1.05 && price <= 1.70;
            // MEDIUM
            case MEDIUM -> price >= 1.05 && price <= 2.20;
            // RISKY
            case RISKY -> price >= 1.05 && price <= 15.0;
        };
    }

    //  method for JavaFX that returns String instead of printing to console
    public String getAllMatchesAnalysis(List<Match> matches) {
        StringBuilder sb = new StringBuilder();
        sb.append("-=-=-=-=-=-=-=-=-=-=- FULL MATCH ANALYSIS & BTTS PROBABILITY -=-=-=-=-=-=-=-=-=-=-\n\n");

        // sorting by highest percentage of btts
        matches.sort((m1, m2) -> Double.compare(
                statsService.predictBTTSProb(m2.getHomeTeam(), m2.getAwayTeam()),
                statsService.predictBTTSProb(m1.getHomeTeam(), m1.getAwayTeam())
        ));

        LocalDate today = LocalDate.now();
        LocalDate threeDays = today.plusDays(2);

        for (Match m : matches) {
            LocalDate matchDate = m.getTime().toLocalDate();

            if (matchDate.isBefore(today) || matchDate.isAfter(threeDays)) {
                continue;
            }
            double bttsProb = statsService.predictBTTSProb(m.getHomeTeam(), m.getAwayTeam());
            double predictedGoals = statsService.predictMatchTotal(m.getHomeTeam(), m.getAwayTeam());

            String status = (bttsProb > 0.60) ? "HIGH" : (bttsProb < 0.45) ? "LOW" : "MEDIUM";

            // appending each line to our string builder
            sb.append(String.format("[%s] %s vs %s | BTTS: %d%% | Exp. Goals: %.2f | %s\n\n",
                    matchDate, m.getHomeTeam(), m.getAwayTeam(), (int)(bttsProb * 100), predictedGoals, status));
        }

        sb.append("-=-=-=-=-=-=-=-=-=-=-=-=-=- END OF ANALYSIS -=-=-=-=-=-=-=-=-=-=-=-=-=-");
        return sb.toString();
    }

    /*
    // checking possibility for btts for every match for printing (old version)
    public void printAllMatchesAnalysis(List<Match> matches) {
        System.out.println("\n-=-=-=-=-=-=-=-=-=-=- FULL MATCH ANALYSIS & BTTS PROBABILITY -=-=-=-=-=-=-=-=-=-=-\n");

        // sorting by highest percentage of btts
        matches.sort((m1, m2) -> Double.compare(
                statsService.predictBTTSProb(m2.getHomeTeam(), m2.getAwayTeam()),
                statsService.predictBTTSProb(m1.getHomeTeam(), m1.getAwayTeam())
        ));

        for (Match m : matches) {
            LocalDate matchDate = m.getTime().toLocalDate();
            LocalDate today = LocalDate.now();
            LocalDate threeDays = today.plusDays(2);

            if (matchDate.isBefore(today) || matchDate.isAfter(threeDays)) {
                continue;
            }
            double bttsProb = statsService.predictBTTSProb(m.getHomeTeam(), m.getAwayTeam());
            double predictedGoals = statsService.predictMatchTotal(m.getHomeTeam(), m.getAwayTeam());

            String status = (bttsProb > 0.65) ? "HIGH" : (bttsProb < 0.40) ? "LOW" : "MEDIUM";

            System.out.printf("[%s] %s vs %s | BTTS: %d%% | Exp. Goals: %.2f | %s%n\n",
                    m.getTime().toLocalDate(),
                    m.getHomeTeam(),
                    m.getAwayTeam(),
                    (int)(bttsProb * 100),
                    predictedGoals,
                    status
            );
        }
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n");
    }
     */

    private record SmartOption(Match match, BetOption option, double score) {}
}