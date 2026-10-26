package net.wellwellwell.sound;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModConfig {

    public static boolean enabled = true;
    public static boolean spamProtect = true;
    public static boolean deathEffects = false;
    public static boolean customSoundEnabled = false;
    public static boolean monitorDeaths = false;
    public static boolean notifyTrackedDeath = false;
    public static boolean monitorPops = false;
    public static boolean showMenuButton = true;
    public static boolean useFeet = false;
    public static boolean useDistance = false;
    public static int delayTicks = 0;
    public static int maxDistanceChunks = 32;
    public static int cooldownMS = 5000;
    public static long lastPlayedTime = 0;
    public static long lastSoundTimestamp = 0;
    public static float volume = 0.5f;
    public static float pitch = 1.0f;
    public static double particleMultiplier = 1.000;
    public static String selectedSound = "wither";
    public static String filterMode = "everyone";
    public static String selectedEffect = "lightning";
    public static String customSoundFileName = "";
    public static Set<String> trackedPlayers = new HashSet<>();
    public static List<DeathSound.DeathEntry> deathHistory = new java.util.ArrayList<>();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("deathsound.properties");

    public static void save() {
        Properties props = new Properties();

        props.setProperty("enabled", String.valueOf(enabled));
        props.setProperty("spamProtect", String.valueOf(spamProtect));
        props.setProperty("deathEffects", String.valueOf(deathEffects));
        props.setProperty("customSoundEnabled", String.valueOf(customSoundEnabled));
        props.setProperty("monitorDeaths", String.valueOf(monitorDeaths));
        props.setProperty("notifyTrackedDeath", String.valueOf(notifyTrackedDeath));
        props.setProperty("monitorPops", String.valueOf(monitorPops));
        props.setProperty("showMenuButton", String.valueOf(showMenuButton));
        props.setProperty("useFeet", String.valueOf(useFeet));
        props.setProperty("useDistance", String.valueOf(useDistance));
        props.setProperty("delayTicks", String.valueOf(delayTicks));
        props.setProperty("maxDistanceChunks", String.valueOf(maxDistanceChunks));
        props.setProperty("cooldownMS", String.valueOf(cooldownMS));
        props.setProperty("lastSoundTimestamp", String.valueOf(lastSoundTimestamp));
        props.setProperty("volume", String.valueOf(volume));
        props.setProperty("pitch", String.valueOf(pitch));
        props.setProperty("particleMultiplier", String.valueOf(particleMultiplier));
        props.setProperty("selectedSound", selectedSound);
        props.setProperty("filterMode", filterMode);
        props.setProperty("selectedEffect", String.valueOf(selectedEffect));
        props.setProperty("customSoundFileName", String.valueOf(customSoundFileName));
        props.setProperty("trackedPlayers", String.join(",", trackedPlayers));

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
            spamProtect = Boolean.parseBoolean(props.getProperty("spamProtect", "true"));
            deathEffects = Boolean.parseBoolean(props.getProperty("deathEffects", "false"));
            customSoundEnabled = Boolean.parseBoolean(props.getProperty("customSoundEnabled", "false"));
            monitorDeaths = Boolean.parseBoolean(props.getProperty("monitorDeaths", "false"));
            notifyTrackedDeath = Boolean.parseBoolean(props.getProperty("notifyTrackedDeath", "false"));
            monitorPops = Boolean.parseBoolean(props.getProperty("monitorPops", "false"));
            showMenuButton = Boolean.parseBoolean(props.getProperty("showMenuButton", "true"));
            useDistance = Boolean.parseBoolean(props.getProperty("useDistance", "false"));
            useFeet = Boolean.parseBoolean(props.getProperty("useFeet", "false"));
            delayTicks = Integer.parseInt(props.getProperty("delayTicks", "0"));
            maxDistanceChunks = Integer.parseInt(props.getProperty("maxDistanceChunks", "32"));
            cooldownMS = Integer.parseInt(props.getProperty("cooldownMS", "5000"));
            lastSoundTimestamp = Long.parseLong(props.getProperty("lastSoundTimestamp", "0"));
            volume = Float.parseFloat(props.getProperty("volume", "0.5"));
            pitch = Float.parseFloat(props.getProperty("pitch", "1.0"));
            particleMultiplier = Double.parseDouble(props.getProperty("particleMultiplier", "1.000"));
            selectedSound = props.getProperty("selectedSound", "wither");
            filterMode = props.getProperty("filterMode", "everyone");
            selectedEffect = props.getProperty("selectedEffect", "lightning");
            customSoundFileName = props.getProperty("customSoundFileName", "");

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
        volume = 0.5f;
        pitch = 1.0f;
        spamProtect = true;
        filterMode = "everyone";
        trackedPlayers.clear();
        deathEffects = false;
        selectedEffect = "lightning";
        maxDistanceChunks = 32;
        monitorDeaths = false;
        notifyTrackedDeath = false;
        customSoundEnabled = false;
        customSoundFileName = "";
        lastSoundTimestamp = 0;
        cooldownMS = 5000;
        useFeet = false;
        monitorPops = false;
        showMenuButton = true;
        useDistance = false;
        particleMultiplier = 1.000;
        deathHistory.clear();
        save();
    }
}
