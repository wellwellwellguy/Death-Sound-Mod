package net.wellwellwell.sound;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import static java.lang.String.format;


public class DeathSound implements ClientModInitializer {

    public static final List<DeathEntry> deathHistory = new ArrayList<>();
    public record DeathEntry(String playerName, double x, double y, double z, long timestamp) {}
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("DeathSound");

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        LOGGER.info("DeathSound Mod has been loaded!");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var node = dispatcher.register(ClientCommandManager.literal("deathsound")
                    .then(ClientCommandManager.literal("on").executes(context -> {
                        if (ModConfig.enabled) {
                            context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Death Sounds are already enabled!"));
                            return 1;
                        }
                        ModConfig.enabled = true;
                        ModConfig.save();
                        context.getSource().sendFeedback(Component.literal("§6[DeathSound]§7 Mod §aenabled"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("off").executes(context -> {
                        if (!ModConfig.enabled) {
                            context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7DeathSounds are already disabled!"));
                            return 1;
                        }
                        ModConfig.enabled = false;
                        ModConfig.save();
                        context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Mod §cdisabled"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("distance")
                            .then(ClientCommandManager.argument("chunks", IntegerArgumentType.integer(1, 21))
                                    .executes(context -> {
                                        int dist = IntegerArgumentType.getInteger(context, "chunks");
                                        ModConfig.maxDistanceChunks = dist;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Distance: " + (dist > 20 ? "§aUnlimited" : "§b" + dist + " Chunks")));
                                        return 1;
                                    }))
                    )
                    .then(ClientCommandManager.literal("volume")
                            .then(ClientCommandManager.argument("value", IntegerArgumentType.integer(1, 100))
                                    .executes(context -> {
                                        int vol = IntegerArgumentType.getInteger(context, "value");
                                        ModConfig.volume = vol / 100.0f;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Volume set to §e" + vol + "%"));
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
                                        context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Pitch set to §e" + pitch));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("history").executes(context -> {
                        if (DeathSound.deathHistory.isEmpty()) {
                            context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7No deaths recorded yet."));
                            return 1;
                        }
                        context.getSource().sendFeedback(Component.literal("§6§l----- Death History -----"));
                        for (DeathEntry entry : DeathSound.deathHistory) {
                            long totalSeconds = (System.currentTimeMillis() - entry.timestamp()) / 1000;
                            String timeStr = (totalSeconds < 60) ? totalSeconds + "s ago" : (totalSeconds / 60) + "min ago";
                            context.getSource().sendFeedback(Component.literal(format("§e%s §7died §b%s §7at §f%.0f, %.0f, %.0f", entry.playerName(), timeStr, entry.x(), entry.y(), entry.z())));
                        }
                        context.getSource().sendFeedback(Component.literal("§6§l-----------------------  "));
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
                                            context.getSource().sendFeedback(Component.literal(format("§e%s's §7last §cdeath position§7: §f%.0f, %.0f, %.0f", found.playerName(), found.x(), found.y(), found.z())));
                                        } else {
                                            context.getSource().sendFeedback(Component.literal("§6[DeathSound] §fPlayer not found in history."));
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("test-effect").executes(context -> {
                        Minecraft client = Minecraft.getInstance();

                        var player = client.player;
                        var level = client.level;

                        if (player != null && level != null) {
                            Vec3 lookVec = Vec3.directionFromRotation(player.getRotationVector());

                            double x = player.getX() + (lookVec.x * 5.0);
                            double y = player.getY() + (lookVec.y * 5.0) + 1.5;
                            double z = player.getZ() + (lookVec.z * 5.0);

                            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                            lightning.setPos(x, y, z);
                            lightning.setVisualOnly(true);
                            level.addEntity(lightning);

                            context.getSource().sendFeedback(Component.literal("§6[DeathSound] §eVisual effect spawned."));
                        }
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("test").executes(context -> {
                        Minecraft client = Minecraft.getInstance();

                        if (client.player != null && client.level != null) {
                            SoundEvent event = switch (ModConfig.selectedSound.toLowerCase()) {
                                case "thunder" -> SoundEvents.LIGHTNING_BOLT_THUNDER;
                                case "explosion" -> SoundEvents.GENERIC_EXPLODE.value();
                                case "shrieker" -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
                                default -> SoundEvents.WITHER_SPAWN;
                            };

                            client.level.playLocalSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                                    event,
                                    net.minecraft.sounds.SoundSource.MASTER,
                                    ModConfig.volume, ModConfig.pitch, false);
                        }
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("status").executes(context -> {
                        long currentTime = System.currentTimeMillis();
                        String lastPlayed = (ModConfig.lastPlayedTime == 0) ? "§7Never" : "§e" + (currentTime - ModConfig.lastPlayedTime)  + "ms ago";
                        String soundDisplay = ModConfig.selectedSound.toUpperCase();
                        context.getSource().sendFeedback(Component.literal("§6§l[ DeathSound Status ]"));
                        context.getSource().sendFeedback(Component.literal("§7Mod: " + (ModConfig.enabled ? "§aEnabled" : "§cDisabled") + " §7| Debug: " + (ModConfig.debugMode ? "§aEnabled" : "§cDisabled")));
                        context.getSource().sendFeedback(Component.literal("§7Sound: " + soundDisplay + " §7| Volume: §e" + (int)(ModConfig.volume * 100) + "% §7| Pitch: §e" + format("%.1f", ModConfig.pitch)));
                        context.getSource().sendFeedback(Component.literal("§7Filter: §b" + ModConfig.filterMode.toUpperCase() + " §7| Distance: §e" + ModConfig.maxDistanceChunks + " Chunks"));
                        context.getSource().sendFeedback(Component.literal("§7Delay: §e" + ModConfig.delayTicks + " Ticks | SpamProtect: " + (ModConfig.spamProtect ? "§aEnabled" : "§cDisabled")));
                        context.getSource().sendFeedback(Component.literal("§7Notify List: " + (ModConfig.notifyTrackedDeath ? "§aEnabled" : "§cDisabled") + " | Effects: " + (ModConfig.deathEffects ? "§aEnabled" : "§cDisabled")));
                        context.getSource().sendFeedback(Component.literal("§8> §7Last Sound: " + lastPlayed + " | List: §e" + ModConfig.trackedPlayers.size()));
                        context.getSource().sendFeedback(Component.literal("§6§l---------------------"));
                        return 1;
                    }))

                    .then(ClientCommandManager.literal("debug-mode")
                            .then(ClientCommandManager.literal("on").executes(context -> {
                                if (ModConfig.debugMode) {
                                    context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Debug Mode is already enabled!"));
                                    return 1;
                                }
                                ModConfig.debugMode = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Debug Mode: §aEnabled"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("off").executes(context -> {
                                if (!ModConfig.debugMode) {
                                    context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Debug Mode is already disabled!"));
                                    return 1;
                                }
                                ModConfig.debugMode = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Debug Mode: §cDisabled"));
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("spamprotect")
                            .then(ClientCommandManager.literal("on").executes(context -> {
                                if (ModConfig.spamProtect) {
                                    context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7SpamProtect is already enabled!"));
                                    return 1;
                                }
                                ModConfig.spamProtect = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7SpamProtect: §aEnabled"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("off").executes(context -> {
                                if (!ModConfig.spamProtect) {
                                    context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7SpamProtect is already disabled!"));
                                    return 1;
                                }
                                ModConfig.spamProtect = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7SpamProtect: §cDisabled"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("cooldown")
                                    .then(ClientCommandManager.argument("ms", IntegerArgumentType.integer(0, 60000))
                                            .executes(context -> {
                                                int ms = IntegerArgumentType.getInteger(context, "ms");
                                                ModConfig.cooldownMS = ms;
                                                ModConfig.save();
                                                context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Cooldown set to: §e" + ms + "ms (" + ms/1000 + "s)"));
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
                                            context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Delay is already set to §e" + ticks + " ticks!"));
                                            return 1;
                                        }
                                        ModConfig.delayTicks = ticks;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.literal("§6[DeathSound] §7Delay set to §e" + ticks + " ticks"));
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
        });
    }
}