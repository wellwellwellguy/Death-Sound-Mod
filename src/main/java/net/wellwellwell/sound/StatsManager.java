package net.wellwellwell.sound;

import net.minecraft.client.MinecraftClient;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {
    private static File getStatsFile() {
        File mcDir = MinecraftClient.getInstance().runDirectory;
        File configDir = new File(mcDir, "config/deathsound");

        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        return new File(configDir, "stats.properties");
    }

    public static int totalDeathsObserved = 0;
    public static int totalSoundsPlayed = 0;
    public static int totalPops = 0;

    public static Map<String, Integer> playerDeathMap = new HashMap<>();
    public static Map<String, Integer> playerPopsMap = new HashMap<>();

    public static void load() {
        File file = getStatsFile();
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Properties props = new Properties();
            props.load(reader);

            totalDeathsObserved = Integer.parseInt(props.getProperty("totalDeaths", "0"));
            totalSoundsPlayed = Integer.parseInt(props.getProperty("soundsPlayed", "0"));
            totalPops = Integer.parseInt(props.getProperty("totalPops", "0"));

            String playerStats = props.getProperty("playerStats", "");
            if (!playerStats.isEmpty()) {
                playerDeathMap.clear();
                for (String entry : playerStats.split(";")) {
                    String[] parts = entry.split(":");
                    if (parts.length == 2) {
                        playerDeathMap.put(parts[0], Integer.parseInt(parts[1]));
                    }
                }
            }

            String popStats = props.getProperty("playerPops", "");
            if (!popStats.isEmpty()) {
                playerPopsMap.clear();
                for (String entry : popStats.split(";")) {
                    String[] parts = entry.split(":");
                    if (parts.length == 2) {
                        playerPopsMap.put(parts[0], Integer.parseInt(parts[1]));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            File file = getStatsFile();
            Properties props = new Properties();
            props.setProperty("totalDeaths", String.valueOf(totalDeathsObserved));
            props.setProperty("soundsPlayed", String.valueOf(totalSoundsPlayed));
            props.setProperty("totalPops", String.valueOf(totalPops));

            String playerStats = playerDeathMap.entrySet().stream()
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(";"));
            props.setProperty("playerStats", playerStats);

            String popStats = playerPopsMap.entrySet().stream()
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(";"));
            props.setProperty("playerPops", popStats);

            try (FileWriter writer = new FileWriter(file)) {
                props.store(writer, "DeathSound Global Statistics");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addDeath(String name, boolean soundPlayed) {
        totalDeathsObserved++;
        if (soundPlayed) totalSoundsPlayed++;
        playerDeathMap.put(name, playerDeathMap.getOrDefault(name, 0) + 1);
        save();
    }

    public static void addPop(String name) {
        totalPops++;
        playerPopsMap.put(name, playerPopsMap.getOrDefault(name, 0) + 1);
        save();
    }
}