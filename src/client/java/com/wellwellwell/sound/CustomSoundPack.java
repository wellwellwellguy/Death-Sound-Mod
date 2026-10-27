package com.wellwellwell.sound;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class CustomSoundPack {
    private static final String PACK_NAME = "DeathSound-Resources";

    public static void init() {
        File mcDir = Minecraft.getInstance().gameDirectory;
        File resourcePacksDir = new File(mcDir, "resourcepacks");
        File baseFolder = new File(resourcePacksDir, PACK_NAME);
        File soundsFolder = new File(baseFolder, "assets/deathsound/sounds");

        File iconFile = new File(baseFolder, "pack.png");

        if (!iconFile.exists()) {
            try (var inputStream = CustomSoundPack.class.getResourceAsStream("/assets/deathsound/icon.png")) {
                if (inputStream != null) {
                    Files.copy(inputStream, iconFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                System.err.println("[DeathSound] Could not create pack.png: " + e.getMessage());
            }
        }

        if (!soundsFolder.exists()) {
            soundsFolder.mkdirs();
        }

        File mcmeta = new File(baseFolder, "pack.mcmeta");
        if (!mcmeta.exists()) {
            String content = """
            {
              "pack": {
                "pack_format": 15,
                "supported_formats": [1,100],                  
                "min_format": 1.0,
                "max_format": 100.0,
                "description": "DeathSound-Resources"
              }
            }""";

            writeFile(mcmeta, content);
        }

        File assetsFolder = new File(baseFolder, "assets/deathsound");
        if (!assetsFolder.exists()) assetsFolder.mkdirs();

        syncSounds();
        if (ModConfig.customSoundFileName != null) {
            saveSoundsJson(Minecraft.getInstance(), ModConfig.customSoundFileName);
        } else {
            saveSoundsJson(Minecraft.getInstance(), "custom");
        }
    }

    public static void saveSoundsJson(Minecraft client, String name) {
        try {
            File assetsDir = new File(client.gameDirectory, "resourcepacks/DeathSound-Resources/assets/deathsound/");
            if (!assetsDir.exists()) assetsDir.mkdirs();

            File jsonFile = new File(assetsDir, "sounds.json");
            String fileNameOnly = name.toLowerCase().replace(".ogg", "");

            String json = "{\n" +
                    "  \"custom_ogg\": {\n" +
                    "    \"sounds\": [\n" +
                    "      {\n" +
                    "        \"name\": \"deathsound:" + fileNameOnly + "\",\n" +
                    "        \"stream\": true\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }\n" +
                    "}";

            java.nio.file.Files.writeString(jsonFile.toPath(), json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getFiles() {
        File mcDir = Minecraft.getInstance().gameDirectory;
        File soundsFolder = new File(mcDir, "resourcepacks/" + PACK_NAME + "/assets/deathsound/sounds");
        List<String> sounds = new ArrayList<>();

        if (soundsFolder.exists()) {
            File[] files = soundsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".ogg"));
            if (files != null) {
                for (File f : files) {
                    sounds.add(f.getName().replace(".ogg", ""));
                }
            }
        }
        return sounds;
    }

    public static void syncSounds() {
        Path configSoundsDir = FabricLoader.getInstance().getConfigDir().resolve("deathsound/sounds");
        Path packSoundsDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("resourcepacks")
                .resolve(PACK_NAME)
                .resolve("assets/deathsound/sounds");

        try {
            if (!Files.exists(configSoundsDir)) Files.createDirectories(configSoundsDir);
            if (!Files.exists(packSoundsDir)) Files.createDirectories(packSoundsDir);
            List<Path> filesToRename = new ArrayList<>();
            try (DirectoryStream<Path> configStream = Files.newDirectoryStream(configSoundsDir, "*.ogg")) {
                for (Path configFile : configStream) {
                    String originalName = configFile.getFileName().toString();
                    String cleanedName = originalName.replace(" ", "_").toLowerCase();

                    if (!originalName.equals(cleanedName)) {
                        filesToRename.add(configFile);
                    }
                }
            }

            for (Path configFile : filesToRename) {
                String originalName = configFile.getFileName().toString();
                String cleanedName = originalName.replace(" ", "_").toLowerCase();

                Path targetPath = configFile.resolveSibling(cleanedName);

                if (originalName.equalsIgnoreCase(cleanedName)) {
                    Path tempPath = configFile.resolveSibling(cleanedName + ".tmp");

                    Files.move(configFile, tempPath, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    if (Files.exists(targetPath)) {
                        Files.delete(configFile);
                    } else {
                        Files.move(configFile, targetPath);
                    }
                }
            }

            try (DirectoryStream<Path> packStream = Files.newDirectoryStream(packSoundsDir, "*.ogg")) {
                for (Path packFile : packStream) {
                    if (!Files.exists(configSoundsDir.resolve(packFile.getFileName()))) {
                        Files.delete(packFile);
                    }
                }
            }

            try (DirectoryStream<Path> configStream = Files.newDirectoryStream(configSoundsDir, "*.ogg")) {
                for (Path configFile : configStream) {
                    Files.copy(configFile, packSoundsDir.resolve(configFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.out.println("[DeathSound] Sync complete.");
        } catch (IOException e) {
            System.err.println("[DeathSound] Sync error: " + e.getMessage());
            e.printStackTrace();
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
