package net.wellwellwell.sound;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;

public class ModConfig {
    private static final File CONFIG_FILE = new File("config/deathsound.properties");

    public static boolean enabled = true;
    public static float volume = 1.0f;
    public static float pitch = 1.0f;

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (InputStream input = Files.newInputStream(CONFIG_FILE.toPath())) {
            Properties prop = new Properties();
            prop.load(input);
            enabled = Boolean.parseBoolean(prop.getProperty("enabled", "true"));
            volume = Float.parseFloat(prop.getProperty("volume", "1.0"));
            pitch = Float.parseFloat(prop.getProperty("pitch", "1.0"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (OutputStream output = Files.newOutputStream(CONFIG_FILE.toPath())) {
            Properties prop = new Properties();
            prop.setProperty("enabled", String.valueOf(enabled));
            prop.setProperty("volume", String.valueOf(volume));
            prop.setProperty("pitch", String.valueOf(pitch));
            prop.store(output, "DeathSound Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reset() {
        enabled = true;
        volume = 1.0f;
        pitch = 1.0f;
        save();
    }
}