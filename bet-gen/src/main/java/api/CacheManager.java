package api;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONArray;

public class CacheManager {
    private static final long CACHE_DURATION_MS = 60 * 60 * 1000; // 60 minutes

    // safe path to save
    private static Path getSafePath(String leagueKey) {
        String userHome = System.getProperty("user.home");
        Path cacheFolder = Paths.get(userHome, ".betaidashboard");

        if (!Files.exists(cacheFolder)) {
            try {
                Files.createDirectories(cacheFolder);
            } catch (Exception e) {
                System.err.println("Could not create cache directory: " + e.getMessage());
            }
        }
        return cacheFolder.resolve("cache_" + leagueKey + ".json");
    }

    public static boolean isCacheValid(String leagueKey) {
        File file = getSafePath(leagueKey).toFile();
        if (!file.exists()) return false;

        long lastModified = file.lastModified();
        long now = System.currentTimeMillis();
        return (now - lastModified) < CACHE_DURATION_MS;
    }

    public static String loadCache(String leagueKey) throws Exception {
        System.out.println(">>> Reading from secure user path: " + leagueKey);
        return Files.readString(getSafePath(leagueKey));
    }

    public static void saveCache(String leagueKey, String data) throws Exception {
        String fixedData;
        try {
            JSONArray jsonArray = new JSONArray(data);
            fixedData = jsonArray.toString(4);
        } catch (Exception e) {
            fixedData = data;
        }

        // Zapisujemy do bezpiecznej ścieżki zamiast do folderu aplikacji
        Files.writeString(getSafePath(leagueKey), fixedData);
        System.out.println(">>> Saved data to secure user path: " + leagueKey);
    }
}