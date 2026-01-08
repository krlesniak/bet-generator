package api;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONArray;

public class CacheManager {
    private static final long CACHE_DURATION_MS = 60 * 60 * 1000; // 60 minutes

    // creates a file based on a league
    private static String getFileName(String leagueKey) {
        return "cache_" + leagueKey + ".json";
    }

    public static boolean isCacheValid(String leagueKey) {
        File file = new File(getFileName(leagueKey));
        if (!file.exists()) return false;

        long lastModified = file.lastModified();
        long now = System.currentTimeMillis();
        return (now - lastModified) < CACHE_DURATION_MS;
    }

    public static String loadCache(String leagueKey) throws Exception {
        System.out.println(">>> Reading from json files: " + leagueKey);
        return Files.readString(Path.of(getFileName(leagueKey)));
    }

    public static void saveCache(String leagueKey, String data) throws Exception {
        String fixedData;
        try {
            // as list in json files
            JSONArray jsonArray = new JSONArray(data);
            // indent = 4
            fixedData = jsonArray.toString(4);
        } catch (Exception e) {
            fixedData = data;
        }

        Files.writeString(Path.of(getFileName(leagueKey)), fixedData);
        System.out.println(">>> Saved data to json files: " + leagueKey);
    }

}
