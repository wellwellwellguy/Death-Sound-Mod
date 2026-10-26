package com.wellwellwell.sound;

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
    public static boolean deathEffects = false;
    public static int maxDistanceChunks = 32;
    public static boolean monitorDeaths = false;
    public static boolean notifyTrackedDeath = false;
    public static String customSoundFileName = "";
    public static String selectedEffect = "lightning";
    public static double particleMultiplier = 1.000;
    public static boolean useDistance = false;
    public static boolean showMenuButton = true;
    public static boolean customSoundEnabled = false;
    public static long lastSoundTimestamp = 0;
    public static int cooldownMS = 5000;
    public static boolean useFeet = false;
    public static boolean monitorPops = false;

    public static List<DeathSound.DeathEntry> deathHistory = new java.util.ArrayList<>();

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
        props.setProperty("deathEffects", String.valueOf(deathEffects));
        props.setProperty("maxDistanceChunks", String.valueOf(maxDistanceChunks));
        props.setProperty("monitorDeaths", String.valueOf(monitorDeaths));
        props.setProperty("notifyTrackedDeath", String.valueOf(notifyTrackedDeath));
        props.setProperty("customSoundFileName", String.valueOf(customSoundFileName));
        props.setProperty("customSoundEnabled", String.valueOf(customSoundEnabled));
        props.setProperty("lastSoundTimestamp", String.valueOf(lastSoundTimestamp));
        props.setProperty("cooldownMS", String.valueOf(cooldownMS));
        props.setProperty("useFeet", String.valueOf(useFeet));
        props.setProperty("monitorPops", String.valueOf(monitorPops));
        props.setProperty("selectedEffect", selectedEffect);
        props.setProperty("particleMultiplier", String.valueOf(particleMultiplier));
        props.setProperty("useDistance", String.valueOf(useDistance));
        props.setProperty("showMenuButton", String.valueOf(showMenuButton));

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
            deathEffects = Boolean.parseBoolean(props.getProperty("deathEffects", "false"));
            maxDistanceChunks = Integer.parseInt(props.getProperty("maxDistanceChunks", "32"));
            monitorDeaths = Boolean.parseBoolean(props.getProperty("monitorDeaths", "false"));
            notifyTrackedDeath = Boolean.parseBoolean(props.getProperty("notifyTrackedDeath", "false"));
            customSoundEnabled = Boolean.parseBoolean(props.getProperty("customSoundEnabled", "false"));
            customSoundFileName = props.getProperty("customSoundFileName", "");
            lastSoundTimestamp = Long.parseLong(props.getProperty("lastSoundTimestamp", "0"));
            cooldownMS = Integer.parseInt(props.getProperty("cooldownMS", "5000"));
            useFeet = Boolean.parseBoolean(props.getProperty("useFeet", "false"));
            monitorPops = Boolean.parseBoolean((props.getProperty("monitorPops","false")));
            selectedEffect = props.getProperty("selectedEffect", "lightning");
            particleMultiplier = Double.parseDouble(props.getProperty("particleMultiplier", "1.000"));
            useDistance = Boolean.parseBoolean(props.getProperty("useDistance", "false"));
            showMenuButton = Boolean.parseBoolean(props.getProperty("showMenuButton", "true"));

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
        maxDistanceChunks = 32;
        monitorDeaths = false;
        notifyTrackedDeath = false;
        customSoundEnabled = false;
        customSoundFileName = "";
        lastSoundTimestamp = 0;
        cooldownMS = 5000;
        useFeet = false;
        monitorPops = false;
        selectedEffect = "lightning";
        particleMultiplier = 1.000;
        useDistance = false;
        showMenuButton = true;
        save();
    }
}