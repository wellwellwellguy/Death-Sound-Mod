package net.wellwellwell.sound;

import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CustomSoundPack {
    public static final String PACK_NAME = "DeathSound-Resources";

    public static void init() {
        File mcDir = MinecraftClient.getInstance().runDirectory;
        File resourcePacksDir = new File(mcDir, "resourcepacks");
        File baseFolder = new File(resourcePacksDir, PACK_NAME);
        File soundsFolder = new File(baseFolder, "assets/deathsound/sounds");

        if (!soundsFolder.exists()) {
            soundsFolder.mkdirs();
        }


        File mcmeta = new File(baseFolder, "pack.mcmeta");
        if (!mcmeta.exists()) {
            writeFile(mcmeta, "{\"pack\":{\"pack_format\":15,\"description\":\"§6DeathSound §7Resources\"}}");
        }

        File assetsFolder = new File(baseFolder, "assets/deathsound");
        if (!assetsFolder.exists()) assetsFolder.mkdirs();

        File soundsJson = new File(assetsFolder, "sounds.json");
        if (!soundsJson.exists()) {
            writeFile(soundsJson, "{}");
        }
    }

    private static void writeFile(File file, String content) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}