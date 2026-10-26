package net.wellwellwell.sound;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class DeathSound implements ClientModInitializer {

    private static final String PACK_NAME = "DeathSound-Resources";
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
                    .then(ClientCommandManager.literal("stop").executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client != null && client.getSoundManager() != null) {
                            client.getSoundManager().stop(new ResourceLocation("minecraft", "entity.wither.spawn"), null);
                            client.getSoundManager().stop(new ResourceLocation("minecraft", "entity.lightning_bolt.thunder"), null);
                            client.getSoundManager().stop(new ResourceLocation("minecraft", "entity.generic.explode"), null);
                            client.getSoundManager().stop(new ResourceLocation("minecraft", "block.sculk_shrieker.shriek"), null);

                            context.getSource().sendFeedback(Component.literal("§6[DeathSound] §aAll death sounds stopped."));
                            System.out.println("Stopped all death sounds.");
                        }
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("test").executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client.player != null && client.level != null) {
                            SoundEvent event = switch (ModConfig.selectedSound.toLowerCase()) {
                                case "thunder" -> SoundEvents.LIGHTNING_BOLT_THUNDER;
                                case "explosion" -> SoundEvents.GENERIC_EXPLODE;
                                case "shrieker" -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
                                default -> SoundEvents.WITHER_SPAWN;
                            };
                            client.level.playLocalSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                                    event, net.minecraft.sounds.SoundSource.PLAYERS, ModConfig.volume, ModConfig.pitch, false);
                        }
                        return 1;
                    }))
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