package net.wellwellwell.sound;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static java.lang.String.format;
import static net.wellwellwell.sound.CustomSoundScreen.saveSoundsJson;


public class DeathSound implements ClientModInitializer {

    private static final String PACK_NAME = "DeathSound-Resources";
    public static final List<DeathEntry> deathHistory = new ArrayList<>();
    public record DeathEntry(String playerName, double x, double y, double z, long timestamp) {}
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("DeathSound");

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        CustomSoundPack.init();
        LOGGER.info("DeathSound Mod has been loaded!");

        File mcDir = MinecraftClient.getInstance().runDirectory;
        File baseFolder = new File(mcDir, "resourcepacks/" + PACK_NAME);
        File soundsFolder = new File(baseFolder, "assets/deathsound/sounds");
        if (!soundsFolder.exists()) soundsFolder.mkdirs();

        File mcmeta = new File(baseFolder, "pack.mcmeta");
        if (!mcmeta.exists()) {
            try (java.io.FileWriter writer = new java.io.FileWriter(mcmeta)) {
                writer.write("{\"pack\":{\"pack_format\":15,\"description\":\"Required for custom death sounds.\"}}");
            } catch (IOException e) { e.printStackTrace(); }
        }

        net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(
                new Identifier("deathsound", "custom_pack"),
                net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("death-sound").get(),
                Text.literal("DeathSound Resources"),
                net.fabricmc.fabric.api.resource.ResourcePackActivationType.ALWAYS_ENABLED
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var node = dispatcher.register(ClientCommandManager.literal("deathsound")
                    .then(ClientCommandManager.literal("on").executes(context -> {
                        if (ModConfig.enabled) {
                            context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Death Sounds are already enabled!"));
                            return 1;
                        }
                        ModConfig.enabled = true;
                        ModConfig.save();
                        context.getSource().sendFeedback(Text.literal("§6[DeathSound]§7 Mod §aenabled"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("off").executes(context -> {
                        if (!ModConfig.enabled) {
                            context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7DeathSounds are already disabled!"));
                            return 1;
                        }
                        ModConfig.enabled = false;
                        ModConfig.save();
                        context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Mod §cdisabled"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("distance")
                            .then(ClientCommandManager.argument("chunks", IntegerArgumentType.integer(1, 21))
                                    .executes(context -> {
                                        int dist = IntegerArgumentType.getInteger(context, "chunks");
                                        ModConfig.maxDistanceChunks = dist;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Distance: " + (dist > 20 ? "§aUnlimited" : "§b" + dist + " Chunks")));
                                        return 1;
                                    }))
                    )
                    .then(ClientCommandManager.literal("volume")
                            .then(ClientCommandManager.argument("value", IntegerArgumentType.integer(1, 100))
                                    .executes(context -> {
                                        int vol = IntegerArgumentType.getInteger(context, "value");
                                        ModConfig.volume = vol / 100.0f;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Volume set to §e" + vol + "%"));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("set-sound")
                            .then(ClientCommandManager.argument("file", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        File folder = new File(MinecraftClient.getInstance().runDirectory, "resourcepacks/" + PACK_NAME + "/assets/deathsound/sounds");
                                        if (folder.exists()) {
                                            File[] files = folder.listFiles((dir, name) -> name.endsWith(".ogg"));
                                            if (files != null) {
                                                for (File f : files) {
                                                    builder.suggest(f.getName().toLowerCase());
                                                }
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String rawName = StringArgumentType.getString(context, "file");
                                        String cleanName = rawName.toLowerCase().replace(".ogg", "");
                                        long ts = System.currentTimeMillis();

                                        ModConfig.customSoundFileName = cleanName;
                                        ModConfig.lastSoundTimestamp = ts;
                                        ModConfig.save();

                                        MinecraftClient client = MinecraftClient.getInstance();
                                        saveSoundsJson(client, ts, cleanName);

                                        client.getResourcePackManager().scanPacks();
                                        List<String> enabledPacks = new ArrayList<>(client.getResourcePackManager().getEnabledNames());
                                        String fullPackName = "file/" + PACK_NAME;

                                        if (!enabledPacks.contains(fullPackName)) {
                                            enabledPacks.add(fullPackName);
                                            client.getResourcePackManager().setEnabledProfiles(enabledPacks);
                                        } else {
                                            client.reloadResources();
                                        }

                                        context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Sound set to: §e" + cleanName));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("pitch")
                            .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 2.0f))
                                    .executes(context -> {
                                        float pitch = FloatArgumentType.getFloat(context, "value");
                                        ModConfig.pitch = pitch;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Pitch set to §e" + pitch));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("menu").executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.execute(() -> {
                            client.setScreen(new DeathSoundScreen(null));
                        });
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("stop").executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client != null && client.getSoundManager() != null) {
                            client.getSoundManager().stopSounds(new Identifier("deathsound", "custom_" + ModConfig.lastSoundTimestamp), SoundCategory.PLAYERS);
                            client.getSoundManager().stopSounds(new Identifier("minecraft", "entity.wither.spawn"), SoundCategory.PLAYERS);
                            client.getSoundManager().stopSounds(new Identifier("minecraft", "entity.lightning_bolt.thunder"), SoundCategory.PLAYERS);
                            client.getSoundManager().stopSounds(new Identifier("minecraft", "entity.generic.explode"), SoundCategory.PLAYERS);
                            client.getSoundManager().stopSounds(new Identifier("minecraft", "block.sculk_shrieker.shriek"), SoundCategory.PLAYERS);
                            context.getSource().sendFeedback(Text.literal("§6[DeathSound] §aAll death sounds stopped."));
                            System.out.println("Stopped all death sounds.");
                        }
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("history").executes(context -> {
                        if (DeathSound.deathHistory.isEmpty()) {
                            context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7No deaths recorded yet."));
                            return 1;
                        }
                        context.getSource().sendFeedback(Text.literal("§6§l----- Death History -----"));
                        for (DeathEntry entry : DeathSound.deathHistory) {
                            long totalSeconds = (System.currentTimeMillis() - entry.timestamp()) / 1000;
                            String timeStr = (totalSeconds < 60) ? totalSeconds + "s ago" : (totalSeconds / 60) + "min ago";
                            context.getSource().sendFeedback(Text.literal(format("§e%s §7died §b%s §7at §f%.0f, %.0f, %.0f", entry.playerName(), timeStr, entry.x(), entry.y(), entry.z())));
                        }
                        context.getSource().sendFeedback(Text.literal("§6§l-----------------------  "));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("locate")
                            .then(ClientCommandManager.argument("player", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        for (DeathEntry entry : DeathSound.deathHistory) builder.suggest(entry.playerName());
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String target = StringArgumentType.getString(context, "player");
                                        DeathEntry found = null;
                                        for (int i = DeathSound.deathHistory.size() - 1; i >= 0; i--) {
                                            if (DeathSound.deathHistory.get(i).playerName().equalsIgnoreCase(target)) {
                                                found = DeathSound.deathHistory.get(i);
                                                break;
                                            }
                                        }
                                        if (found != null) {
                                            context.getSource().sendFeedback(Text.literal(format("§e%s's §7last §cdeath position§7: §f%.0f, %.0f, %.0f", found.playerName(), found.x(), found.y(), found.z())));
                                        } else {
                                            context.getSource().sendFeedback(Text.literal("§6[DeathSound] §fPlayer not found in history."));
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("test-effect").executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null && client.world != null) {
                            Vec3d lookVec = client.player.getRotationVec(1.0F);
                            double x = client.player.getX() + (lookVec.x * 5.0);
                            double y = client.player.getY() + (lookVec.y * 5.0);
                            double z = client.player.getZ() + (lookVec.z * 5.0);
                            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, client.world);
                            lightning.refreshPositionAfterTeleport(x, y, z);
                            lightning.setCosmetic(true);
                            client.world.addEntity(lightning.getId(), lightning);
                            context.getSource().sendFeedback(Text.literal("§6[DeathSound] §eVisual effect spawned."));
                        }
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("custom-deathsound")
                            .then(ClientCommandManager.literal("on").executes(context -> {
                                ModConfig.customSoundEnabled = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.literal("§6[DeathSound] §fCustom Sound: §aEnabled"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("off").executes(context -> {
                                ModConfig.customSoundEnabled = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.literal("§6[DeathSound] §fCustom Sound: §cDisabled"));
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("test").executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null && client.world != null) {
                            if (ModConfig.customSoundEnabled) {
                                client.world.playSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                                        SoundEvent.of(new Identifier("deathsound", "custom_" + ModConfig.lastSoundTimestamp)),
                                        SoundCategory.PLAYERS, ModConfig.volume, ModConfig.pitch, false);
                            } else {
                                SoundEvent event = switch (ModConfig.selectedSound.toLowerCase()) {
                                    case "thunder" -> SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER;
                                    case "explosion" -> SoundEvents.ENTITY_GENERIC_EXPLODE;
                                    case "shrieker" -> SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK;
                                    default -> SoundEvents.ENTITY_WITHER_SPAWN;
                                };
                                client.world.playSound(client.player.getX(), client.player.getY(), client.player.getZ(), event, SoundCategory.PLAYERS, ModConfig.volume, ModConfig.pitch, false);
                            }
                        }
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("status").executes(context -> {
                        long currentTime = System.currentTimeMillis();
                        String lastPlayed = (ModConfig.lastPlayedTime == 0) ? "§7Never" : "§e" + (currentTime - ModConfig.lastPlayedTime)  + "ms ago";
                        String soundDisplay = ModConfig.customSoundEnabled ? "§b" + ModConfig.customSoundFileName + ".ogg" : "§e" + ModConfig.selectedSound.toUpperCase();
                        context.getSource().sendFeedback(Text.literal("§6§l[ DeathSound Status ]"));
                        context.getSource().sendFeedback(Text.literal("§7Mod: " + (ModConfig.enabled ? "§aEnabled" : "§cDisabled") + " §7| Debug: " + (ModConfig.debugMode ? "§aEnabled" : "§cDisabled")));
                        context.getSource().sendFeedback(Text.literal("§7Sound: " + soundDisplay + " §7| Volume: §e" + (int)(ModConfig.volume * 100) + "% §7| Pitch: §e" + format("%.1f", ModConfig.pitch)));
                        context.getSource().sendFeedback(Text.literal("§7Filter: §b" + ModConfig.filterMode.toUpperCase() + " §7| Distance: §e" + ModConfig.maxDistanceChunks + " Chunks"));
                        context.getSource().sendFeedback(Text.literal("§7Delay: §e" + ModConfig.delayTicks + " Ticks | SpamProtect: " + (ModConfig.spamProtect ? "§aEnabled" : "§cDisabled")));
                        context.getSource().sendFeedback(Text.literal("§7Notify List: " + (ModConfig.notifyTrackedDeath ? "§aEnabled" : "§cDisabled") + " | Effects: " + (ModConfig.deathEffects ? "§aEnabled" : "§cDisabled")));
                        context.getSource().sendFeedback(Text.literal("§8> §7Last Sound: " + lastPlayed + " | List: §e" + ModConfig.trackedPlayers.size()));
                        context.getSource().sendFeedback(Text.literal("§6§l---------------------"));
                        return 1;
                    }))

                    .then(ClientCommandManager.literal("debug-mode")
                            .then(ClientCommandManager.literal("on").executes(context -> {
                                if (ModConfig.debugMode) {
                                    context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Debug Mode is already enabled!"));
                                    return 1;
                                }
                                ModConfig.debugMode = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Debug Mode: §aEnabled"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("off").executes(context -> {
                                if (!ModConfig.debugMode) {
                                    context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Debug Mode is already disabled!"));
                                    return 1;
                                }
                                ModConfig.debugMode = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Debug Mode: §cDisabled"));
                                return 1;
                            }))
                    )

                    .then(ClientCommandManager.literal("spamprotect")
                            .then(ClientCommandManager.literal("on").executes(context -> {
                                if (ModConfig.spamProtect) {
                                    context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7SpamProtect is already enabled!"));
                                    return 1;
                                }
                                ModConfig.spamProtect = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7SpamProtect: §aEnabled"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("off").executes(context -> {
                                if (!ModConfig.spamProtect) {
                                    context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7SpamProtect is already disabled!"));
                                    return 1;
                                }
                                ModConfig.spamProtect = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7SpamProtect: §cDisabled"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("cooldown")
                                    .then(ClientCommandManager.argument("ms", IntegerArgumentType.integer(0, 60000))
                                            .executes(context -> {
                                                int ms = IntegerArgumentType.getInteger(context, "ms");
                                                ModConfig.cooldownMS = ms;
                                                ModConfig.save();
                                                context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Cooldown set to: §e" + ms + "ms (" + ms/1000 + "s)"));
                                                return 1;
                                            })
                                    )
                            )
                    )

                    .then(ClientCommandManager.literal("unit")
                            .then(ClientCommandManager.literal("m")
                                    .executes(context -> {
                                        ModConfig.useFeet = false;
                                        ModConfig.save();
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("ft")
                                    .executes(context -> {
                                        ModConfig.useFeet = true;
                                        ModConfig.save();
                                        return 1;
                                    }))
                    )
                    .then(ClientCommandManager.literal("filter")
                            .then(ClientCommandManager.literal("all")
                                    .executes(context -> {
                                        ModConfig.filterMode = "everyone";
                                        ModConfig.save();
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("others")
                                    .executes(context -> {
                                        ModConfig.filterMode = "others";
                                        ModConfig.save();
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("list")
                                    .executes(context -> {
                                        ModConfig.filterMode = "list";
                                        ModConfig.save();
                                        return 1;
                                    }))
                    )
                    .then(ClientCommandManager.literal("delay")
                            .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(0, 100))
                                    .executes(context -> {
                                        int ticks = IntegerArgumentType.getInteger(context, "ticks");
                                        if (ModConfig.delayTicks == ticks) {
                                            context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Delay is already set to §e" + ticks + " ticks!"));
                                            return 1;
                                        }
                                        ModConfig.delayTicks = ticks;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.literal("§6[DeathSound] §7Delay set to §e" + ticks + " ticks"));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("player")
                            .then(ClientCommandManager.literal("add")
                                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                            .executes(context -> {
                                                ModConfig.trackedPlayers.add(StringArgumentType.getString(context, "name"));
                                                ModConfig.save(); return 1;
                                            })))
                            .then(ClientCommandManager.literal("remove")
                                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                            .executes(context -> {
                                                ModConfig.trackedPlayers.remove(StringArgumentType.getString(context, "name"));
                                                ModConfig.save(); return 1;
                                            })))
                    )
                    .then(ClientCommandManager.literal("reset").executes(context -> { ModConfig.resetToDefaults(); ModConfig.save(); return 1; }))
            );
            dispatcher.register(ClientCommandManager.literal("dsound").redirect(node));
            dispatcher.register(ClientCommandManager.literal("death").redirect(node));
            dispatcher.register(ClientCommandManager.literal("deaths").redirect(node));
        });
    }
}