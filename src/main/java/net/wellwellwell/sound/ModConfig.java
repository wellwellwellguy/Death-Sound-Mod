package net.wellwellwell.sound;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModConfig {
    public static boolean enabled = true;
    public static int delayTicks = 0;
    public static String selectedSound = "wither";
    public static float volume = 0.5f;
    public static boolean spamProtect = true;
    public static Set<String> trackedPlayers = new HashSet<>();
    public static long lastPlayedTime = 0;
    public static float pitch = 1.0f;
    public static String filterMode = "everyone";
    public static int maxDistanceChunks = 10;
    public static boolean notifyTrackedDeath = false;
    public static int cooldownMS = 5000;
    public static boolean useFeet = false;

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("deathsound.properties");

    public static boolean canPlaySound() {
        if (!spamProtect) return true;
        long now = System.currentTimeMillis();
        if (now - lastPlayedTime < cooldownMS) {
            return false;
        }
        lastPlayedTime = now;
        return true;
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("enabled", String.valueOf(enabled));
        props.setProperty("delayTicks", String.valueOf(delayTicks));
        props.setProperty("selectedSound", selectedSound);
        props.setProperty("volume", String.valueOf(volume));
        props.setProperty("spamProtect", String.valueOf(spamProtect));
        props.setProperty("filterMode", filterMode);
        props.setProperty("trackedPlayers", String.join(",", trackedPlayers));
        props.setProperty("pitch", String.valueOf(pitch));
        props.setProperty("maxDistanceChunks", String.valueOf(maxDistanceChunks));
        props.setProperty("notifyTrackedDeath", String.valueOf(notifyTrackedDeath));
        props.setProperty("cooldownMS", String.valueOf(cooldownMS));
        props.setProperty("useFeet", String.valueOf(useFeet));

        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "DeathSound Config");
        } catch (IOException e) {
            System.out.println("[DeathSound] Error during saving the config file.");
        }
    }

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            save();
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
            enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
            delayTicks = Integer.parseInt(props.getProperty("delayTicks", "0"));
            selectedSound = props.getProperty("selectedSound", "wither");
            volume = Float.parseFloat(props.getProperty("volume", "0.5"));
            spamProtect = Boolean.parseBoolean(props.getProperty("spamProtect", "true"));
            filterMode = props.getProperty("filterMode", "everyone");
            pitch = Float.parseFloat(props.getProperty("pitch", "1.0"));
            maxDistanceChunks = Integer.parseInt(props.getProperty("maxDistanceChunks", "10"));
            notifyTrackedDeath = Boolean.parseBoolean(props.getProperty("notifyTrackedDeath", "false"));
            cooldownMS = Integer.parseInt(props.getProperty("cooldownMS", "5000"));
            useFeet = Boolean.parseBoolean(props.getProperty("useFeet", "false"));

            String players = props.getProperty("trackedPlayers", "");
            trackedPlayers.clear();
            if (!players.isEmpty()) {
                Collections.addAll(trackedPlayers, players.split(","));
            }
        } catch (Exception e) {
            System.out.println("[DeathSound] Error during loading process.");
        }
    }

    public static void resetToDefaults() {
        enabled = true;
        delayTicks = 0;
        selectedSound = "wither";
        volume = 0.25f;
        pitch = 1.0f;
        spamProtect = true;
        filterMode = "everyone";
        trackedPlayers.clear();
        maxDistanceChunks = 10;
        notifyTrackedDeath = false;
        cooldownMS = 5000;
        useFeet = false;
        save();
    }
}