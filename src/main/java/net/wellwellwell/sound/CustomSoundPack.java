package net.wellwellwell.sound;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class CustomSoundPack {
    private static final String PACK_NAME = "DeathSound-Resources";

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
            try (FileWriter writer = new FileWriter(mcmeta)) {
                writer.write("{" +
                        "\"pack\":{" +
                        "\"pack_format\":15," +
                        "\"supported_formats\":[1,75]," +
                        "\"min_format\":1.0," +
                        "\"max_format\":75.0," +
                        "\"description\":\"DeathSound-Resources\"" +
                        "}" +
                        "}");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File packIcon = new File(baseFolder, "pack.png");
        if (!packIcon.exists()) {
            try (java.io.InputStream iconStream = CustomSoundPack.class.getResourceAsStream("/assets/deathsound/icon.png")) {
                if (iconStream != null) {
                    java.nio.file.Files.copy(iconStream, packIcon.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    System.err.println("Failed to load icon for Resource Pack: DeathSound-Resources");
                }
            } catch (IOException e) {
                System.err.println("[DeathSound] Error loading icon for Resource Pack: " + e.getMessage());
            }
        }

        File assetsFolder = new File(baseFolder, "assets/deathsound");
        if (!assetsFolder.exists()) assetsFolder.mkdirs();

        String selectedTrack = ModConfig.customSoundFileName;
        String cleanName = "custom_ogg";

        if (selectedTrack != null && !selectedTrack.isEmpty()) {
            if (selectedTrack.contains(".")) {
                cleanName = selectedTrack.substring(0, selectedTrack.lastIndexOf('.'));
            } else {
                cleanName = selectedTrack;
            }
        } else {
            File[] files = soundsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".ogg"));
            if (files != null && files.length > 0) {
                String fileName = files[0].getName();
                cleanName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
        }

        File soundsJson = new File(assetsFolder, "sounds.json");

        String jsonContent = "{\n" +
                "  \"custom_ogg\": {\n" +
                "    \"sounds\": [\n" +
                "      {\n" +
                "        \"name\": \"deathsound:" + cleanName + "\",\n" +
                "        \"stream\": true\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        writeFile(soundsJson, jsonContent);
    }

    public static void syncSounds() {
        Path configSoundsDir = FabricLoader.getInstance().getConfigDir().resolve("deathsound/sounds");
        Path packSoundsDir = FabricLoader.getInstance().getGameDir().resolve("resourcepacks/DeathSound-Resources/assets/deathsound/sounds");

        try {
            if (!Files.exists(configSoundsDir)) Files.createDirectories(configSoundsDir);
            if (!Files.exists(packSoundsDir)) Files.createDirectories(packSoundsDir);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(configSoundsDir)) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file)) {
                        Path target = packSoundsDir.resolve(file.getFileName());
                        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(packSoundsDir)) {
                for (Path packFile : stream) {
                    if (Files.isRegularFile(packFile)) {
                        Path correspondingConfigFile = configSoundsDir.resolve(packFile.getFileName());

                        if (!Files.exists(correspondingConfigFile)) {
                            Files.delete(packFile);
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("[DeathSound] Error syncing sounds: " + e.getMessage());
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