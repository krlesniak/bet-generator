package model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // ignores other data from API that we do not have in this class
public class Match {

    @JsonProperty("home_team")
    private String homeTeam;

    @JsonProperty("away_team")
    private String awayTeam;

    @JsonProperty("sport_title")
    private String sportTitle;

    @JsonProperty("commence_time")
    private LocalDateTime time;

    private List<BetOption> odds;

    public Match() {}

    // getters and setters
    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }

    public void setSportTitle(String sportTitle) { this.sportTitle = sportTitle; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }

    public List<BetOption> getOdds() { return odds; }
    public void setOdds(List<BetOption> odds) { this.odds = odds; }
}