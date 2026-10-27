package com.wellwellwell.sound;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.wellwellwell.sound.Screen.CustomSoundScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;



import java.io.File;
import java.util.*;

public class DeathSound implements ClientModInitializer {

    public static final Identifier CLASSIC_HURT_ID = Identifier.fromNamespaceAndPath("deathsound", "classic_hurt");
    public static final SoundEvent CLASSIC_HURT = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            CLASSIC_HURT_ID,
            SoundEvent.createVariableRangeEvent(CLASSIC_HURT_ID)
    );

    public static final Identifier CUSTOM_SOUND_ID = Identifier.fromNamespaceAndPath("deathsound", "custom_ogg");
    public static final SoundEvent CUSTOM_SOUND_EVENT = SoundEvent.createVariableRangeEvent(CUSTOM_SOUND_ID);

    public static final List<DeathEntry> deathHistory = new ArrayList<>();
    public record DeathEntry(String playerName, double x, double y, double z, long time, String dimension) {}

    @Override
    public void onInitializeClient() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, CUSTOM_SOUND_ID, CUSTOM_SOUND_EVENT);
        ModConfig.load();
        createCustomFolder();
        CustomSoundPack.init();
        StatsManager.load();


        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommands.literal("deathsound")

                    .then(ClientCommands.literal("on").executes(context -> {
                        if (ModConfig.enabled) {
                            context.getSource().sendFeedback(Component.translatable("deathsound.command.main.already_on"));
                        } else {
                            ModConfig.enabled = true;
                            ModConfig.save();
                            context.getSource().sendFeedback(Component.translatable("deathsound.command.main.set_on"));
                        }
                        return 1;
                    }))

                    .then(ClientCommands.literal("stop").executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client != null && client.getSoundManager() != null) {

                            client.getSoundManager().stop(Identifier.fromNamespaceAndPath("deathsound", "custom_ogg"), null);

                            String selected = ModConfig.selectedSound.toLowerCase().trim();
                            Identifier selectedId = null;

                            switch (selected) {
                                case "classic_hurt"      -> selectedId = Identifier.fromNamespaceAndPath("deathsound", "classic_hurt");
                                case "wither"            -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.wither.spawn");
                                case "thunder"           -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.lightning_bolt.thunder");
                                case "explosion"         -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.generic.explode");
                                case "shrieker"          -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "block.sculk_shrieker.shriek");
                                case "warden_heartbeat"  -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.warden.heartbeat");
                                case "totem"             -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "item.totem.use");
                                case "anvil"             -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "block.anvil.land");
                                case "ghast"             -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.ghast.scream");
                                case "enderdragon"       -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.ender_dragon.death");
                                case "elder_guardian"    -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.elder_guardian.curse");
                                case "amethyst"          -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "block.amethyst_block.break");
                                case "villager_no"       -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.villager.no");
                                case "bell"              -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "block.bell.use");
                                case "creeper"           -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.creeper.primed");
                                case "level_up"          -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.player.levelup");
                                case "goat_scream"       -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.goat.screaming.ambient");
                                case "plinth"            -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "block.trial_spawner.ominous_activate");
                                case "enderman"          -> selectedId = Identifier.fromNamespaceAndPath("minecraft", "entity.enderman.teleport");
                            }

                            if (selectedId != null) {
                                client.getSoundManager().stop(selectedId, null);
                            }

                            net.minecraft.resources.Identifier[] panicStopList = {   //why did I name it like this
                                    Identifier.fromNamespaceAndPath("minecraft", "entity.wither.spawn"),
                                    Identifier.fromNamespaceAndPath("minecraft", "entity.lightning_bolt.thunder"),
                                    Identifier.fromNamespaceAndPath("minecraft", "entity.ender_dragon.death"),
                                    Identifier.fromNamespaceAndPath("minecraft", "block.sculk_shrieker.shriek")
                            };

                            for (net.minecraft.resources.Identifier id : panicStopList) {
                                client.getSoundManager().stop(id, null);
                            }

                            context.getSource().sendFeedback(Component.translatable("deathsound.command.stop.success"));
                        }
                        return 1;
                    }))

                    .then(ClientCommands.literal("off").executes(context -> {
                        if (!ModConfig.enabled) {
                            context.getSource().sendFeedback(Component.translatable("deathsound.command.main.already_off"));
                        } else {
                            ModConfig.enabled = false;
                            ModConfig.save();
                            context.getSource().sendFeedback(Component.translatable("deathsound.command.main.set_off"));
                        }
                        return 1;
                    }))
                    .then(ClientCommands.literal("useDistance")
                            .then(ClientCommands.literal("true").executes(context -> {
                                ModConfig.useDistance = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.chat.use_distance.on"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("false").executes(context -> {
                                ModConfig.useDistance = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.chat.use_distance.off"));
                                return 1;
                            }))
                    )

                    .then(ClientCommands.literal("showMenuButton")
                            .then(ClientCommands.literal("true").executes(context -> {
                                ModConfig.showMenuButton = true;
                                ModConfig.save();
                                Component feedback = Component.translatable("deathsound.screen.menu_button.title",
                                        Component.translatable("deathsound.screen.menu_button.shown"));

                                context.getSource().sendFeedback(feedback);
                                return 1;
                            }))
                            .then(ClientCommands.literal("false").executes(context -> {
                                ModConfig.showMenuButton = false;
                                ModConfig.save();
                                Component feedback = Component.translatable("deathsound.screen.menu_button.title",
                                        Component.translatable("deathsound.screen.menu_button.hidden"));

                                context.getSource().sendFeedback(feedback);
                                return 1;
                            }))
                    )
                    .then(ClientCommands.literal("volume")
                            .then(ClientCommands.argument("value", IntegerArgumentType.integer(0, 100))
                                    .executes(context -> {
                                        int vol = IntegerArgumentType.getInteger(context, "value");
                                        float volFloat = vol / 100.0f;
                                        if (ModConfig.volume == volFloat) {
                                            context.getSource().sendFeedback(Component.translatable("deathsound.command.volume.already_set", vol));
                                            return 1;
                                        }
                                        ModConfig.volume = volFloat;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.volume.set", vol));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommands.literal("pitch")
                            .then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.0001f, 2.0f))
                                    .executes(context -> {
                                        float pitch = FloatArgumentType.getFloat(context, "value");
                                        if (ModConfig.pitch == pitch) {
                                            context.getSource().sendFeedback(Component.translatable("deathsound.command.pitch.already_set", pitch));
                                            return 1;
                                        }
                                        ModConfig.pitch = pitch;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.pitch.set", pitch));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommands.literal("distance")
                            .then(ClientCommands.argument("chunks", IntegerArgumentType.integer(1, 100))
                                    .executes(context -> {
                                        int dist = IntegerArgumentType.getInteger(context, "chunks");
                                        if (ModConfig.maxDistanceChunks == dist) {
                                            context.getSource().sendFeedback(Component.translatable("deathsound.command.distance.already_set", dist));
                                            return 1;
                                        }
                                        ModConfig.maxDistanceChunks = dist;
                                        ModConfig.save();

                                        if (dist >= 100) {
                                            context.getSource().sendFeedback(Component.translatable("deathsound.command.distance.unlimited"));
                                        } else {
                                            context.getSource().sendFeedback(Component.translatable("deathsound.command.distance.set", dist));
                                        }
                                        return 1;
                                    }))
                    )

                    .then(ClientCommands.literal("delay")
                            .then(ClientCommands.argument("ticks", IntegerArgumentType.integer(0, 100))
                                    .executes(context -> {
                                        int ticks = IntegerArgumentType.getInteger(context, "ticks");
                                        ModConfig.delayTicks = ticks;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.delay.set", ticks));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommands.literal("selectedEffect")
                            .then(ClientCommands.literal("lightning").executes(context -> {
                                ModConfig.selectedEffect = "lightning";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.effect.set.success", "lightning"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("explosion").executes(context -> {
                                ModConfig.selectedEffect = "explosion";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.effect.set.success", "explosion"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("bad_omen").executes(context -> {
                                ModConfig.selectedEffect = "bad_omen";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.effect.set.success", "bad_omen"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("smoke").executes(context -> {
                                ModConfig.selectedEffect = "smoke";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.effect.set.success", "smoke"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("shriek").executes(context -> {
                                ModConfig.selectedEffect = "shriek";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.effect.set.success", "shriek"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("sonic_boom").executes(context -> {
                                ModConfig.selectedEffect = "sonic_boom";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.effect.set.success", "sonic_boom"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("totem").executes(context -> {
                                ModConfig.selectedEffect = "totem";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.effect.set.success", "totem"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("firework").executes(context -> {
                                ModConfig.selectedEffect = "firework";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.effect.set.success", "firework"));
                                return 1;
                            }))
                    )
                    .then(ClientCommands.literal("sound")
                            .then(ClientCommands.literal("classic_hurt").executes(context -> {
                                ModConfig.selectedSound = "classic_hurt";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Classic Hurt"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("wither").executes(context -> {
                                ModConfig.selectedSound = "wither";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Wither"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("thunder").executes(context -> {
                                ModConfig.selectedSound = "thunder";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Thunder"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("explosion").executes(context -> {
                                ModConfig.selectedSound = "explosion";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Explosion"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("shrieker").executes(context -> {
                                ModConfig.selectedSound = "shrieker";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Shrieker"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("warden_heartbeat").executes(context -> {
                                ModConfig.selectedSound = "warden_heartbeat";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Warden Heartbeat"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("totem").executes(context -> {
                                ModConfig.selectedSound = "totem";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Totem"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("anvil").executes(context -> {
                                ModConfig.selectedSound = "anvil";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Anvil"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("ghast").executes(context -> {
                                ModConfig.selectedSound = "ghast";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Ghast"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("enderdragon").executes(context -> {
                                ModConfig.selectedSound = "enderdragon";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Enderdragon"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("elder_guardian").executes(context -> {
                                ModConfig.selectedSound = "elder_guardian";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Elder Guardian"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("amethyst").executes(context -> {
                                ModConfig.selectedSound = "amethyst";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Amethyst"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("villager_no").executes(context -> {
                                ModConfig.selectedSound = "villager_no";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Villager No"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("bell").executes(context -> {
                                ModConfig.selectedSound = "bell";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Bell"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("creeper").executes(context -> {
                                ModConfig.selectedSound = "creeper";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Creeper"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("level_up").executes(context -> {
                                ModConfig.selectedSound = "level_up";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Level Up"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("goat_scream").executes(context -> {
                                ModConfig.selectedSound = "goat_scream";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Goat Scream"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("plinth").executes(context -> {
                                ModConfig.selectedSound = "plinth";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Plinth"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("enderman").executes(context -> {
                                ModConfig.selectedSound = "enderman";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", "Enderman"));
                                return 1;
                            }))
                    )
                    .then(ClientCommands.literal("monitor")
                            .then(ClientCommands.literal("deaths")
                                    .then(ClientCommands.literal("on").executes(context -> {
                                        ModConfig.monitorDeaths = true;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.monitor.on"));
                                        return 1;
                                    }))
                                    .then(ClientCommands.literal("off").executes(context -> {
                                        ModConfig.monitorDeaths = false;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.monitor.off"));
                                        return 1;
                                    })))
                            .then(ClientCommands.literal("pops")
                                    .then(ClientCommands.literal("on").executes(context -> {
                                        ModConfig.monitorPops = true;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.monitor.pops.on"));
                                        return 1;
                                    }))
                                    .then(ClientCommands.literal("off").executes(context -> {
                                        ModConfig.monitorPops = false;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.monitor.pops.off"));
                                        return 1;
                                    })))
                    )
                    .then(ClientCommands.literal("spamprotect")
                            .then(ClientCommands.literal("on").executes(context -> {
                                if (ModConfig.spamProtect) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.spamprotect.already_on"));
                                } else {
                                    ModConfig.spamProtect = true;
                                    ModConfig.save();
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.spamprotect.set_on"));
                                }
                                return 1;
                            }))
                            .then(ClientCommands.literal("off").executes(context -> {
                                if (!ModConfig.spamProtect) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.spamprotect.already_off"));
                                } else {
                                    ModConfig.spamProtect = false;
                                    ModConfig.save();
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.spamprotect.set_off"));
                                }
                                return 1;
                            }))
                            .then(ClientCommands.literal("cooldown")
                                    .then(ClientCommands.argument("ms", IntegerArgumentType.integer(1, 50000))
                                            .executes(context -> {
                                                int ms = IntegerArgumentType.getInteger(context, "ms");

                                                ModConfig.cooldownMS = ms;
                                                ModConfig.save();

                                                context.getSource().sendFeedback(Component.translatable("deathsound.command.cooldown.set", ms, (ms/1000)));

                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(ClientCommands.literal("filter")
                            .then(ClientCommands.literal("all").executes(context -> {
                                if (ModConfig.filterMode.equals("everyone")) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.filter.already_all"));
                                    return 1;
                                }
                                ModConfig.filterMode = "everyone";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.filter.set_all"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("others").executes(context -> {
                                if (ModConfig.filterMode.equals("others")) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.filter.already_others"));
                                    return 1;
                                }
                                ModConfig.filterMode = "others";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.filter.set_others"));
                                return 1;
                            }))
                            .then(ClientCommands.literal("list").executes(context -> {
                                if (ModConfig.filterMode.equals("list")) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.filter.already_list"));
                                    return 1;
                                }
                                ModConfig.filterMode = "list";
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.filter.set_list"));
                                return 1;
                            })))

                    .then(ClientCommands.literal("unit")
                            .then(ClientCommands.literal("meters").executes(context -> {
                                if (ModConfig.useFeet) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.unit.already_meters"));
                                } else {
                                    ModConfig.useFeet = false;
                                    ModConfig.save();
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.unit.set_meters"));
                                }
                                return 1;
                            }))
                            .then(ClientCommands.literal("feet").executes(context -> {
                                if (!ModConfig.useFeet) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.unit.already_feet"));
                                } else {
                                    ModConfig.useFeet = true;
                                    ModConfig.save();
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.unit.set_feet"));
                                }
                                return 1;
                            })))

                    .then(ClientCommands.literal("deathEffects")
                            .then(ClientCommands.literal("off").executes(context -> {
                                ModConfig.deathEffects = false;
                                context.getSource().sendFeedback(Component.translatable("deathsound.chat.effects_off"));
                                ModConfig.save();
                                return 1;
                            }))
                            .then(ClientCommands.literal("on").executes(context -> {
                                ModConfig.deathEffects = true;
                                context.getSource().sendFeedback(Component.translatable("deathsound.chat.effects_on"));
                                ModConfig.save();
                                return 1;
                            })))

                    .then(ClientCommands.literal("setEffect")
                            .then(ClientCommands.literal("lightning").executes(context -> setDeathEffect(context, "lightning")))
                            .then(ClientCommands.literal("explosion").executes(context -> setDeathEffect(context, "explosion")))
                            .then(ClientCommands.literal("bad_omen").executes(context -> setDeathEffect(context, "bad_omen")))
                            .then(ClientCommands.literal("smoke").executes(context -> setDeathEffect(context, "smoke")))
                            .then(ClientCommands.literal("shriek").executes(context -> setDeathEffect(context, "shriek")))
                            .then(ClientCommands.literal("sonic_boom").executes(context -> setDeathEffect(context, "sonic_boom")))
                            .then(ClientCommands.literal("totem").executes(context -> setDeathEffect(context, "totem")))
                            .then(ClientCommands.literal("firework").executes(context -> setDeathEffect(context, "firework")))
                    )


                    .then(ClientCommands.literal("history").executes(context -> {
                        if (deathHistory.isEmpty()) {
                            context.getSource().sendFeedback(Component.translatable("deathsound.command.history.empty"));
                            return 1;
                        }

                        context.getSource().sendFeedback(Component.translatable("deathsound.command.history.header"));

                        for (DeathEntry entry : deathHistory) {
                            long totalSeconds = (System.currentTimeMillis() - entry.time()) / 1000;
                            Component timeStr = totalSeconds < 60 ?
                                    Component.translatable("deathsound.command.history.time.seconds", totalSeconds) :
                                    Component.translatable("deathsound.command.history.time.minutes", (totalSeconds / 60), (totalSeconds % 60));

                            context.getSource().sendFeedback(Component.translatable("deathsound.command.history.entry",
                                    entry.playerName(), timeStr, (int)entry.x(), (int)entry.y(), (int)entry.z()));
                        }

                        context.getSource().sendFeedback(Component.translatable("deathsound.command.history.footer"));
                        return 1;
                    }))


                    .then(ClientCommands.literal("stop").executes(context -> {
                        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MASTER);
                        context.getSource().sendFeedback(Component.translatable("deathsound.command.stop.success"));
                        return 1;
                    }))


                    .then(ClientCommands.literal("list")
                            .executes(context -> {
                                if (ModConfig.trackedPlayers.isEmpty()) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.screen.manage_list.input_label"));
                                } else {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.screen.manage_list.header"));
                                    context.getSource().sendFeedback(Component.literal("§b" + String.join(", ", ModConfig.trackedPlayers)));
                                }
                                return 1;
                            })
                            .then(ClientCommands.literal("add")
                                    .then(ClientCommands.argument("name", StringArgumentType.string()).executes(context -> {
                                        String name = StringArgumentType.getString(context, "name");
                                        if (!ModConfig.trackedPlayers.contains(name)) {
                                            ModConfig.trackedPlayers.add(name);
                                            ModConfig.save();
                                            context.getSource().sendFeedback(Component.translatable("deathsound.command.player_added", name));
                                        }
                                        return 1;
                                    })))
                            .then(ClientCommands.literal("remove")
                                    .then(ClientCommands.argument("name", StringArgumentType.string())
                                            .suggests((context, builder) -> {
                                                for (String name : ModConfig.trackedPlayers) {
                                                    if (name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                                                        builder.suggest(name);
                                                    }
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                if (ModConfig.trackedPlayers.remove(name)) {
                                                    ModConfig.save();
                                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.player_removed", name));
                                                }
                                                return 1;
                                            })))
                            .then(ClientCommands.literal("clear").executes(context -> {
                                ModConfig.trackedPlayers.clear();
                                ModConfig.save();
                                context.getSource().sendFeedback(Component.translatable("deathsound.screen.manage_list.clear"));
                                return 1;
                            }))
                    )
                    .then(ClientCommands.literal("setSound")
                            .then(ClientCommands.literal("classic_hurt").executes(context -> setDeathSound(context, "classic_hurt")))
                            .then(ClientCommands.literal("wither").executes(context -> setDeathSound(context, "wither")))
                            .then(ClientCommands.literal("thunder").executes(context -> setDeathSound(context, "thunder")))
                            .then(ClientCommands.literal("explosion").executes(context -> setDeathSound(context, "explosion")))
                            .then(ClientCommands.literal("shrieker").executes(context -> setDeathSound(context, "shrieker")))
                            .then(ClientCommands.literal("warden_heartbeat").executes(context -> setDeathSound(context, "warden_heartbeat")))
                            .then(ClientCommands.literal("totem").executes(context -> setDeathSound(context, "totem")))
                            .then(ClientCommands.literal("anvil").executes(context -> setDeathSound(context, "anvil")))
                            .then(ClientCommands.literal("ghast").executes(context -> setDeathSound(context, "ghast")))
                            .then(ClientCommands.literal("enderdragon").executes(context -> setDeathSound(context, "enderdragon")))
                            .then(ClientCommands.literal("elder_guardian").executes(context -> setDeathSound(context, "elder_guardian")))
                            .then(ClientCommands.literal("amethyst").executes(context -> setDeathSound(context, "amethyst")))
                            .then(ClientCommands.literal("villager_no").executes(context -> setDeathSound(context, "villager_no")))
                            .then(ClientCommands.literal("bell").executes(context -> setDeathSound(context, "bell")))
                            .then(ClientCommands.literal("creeper").executes(context -> setDeathSound(context, "creeper")))
                            .then(ClientCommands.literal("level_up").executes(context -> setDeathSound(context, "level_up")))
                            .then(ClientCommands.literal("goat_scream").executes(context -> setDeathSound(context, "goat_scream")))
                            .then(ClientCommands.literal("plinth").executes(context -> setDeathSound(context, "plinth")))
                            .then(ClientCommands.literal("enderman").executes(context -> setDeathSound(context, "enderman")))
                    )
                    .then(ClientCommands.literal("test").executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client.player != null && client.level != null) {

                            String selected = ModConfig.customSoundEnabled ? "custom_ogg" : ModConfig.selectedSound.toLowerCase().trim();
                            SoundEvent event;

                            switch (selected) {
                                case "custom_ogg"        -> event = new SoundEvent(Identifier.fromNamespaceAndPath("deathsound", "custom_ogg"), Optional.empty());
                                case "classic_hurt"      -> event = DeathSound.CLASSIC_HURT;
                                case "wither"            -> event = SoundEvents.WITHER_SPAWN;
                                case "thunder"           -> event = SoundEvents.LIGHTNING_BOLT_THUNDER;
                                case "explosion"         -> event = SoundEvents.GENERIC_EXPLODE.value();
                                case "shrieker"          -> event = SoundEvents.SCULK_SHRIEKER_SHRIEK;
                                case "warden_heartbeat"  -> event = SoundEvents.WARDEN_HEARTBEAT;
                                case "totem"             -> event = SoundEvents.TOTEM_USE;
                                case "anvil"             -> event = SoundEvents.ANVIL_LAND;
                                case "ghast"             -> event = SoundEvents.GHAST_SCREAM;
                                case "enderdragon"       -> event = SoundEvents.ENDER_DRAGON_DEATH;
                                case "elder_guardian"    -> event = SoundEvents.ELDER_GUARDIAN_CURSE;
                                case "amethyst"          -> event = SoundEvents.AMETHYST_BLOCK_BREAK;
                                case "villager_no"       -> event = SoundEvents.VILLAGER_NO;
                                case "bell"              -> event = SoundEvents.BELL_RESONATE;
                                case "creeper"           -> event = SoundEvents.CREEPER_PRIMED;
                                case "level_up"          -> event = SoundEvents.PLAYER_LEVELUP;
                                case "goat_scream"       -> event = SoundEvents.GOAT_SCREAMING_AMBIENT;
                                case "plinth"            -> event = SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE;
                                case "enderman"          -> event = SoundEvents.ENDERMAN_TELEPORT;
                                default                  -> event = SoundEvents.WITHER_SPAWN;
                            }

                            double posX = client.player.getX();
                            double posY = ModConfig.useFeet ? client.player.getY() : client.player.getEyeY();
                            double posZ = client.player.getZ();

                            client.level.playLocalSound(
                                    posX, posY, posZ,
                                    event,
                                    net.minecraft.sounds.SoundSource.MASTER,
                                    ModConfig.volume,
                                    ModConfig.pitch,
                                    ModConfig.useDistance
                            );

                            if (ModConfig.customSoundEnabled) {
                                String displayName = ModConfig.customSoundFileName.replace(".ogg", "");
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.test.custom", displayName));
                            } else {
                                String displayName = selected.toUpperCase();
                                context.getSource().sendFeedback(Component.translatable("deathsound.command.test.default", displayName));
                            }
                        }
                        return 1;
                    }))
                    .then(ClientCommands.literal("testEffect").executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client.player == null || client.level == null) return 0;

                        Vec3 look = client.player.getLookAngle();
                        double px = client.player.getX() + look.x * 5.0;
                        double py = client.player.getY() + look.y * 5.0;
                        double pz = client.player.getZ() + look.z * 5.0;

                        String effect = ModConfig.selectedEffect == null ? "lightning" : ModConfig.selectedEffect.toLowerCase();

                        if (effect.equals("lightning")) {
                            LightningBolt lightning = new LightningBolt(EntityTypes.LIGHTNING_BOLT, client.level);
                            lightning.setPos(px, py, pz);
                            lightning.setVisualOnly(true);
                            lightning.setSilent(true);
                            client.level.addEntity(lightning);
                        } else {
                            ParticleOptions particleToSpawn;

                            switch (effect) {
                                case "explosion"    -> particleToSpawn = ParticleTypes.EXPLOSION;
                                case "bad_omen"     -> particleToSpawn = ParticleTypes.TRIAL_OMEN;
                                case "smoke"        -> particleToSpawn = ParticleTypes.LARGE_SMOKE;
                                case "sonic_boom"   -> particleToSpawn = ParticleTypes.SONIC_BOOM;
                                case "totem"        -> particleToSpawn = ParticleTypes.TOTEM_OF_UNDYING;
                                case "firework"     -> particleToSpawn = ParticleTypes.FIREWORK;
                                case "shriek"       -> particleToSpawn = new ShriekParticleOption(0);
                                default             -> particleToSpawn = ParticleTypes.EXPLOSION;
                            }

                            if (effect.equals("sonic_boom") || effect.equals("shriek")) {
                                client.level.addParticle(particleToSpawn, px, py + 1.0, pz, 0, 1, 0);
                            } else {
                                java.util.Random random = new java.util.Random();
                                int count = effect.equals("smoke") ? 50 : 70;
                                count = (int) Math.round(count * ModConfig.particleMultiplier);

                                if (ModConfig.particleMultiplier >= 1000.0) {
                                    System.out.println("[Death Sound] Warning: High particle multiplier might cause lag.");
                                }

                                for (int i = 0; i < count; i++) {
                                    double offsetX = (random.nextDouble() - 0.5) * 1.5;
                                    double offsetY = random.nextDouble() * 2.0;
                                    double offsetZ = (random.nextDouble() - 0.5) * 1.5;

                                    double speedX = (random.nextDouble() - 0.5) * 0.2;
                                    double speedY = (random.nextDouble() - 0.5) * 0.2;
                                    double speedZ = (random.nextDouble() - 0.5) * 0.2;

                                    client.level.addParticle(particleToSpawn, px + offsetX, py + offsetY, pz + offsetZ, speedX, speedY, speedZ);
                                }
                            }
                            context.getSource().sendFeedback(Component.translatable("deathsound.command.test_effect.success"));
                        }

                        return 1;
                    }))

                    .then(ClientCommands.literal("particleMultiplier")
                            .then(ClientCommands.argument("value", DoubleArgumentType.doubleArg(0.01, 30000000))
                                    .executes(context -> {
                                        double particleMultiplier = DoubleArgumentType.getDouble(context, "value");
                                        ModConfig.particleMultiplier = particleMultiplier;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.particle_multiplier.set", particleMultiplier));
                                        return 1;
                                    })
                            )
                    )


                    .then(ClientCommands.literal("locate")
                            .then(ClientCommands.argument("player", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        for (DeathEntry entry : deathHistory) {
                                            if (entry.playerName().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                                                builder.suggest(entry.playerName());
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String target = StringArgumentType.getString(context, "player");
                                        DeathEntry latest = null;

                                        for (int i = deathHistory.size() - 1; i >= 0; i--) {
                                            if (deathHistory.get(i).playerName().equalsIgnoreCase(target)) {
                                                latest = deathHistory.get(i);
                                                break;
                                            }
                                        }

                                        if (latest == null) {
                                            context.getSource().sendFeedback(Component.translatable("deathsound.command.locate.not_found"));
                                            return 0;
                                        }

                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.locate.success",
                                                latest.playerName(),(int) latest.x(),(int) latest.y(),(int) latest.z()));

                                        return 1;
                                    })
                            )
                    )

                    .then(ClientCommands.literal("customDeathsounds")
                            .then(ClientCommands.literal("on").executes(context -> {
                                if (ModConfig.customSoundEnabled) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.custom_sound.already_on"));
                                } else {
                                    ModConfig.customSoundEnabled = true;
                                    ModConfig.save();
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.custom_sound.set_on"));
                                }
                                return 1;
                            }))
                            .then(ClientCommands.literal("off").executes(context -> {
                                if (!ModConfig.customSoundEnabled) {
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.custom_sound.already_off"));
                                } else {
                                    ModConfig.customSoundEnabled = false;
                                    ModConfig.save();
                                    context.getSource().sendFeedback(Component.translatable("deathsound.command.custom_sound.set_off"));
                                }
                                return 1;
                            }))
                    )

                    .then(ClientCommands.literal("setCustomSound")
                            .then(ClientCommands.argument("soundfile", StringArgumentType.word())
                                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(CustomSoundPack.getFiles(), builder))
                                    .executes(context -> {
                                        String realSoundName = StringArgumentType.getString(context, "soundfile");

                                        ModConfig.customSoundFileName = realSoundName;
                                        ModConfig.save();

                                        CustomSoundScreen.saveSoundsJson(Minecraft.getInstance(), realSoundName);
                                        context.getSource().sendFeedback(Component.translatable("deathsound.command.set_sound.success", (realSoundName + ".ogg")));

                                        Minecraft.getInstance().reloadResourcePacks();

                                        return 1;
                                    })
                            )
                    )

                    .then(ClientCommands.literal("stats")
                            .executes(context -> {
                                FabricClientCommandSource source = context.getSource();
                                source.sendFeedback(Component.translatable("deathsound.stats.header"));
                                source.sendFeedback(Component.translatable("deathsound.stats.total", StatsManager.totalDeathsObserved));
                                source.sendFeedback(Component.translatable("deathsound.stats.total_pops", StatsManager.totalPops));
                                source.sendFeedback(Component.translatable("deathsound.stats.sounds", StatsManager.totalSoundsPlayed));

                                source.sendFeedback(Component.translatable("deathsound.stats.top_victims"));
                                StatsManager.playerDeathMap.entrySet().stream()
                                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5)
                                        .forEach(e -> source.sendFeedback(Component.translatable("deathsound.stats.entry", e.getKey(), e.getValue())));

                                source.sendFeedback(Component.translatable("deathsound.stats.top_poppers"));
                                StatsManager.playerPopsMap.entrySet().stream()
                                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5)
                                        .forEach(e -> source.sendFeedback(Component.translatable("deathsound.stats.entry_pops", e.getKey(), e.getValue())));

                                source.sendFeedback(Component.translatable("deathsound.stats.footer"));
                                return 1;
                            })
                            .then(ClientCommands.literal("player")
                                    .then(ClientCommands.argument("targetName", StringArgumentType.word())
                                            .suggests((context, builder) -> {
                                                Set<String> allPlayers = new HashSet<>(StatsManager.playerDeathMap.keySet());
                                                allPlayers.addAll(StatsManager.playerPopsMap.keySet());
                                                return SharedSuggestionProvider.suggest(allPlayers, builder);
                                            })
                                            .executes(context -> {
                                                String target = StringArgumentType.getString(context, "targetName");
                                                int deaths = StatsManager.playerDeathMap.entrySet().stream()
                                                        .filter(e -> e.getKey().equalsIgnoreCase(target))
                                                        .map(Map.Entry::getValue)
                                                        .findFirst()
                                                        .orElse(0);

                                                int pops = StatsManager.playerPopsMap.entrySet().stream()
                                                        .filter(e -> e.getKey().equalsIgnoreCase(target))
                                                        .map(Map.Entry::getValue)
                                                        .findFirst()
                                                        .orElse(0);

                                                context.getSource().sendFeedback(Component.translatable("deathsound.stats.player_header", target));

                                                context.getSource().sendFeedback(Component.translatable("deathsound.stats.player_deaths",
                                                        Component.literal(String.valueOf(deaths)).withStyle(ChatFormatting.YELLOW)));

                                                context.getSource().sendFeedback(Component.translatable("deathsound.stats.player_pops",
                                                        Component.literal(String.valueOf(pops)).withStyle(ChatFormatting.AQUA)));

                                                return 1;
                                            })
                                    )
                            )
                            .then(ClientCommands.literal("resetPlayers")
                                    .executes(context -> {
                                        int size = StatsManager.playerDeathMap.size() + StatsManager.playerPopsMap.size();
                                        StatsManager.playerDeathMap.clear();
                                        StatsManager.playerPopsMap.clear();
                                        StatsManager.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.stats.deleted_players", size));
                                        return 1;
                                    })
                            )
                            .then(ClientCommands.literal("resetAll")
                                    .executes(context -> {
                                        StatsManager.playerDeathMap.clear();
                                        StatsManager.playerPopsMap.clear();
                                        StatsManager.totalDeathsObserved = 0;
                                        StatsManager.totalPops = 0;
                                        StatsManager.totalSoundsPlayed = 0;
                                        StatsManager.save();
                                        context.getSource().sendFeedback(Component.translatable("deathsound.stats.deleted_all"));
                                        return 1;
                                    })
                            )

                            .then(ClientCommands.literal("resetPlayer")
                                    .then(ClientCommands.argument("targetName", StringArgumentType.word())
                                            .suggests((context, builder) -> {
                                                Set<String> allPlayers = new HashSet<>(StatsManager.playerDeathMap.keySet());
                                                allPlayers.addAll(StatsManager.playerPopsMap.keySet());
                                                return SharedSuggestionProvider.suggest(allPlayers, builder);
                                            })
                                            .then(ClientCommands.literal("deaths")
                                                    .executes(context -> resetPlayerStats(context, "deaths")))
                                            .then(ClientCommands.literal("pops")
                                                    .executes(context -> resetPlayerStats(context, "pops")))
                                            .then(ClientCommands.literal("both")
                                                    .executes(context -> resetPlayerStats(context, "both")))
                                    )
                            )
                    )
                    .then(ClientCommands.literal("reset").executes(context -> {
                        ModConfig.resetToDefaults();
                        ModConfig.save();
                        context.getSource().sendFeedback(Component.translatable("deathsound.command.reset"));
                        return 1;
                    }));

            dispatcher.register(node);
        });
    }

    private void createCustomFolder() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config/deathsound");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    public static void addDeathToHistory(String name, double x, double y, double z, String dimension) {
        if (deathHistory.size() >= 100) {
            deathHistory.remove(0);
        }
        deathHistory.add(new DeathEntry(name, x, y, z, System.currentTimeMillis(), dimension));
    }

    private static int resetPlayerStats(CommandContext<FabricClientCommandSource> context, String mode) {
        String target = StringArgumentType.getString(context, "targetName");

        String actualKey = StatsManager.playerDeathMap.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(target))
                .findFirst()
                .orElse(StatsManager.playerPopsMap.keySet().stream()
                        .filter(k -> k.equalsIgnoreCase(target))
                        .findFirst()
                        .orElse(target));

        boolean resetDeaths = mode.equals("deaths") || mode.equals("both");
        boolean resetPops = mode.equals("pops") || mode.equals("both");

        if (resetDeaths) StatsManager.playerDeathMap.remove(actualKey);
        if (resetPops) StatsManager.playerPopsMap.remove(actualKey);

        StatsManager.save();

        context.getSource().sendFeedback(Component.translatable("deathsound.stats.deleted_players", mode, actualKey));

        return 1;
    }

    public static void onTotemPop(Player player, double distance) {
        if (ModConfig.monitorPops) {
            String name = player.getName().getString();
            int x = (int) player.getX();
            int y = (int) player.getY();
            int z = (int) player.getZ();

            Component formattedName = Component.literal(name).withColor(0xFF5555);
            String coords = String.format("%d, %d, %d", x, y, z);
            String distStr = String.format(java.util.Locale.US, "%.1f", distance) + "m";
            Component formattedDist = Component.literal(distStr).withColor(0x55FF55);

            Minecraft client = Minecraft.getInstance();
            if (client.gui != null && client.gui.hud.getChat() != null) {
                client.gui.hud.getChat().addClientSystemMessage(
                        Component.translatable("deathsound.monitor.pop", formattedName, coords, formattedDist)
                );
            }
        }
    }

    private static int setDeathEffect(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> context, String effectName) {
        ModConfig.selectedEffect = effectName;
        ModConfig.save();

        String formattedName = effectName.replace("_", " ").toUpperCase();

        context.getSource().sendFeedback(net.minecraft.network.chat.Component.translatable(
                "deathsound.command.effect.set.success",
                formattedName
        ));
        return 1;
    }

    private static int setDeathSound(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> context, String soundName) {
        ModConfig.selectedSound = soundName;
        ModConfig.save();

        String formattedName = soundName.replace("_", " ").toUpperCase();

        context.getSource().sendFeedback(net.minecraft.network.chat.Component.translatable(
                "deathsound.command.set_sound.success",
                formattedName
        ));
        return 1;
    }
}
