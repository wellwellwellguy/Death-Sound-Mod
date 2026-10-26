package net.wellwellwell.sound;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ShriekParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.File;
import java.util.*;

public class DeathSound implements ClientModInitializer {


    public static final Identifier CLASSIC_HURT_ID = Identifier.of("deathsound", "classic_hurt");
    public static final SoundEvent CLASSIC_HURT_EVENT = SoundEvent.of(CLASSIC_HURT_ID);
    private static final String PACK_NAME = "DeathSound-Resources";
    public static final List<DeathEntry> deathHistory = new ArrayList<>();
    public record DeathEntry(String playerName, double x, double y, double z, long timestamp) {}

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("DeathSound");


    @Override
    public void onInitializeClient() {
        Registry.register(Registries.SOUND_EVENT, CLASSIC_HURT_ID, CLASSIC_HURT_EVENT);
        ModConfig.load();
        StatsManager.load();
        CustomSoundPack.syncSounds();
        CustomSoundPack.init();

        File mcDir = MinecraftClient.getInstance().runDirectory;
        File resourcePacksDir = new File(mcDir, "resourcepacks");
        File baseFolder = new File(resourcePacksDir, PACK_NAME);
        File soundsFolder = new File(baseFolder, "assets/deathsound/sounds");

        if (!soundsFolder.exists()) soundsFolder.mkdirs();

        LOGGER.info("DeathSound Mod has been successfully loaded");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var node = dispatcher.register(ClientCommandManager.literal("deathsound")
                    .then(ClientCommandManager.literal("on").executes(context -> {
                        if (ModConfig.enabled) {
                            context.getSource().sendFeedback(Text.translatable("deathsound.command.main.already_on"));
                            return 1;
                        }
                        ModConfig.enabled = true;
                        ModConfig.save();
                        context.getSource().sendFeedback(Text.translatable("deathsound.command.main.set_on"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("off").executes(context -> {
                        if (!ModConfig.enabled) {
                            context.getSource().sendFeedback(Text.translatable("deathsound.command.main.already_off"));
                            return 1;
                        }
                        ModConfig.enabled = false;
                        ModConfig.save();
                        context.getSource().sendFeedback(Text.translatable("deathsound.command.main.set_off"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("distance")
                            .then(ClientCommandManager.argument("chunks", IntegerArgumentType.integer(1, 32))
                                    .executes(context -> {
                                        int dist = IntegerArgumentType.getInteger(context, "chunks");
                                        if (ModConfig.maxDistanceChunks == dist) {
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.distance.already_set", dist));
                                            return 1;
                                        }
                                        ModConfig.maxDistanceChunks = dist;
                                        ModConfig.save();

                                        if (dist == 32) {
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.distance.unlimited"));
                                        } else {
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.distance.set", dist));
                                        }
                                        return 1;
                                    }))
                    )
                    .then(ClientCommandManager.literal("selectedEffect")
                            .then(ClientCommandManager.literal("lightning").executes(context -> {
                                ModConfig.selectedEffect = "lightning";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.effect.set.success", "lightning"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("explosion").executes(context -> {
                                ModConfig.selectedEffect = "explosion";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.effect.set.success", "explosion"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("bad_omen").executes(context -> {
                                ModConfig.selectedEffect = "bad_omen";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.effect.set.success", "bad_omen"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("smoke").executes(context -> {
                                ModConfig.selectedEffect = "smoke";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.effect.set.success", "smoke"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("shriek").executes(context -> {
                                ModConfig.selectedEffect = "shriek";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.effect.set.success", "shriek"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("sonic_boom").executes(context -> {
                                ModConfig.selectedEffect = "sonic_boom";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.effect.set.success", "sonic_boom"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("totem").executes(context -> {
                                ModConfig.selectedEffect = "totem";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.effect.set.success", "totem"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("firework").executes(context -> {
                                ModConfig.selectedEffect = "firework";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.effect.set.success", "firework"));
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("sound")
                            .then(ClientCommandManager.literal("classic_hurt").executes(context -> {
                                ModConfig.selectedSound = "classic_hurt";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Classic Hurt"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("wither").executes(context -> {
                                ModConfig.selectedSound = "wither";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Wither"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("thunder").executes(context -> {
                                ModConfig.selectedSound = "thunder";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Thunder"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("explosion").executes(context -> {
                                ModConfig.selectedSound = "explosion";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Explosion"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("shrieker").executes(context -> {
                                ModConfig.selectedSound = "shrieker";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Shrieker"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("warden_heartbeat").executes(context -> {
                                ModConfig.selectedSound = "warden_heartbeat";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Warden Heartbeat"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("totem").executes(context -> {
                                ModConfig.selectedSound = "totem";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Totem"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("anvil").executes(context -> {
                                ModConfig.selectedSound = "anvil";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Anvil"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("ghast").executes(context -> {
                                ModConfig.selectedSound = "ghast";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Ghast"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("enderdragon").executes(context -> {
                                ModConfig.selectedSound = "enderdragon";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Enderdragon"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("elder_guardian").executes(context -> {
                                ModConfig.selectedSound = "elder_guardian";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Elder Guardian"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("amethyst").executes(context -> {
                                ModConfig.selectedSound = "amethyst";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Amethyst"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("villager_no").executes(context -> {
                                ModConfig.selectedSound = "villager_no";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Villager No"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("bell").executes(context -> {
                                ModConfig.selectedSound = "bell";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Bell"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("creeper").executes(context -> {
                                ModConfig.selectedSound = "creeper";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Creeper"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("level_up").executes(context -> {
                                ModConfig.selectedSound = "level_up";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Level Up"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("goat_scream").executes(context -> {
                                ModConfig.selectedSound = "goat_scream";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Goat Scream"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("plinth").executes(context -> {
                                ModConfig.selectedSound = "plinth";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Plinth"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("enderman").executes(context -> {
                                ModConfig.selectedSound = "enderman";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", "Enderman"));
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("list")
                            .executes(context -> {
                                if (ModConfig.trackedPlayers.isEmpty()) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.history.empty"));
                                } else {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.screen.manage_list.header"));
                                    for (String name : ModConfig.trackedPlayers) {
                                        context.getSource().sendFeedback(Text.literal("§8- §e" + name));
                                    }
                                }
                                return 1;
                            })
                            .then(ClientCommandManager.literal("add")
                                    .then(ClientCommandManager.argument("player", StringArgumentType.string()).executes(context -> {
                                        String playerName = StringArgumentType.getString(context, "player");
                                        if (ModConfig.trackedPlayers.add(playerName)) {
                                            ModConfig.save();
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.player_added", playerName));
                                        }
                                        return 1;
                                    }))
                            )
                            .then(ClientCommandManager.literal("remove")
                                    .then(ClientCommandManager.argument("player", StringArgumentType.string()).executes(context -> {
                                        String playerName = StringArgumentType.getString(context, "player");
                                        if (ModConfig.trackedPlayers.remove(playerName)) {
                                            ModConfig.save();
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.player_removed", playerName));
                                        }
                                        return 1;
                                    }))
                            )
                            .then(ClientCommandManager.literal("clear").executes(context -> {
                                ModConfig.trackedPlayers.clear();
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.screen.manage_list.clear"));
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("volume")
                            .then(ClientCommandManager.argument("value", IntegerArgumentType.integer(0, 100))
                                    .executes(context -> {
                                        int vol = IntegerArgumentType.getInteger(context, "value");
                                        float volFloat = vol / 100.0f;
                                        if (ModConfig.volume == volFloat) {
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.volume.already_set", vol));
                                            return 1;
                                        }
                                        ModConfig.volume = volFloat;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.translatable("deathsound.command.volume.set", vol));
                                        return 1;
                                    })
                            )
                    )

                    .then(ClientCommandManager.literal("stats")
                            .executes(context -> {
                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.header"));
                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.total", "§f" + StatsManager.totalDeathsObserved));
                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.total_pops", "§f" + StatsManager.totalPops));
                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.sounds", "§a" + StatsManager.totalSoundsPlayed));

                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.top_victims"));
                                StatsManager.playerDeathMap.entrySet().stream()
                                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                        .limit(5)
                                        .forEach(entry -> context.getSource().sendFeedback(Text.translatable("deathsound.stats.entry", entry.getKey(), "§e" + entry.getValue())));

                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.top_poppers"));
                                StatsManager.playerPopsMap.entrySet().stream()
                                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                        .limit(5)
                                        .forEach(entry -> context.getSource().sendFeedback(Text.translatable("deathsound.stats.entry_pops", entry.getKey(), "§b" + entry.getValue())));

                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.footer"));
                                return 1;
                            })

                            .then(ClientCommandManager.literal("player")
                                    .then(ClientCommandManager.argument("targetName", StringArgumentType.word())
                                            .suggests((context, builder) -> {
                                                Set<String> allPlayers = new HashSet<>(StatsManager.playerDeathMap.keySet());
                                                allPlayers.addAll(StatsManager.playerPopsMap.keySet());
                                                return CommandSource.suggestMatching(allPlayers, builder);
                                            })
                                            .executes(context -> {
                                                String target = StringArgumentType.getString(context, "targetName");
                                                int deaths = StatsManager.playerDeathMap.getOrDefault(target, 0);
                                                int pops = StatsManager.playerPopsMap.getOrDefault(target, 0);

                                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.player_header", target));
                                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.player_deaths", "§e" + deaths));
                                                context.getSource().sendFeedback(Text.translatable("deathsound.stats.player_pops", "§b" + pops));
                                                return 1;
                                            })
                                    )
                            )

                            .then(ClientCommandManager.literal("resetPlayers")
                                    .executes(context -> {
                                        int deathCount = StatsManager.playerDeathMap.size();
                                        int popCount = StatsManager.playerPopsMap.size();
                                        StatsManager.playerDeathMap.clear();
                                        StatsManager.playerPopsMap.clear();
                                        StatsManager.save();
                                        context.getSource().sendFeedback(Text.translatable("deathsound.stats.deleted_players", "§e" + (deathCount + popCount)));
                                        return 1;
                                    })
                            )

                            .then(ClientCommandManager.literal("resetAll")
                                    .executes(context -> {
                                        StatsManager.playerDeathMap.clear();
                                        StatsManager.playerPopsMap.clear();
                                        StatsManager.totalDeathsObserved = 0;
                                        StatsManager.totalSoundsPlayed = 0;
                                        StatsManager.totalPops = 0;
                                        StatsManager.save();
                                        context.getSource().sendFeedback(Text.translatable("deathsound.stats.deleted_all"));
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("resetPlayer")
                                    .then(ClientCommandManager.argument("targetName", StringArgumentType.word())
                                            .suggests((context, builder) -> {
                                                Set<String> allPlayers = new HashSet<>(StatsManager.playerDeathMap.keySet());
                                                allPlayers.addAll(StatsManager.playerPopsMap.keySet());
                                                return CommandSource.suggestMatching(allPlayers, builder);
                                            })
                                            .then(ClientCommandManager.literal("deaths")
                                                    .executes(context -> resetPlayerStat(context, "deaths")))
                                            .then(ClientCommandManager.literal("pops")
                                                    .executes(context -> resetPlayerStat(context, "pops")))
                                            .then(ClientCommandManager.literal("both")
                                                    .executes(context -> resetPlayerStat(context, "both")))
                                    )
                            )
                    )
                    .then(ClientCommandManager.literal("pitch")
                            .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.0f, 2.0f))
                                    .executes(context -> {
                                        float pitch = FloatArgumentType.getFloat(context, "value");
                                        if (ModConfig.pitch == pitch) {
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.pitch.already_set", pitch));
                                            return 1;
                                        }
                                        ModConfig.pitch = pitch;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.translatable("deathsound.command.pitch.set", pitch));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("particleMultiplier")
                            .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0.01, 30000000))
                                    .executes(context -> {
                                        double particleMultiplier = DoubleArgumentType.getDouble(context, "value");
                                        ModConfig.particleMultiplier = particleMultiplier;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.translatable("deathsound.command.particle_multiplier.set", particleMultiplier));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("stop").executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client != null && client.getSoundManager() != null) {

                            client.getSoundManager().stopSounds(Identifier.of("deathsound", "custom_ogg"), null);

                            String selected = ModConfig.selectedSound.toLowerCase().trim();
                            Identifier selectedId = null;

                            switch (selected) {
                                case "classic_hurt"      -> selectedId = Identifier.of("deathsound", "classic_hurt");
                                case "wither"            -> selectedId = Identifier.of("minecraft", "entity.wither.spawn");
                                case "thunder"           -> selectedId = Identifier.of("minecraft", "entity.lightning_bolt.thunder");
                                case "explosion"         -> selectedId = Identifier.of("minecraft", "entity.generic.explode");
                                case "shrieker"          -> selectedId = Identifier.of("minecraft", "block.sculk_shrieker.shriek");
                                case "warden_heartbeat"  -> selectedId = Identifier.of("minecraft", "entity.warden.heartbeat");
                                case "totem"             -> selectedId = Identifier.of("minecraft", "item.totem.use");
                                case "anvil"             -> selectedId = Identifier.of("minecraft", "block.anvil.land");
                                case "ghast"             -> selectedId = Identifier.of("minecraft", "entity.ghast.scream");
                                case "enderdragon"       -> selectedId = Identifier.of("minecraft", "entity.ender_dragon.death");
                                case "elder_guardian"    -> selectedId = Identifier.of("minecraft", "entity.elder_guardian.curse");
                                case "amethyst"          -> selectedId = Identifier.of("minecraft", "block.amethyst_block.break");
                                case "villager_no"       -> selectedId = Identifier.of("minecraft", "entity.villager.no");
                                case "bell"              -> selectedId = Identifier.of("minecraft", "block.bell.use");
                                case "creeper"           -> selectedId = Identifier.of("minecraft", "entity.creeper.primed");
                                case "level_up"          -> selectedId = Identifier.of("minecraft", "entity.player.levelup");
                                case "goat_scream"       -> selectedId = Identifier.of("minecraft", "entity.goat.screaming.ambient");
                                case "plinth"            -> selectedId = Identifier.of("minecraft", "block.trial_spawner.ominous_activate");
                                case "enderman"          -> selectedId = Identifier.of("minecraft", "entity.enderman.teleport");
                            }

                            if (selectedId != null) {
                                client.getSoundManager().stopSounds(selectedId, null);
                            }

                            Identifier[] panicStopList = {
                                    Identifier.of("minecraft", "entity.wither.spawn"),
                                    Identifier.of("minecraft", "entity.lightning_bolt.thunder"),
                                    Identifier.of("minecraft", "entity.ender_dragon.death"),
                                    Identifier.of("minecraft", "block.sculk_shrieker.shriek")
                            };

                            for (Identifier id : panicStopList) {
                                client.getSoundManager().stopSounds(id, null);
                            }

                            context.getSource().sendFeedback(Text.translatable("deathsound.command.stop.success"));
                        }
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("history").executes(context -> {
                        if (DeathSound.deathHistory.isEmpty()) {
                            context.getSource().sendFeedback(Text.translatable("deathsound.command.history.empty"));
                            return 1;
                        }

                        context.getSource().sendFeedback(Text.translatable("deathsound.command.history.header"));

                        for (DeathEntry entry : DeathSound.deathHistory) {
                            long totalSeconds = (System.currentTimeMillis() - entry.timestamp()) / 1000;

                            Text timeText;
                            if (totalSeconds < 60) {
                                timeText = Text.translatable("deathsound.command.history.time.seconds", totalSeconds);
                            } else {
                                timeText = Text.translatable("deathsound.command.history.time.minutes", (totalSeconds / 60), (totalSeconds % 60));
                            }

                            context.getSource().sendFeedback(Text.translatable("deathsound.command.history.entry",
                                    Text.literal(entry.playerName()).formatted(Formatting.RED),
                                    timeText.copy().formatted(Formatting.WHITE),
                                    Text.literal(String.valueOf((int) entry.x())).formatted(Formatting.AQUA),
                                    Text.literal(String.valueOf((int) entry.y())).formatted(Formatting.AQUA),
                                    Text.literal(String.valueOf((int) entry.z())).formatted(Formatting.AQUA)
                            ));
                        }

                        context.getSource().sendFeedback(Text.translatable("deathsound.command.history.footer"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("locate")
                            .then(ClientCommandManager.argument("player", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        for (DeathEntry entry : DeathSound.deathHistory) {
                                            builder.suggest(entry.playerName());
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String target = StringArgumentType.getString(context, "player");
                                        DeathEntry found = null;
                                        for (int i = DeathSound.deathHistory.size() - 1; i >= 0; i--) {
                                            DeathEntry entry = DeathSound.deathHistory.get(i);
                                            if (entry.playerName().equalsIgnoreCase(target)) {
                                                found = entry;
                                                break;
                                            }
                                        }

                                        if (found != null) {

                                            int ix = (int) Math.round(found.x());
                                            int iy = (int) Math.round(found.y());
                                            int iz = (int) Math.round(found.z());

                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.locate.success",
                                                    found.playerName(), ix, iy, iz));
                                        } else {
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.locate.not_found"));
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("testEffect")
                            .executes(context -> spawnTestEffect(context, null))
                            .then(ClientCommandManager.argument("effectName", StringArgumentType.string())
                                    .executes(context -> spawnTestEffect(context, StringArgumentType.getString(context, "effectName")))
                            )
                    )
                    .then(ClientCommandManager.literal("customSounds")
                            .then(ClientCommandManager.literal("on").executes(context -> {
                                if (ModConfig.customSoundEnabled) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.custom_sound.already_on"));
                                    return 1;
                                }
                                ModConfig.customSoundEnabled = true;
                                CustomSoundPack.syncSounds();
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.custom_sound.set_on"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("off").executes(context -> {
                                if (!ModConfig.customSoundEnabled) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.custom_sound.already_off"));
                                    return 1;
                                }
                                ModConfig.customSoundEnabled = false;
                                CustomSoundPack.syncSounds();
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.custom_sound.set_off"));
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("deathEffects")
                            .then(ClientCommandManager.literal("on").executes(context -> {
                                ModConfig.deathEffects = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.chat.effects_on"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("off").executes(context -> {
                                ModConfig.deathEffects = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.chat.effects_off"));
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("useDistance")
                            .then(ClientCommandManager.literal("true").executes(context -> {
                                ModConfig.useDistance = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.chat.use_distance.on"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("false").executes(context -> {
                                ModConfig.useDistance = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.chat.use_distance.off"));
                                return 1;
                            }))
                    )

                    .then(ClientCommandManager.literal("showMenuButton")
                            .then(ClientCommandManager.literal("true").executes(context -> {
                                ModConfig.showMenuButton = true;
                                ModConfig.save();
                                Text feedback = Text.translatable("deathsound.screen.menu_button.title",
                                        Text.translatable("deathsound.screen.menu_button.shown").formatted(Formatting.GREEN));

                                context.getSource().sendFeedback(feedback);
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("false").executes(context -> {
                                ModConfig.showMenuButton = false;
                                ModConfig.save();
                                Text feedback = Text.translatable("deathsound.screen.menu_button.title",
                                        Text.translatable("deathsound.screen.menu_button.hidden").formatted(Formatting.RED));

                                context.getSource().sendFeedback(feedback);
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("test").executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null && client.world != null) {

                            String selected = ModConfig.customSoundEnabled ? "custom_ogg" : ModConfig.selectedSound.toLowerCase().trim();
                            SoundEvent event;

                            switch (selected) {
                                case "custom_ogg"        -> event = SoundEvent.of(Identifier.of("deathsound", "custom_ogg"));
                                case "classic_hurt"      -> event = DeathSound.CLASSIC_HURT_EVENT;
                                case "wither"            -> event = SoundEvents.ENTITY_WITHER_SPAWN;
                                case "thunder"           -> event = SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER;
                                case "explosion"         -> event = SoundEvents.ENTITY_GENERIC_EXPLODE.value();
                                case "shrieker"          -> event = SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK;
                                case "warden_heartbeat"  -> event = SoundEvents.ENTITY_WARDEN_HEARTBEAT;
                                case "totem"             -> event = SoundEvents.ITEM_TOTEM_USE;
                                case "anvil"             -> event = SoundEvents.BLOCK_ANVIL_LAND;
                                case "ghast"             -> event = SoundEvents.ENTITY_GHAST_SCREAM;
                                case "enderdragon"       -> event = SoundEvents.ENTITY_ENDER_DRAGON_DEATH;
                                case "elder_guardian"    -> event = SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE;
                                case "amethyst"          -> event = SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK;
                                case "villager_no"       -> event = SoundEvents.ENTITY_VILLAGER_NO;
                                case "bell"              -> event = SoundEvents.BLOCK_BELL_USE;
                                case "creeper"           -> event = SoundEvents.ENTITY_CREEPER_PRIMED;
                                case "level_up"          -> event = SoundEvents.ENTITY_PLAYER_LEVELUP;
                                case "goat_scream"       -> event = SoundEvents.ENTITY_GOAT_SCREAMING_AMBIENT;
                                case "plinth"            -> event = SoundEvents.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE;
                                case "enderman"          -> event = SoundEvents.ENTITY_ENDERMAN_TELEPORT;
                                default                  -> event = SoundEvents.ENTITY_WITHER_SPAWN;
                            }

                            double posX = client.player.getX();
                            double posY = ModConfig.useFeet ? client.player.getY() : client.player.getEyeY();
                            double posZ = client.player.getZ();

                            client.world.playSound(
                                    posX, posY, posZ,
                                    event,
                                    SoundCategory.MASTER,
                                    ModConfig.volume,
                                    ModConfig.pitch,
                                    ModConfig.useDistance
                            );

                            if (ModConfig.customSoundEnabled) {
                                String displayName = ModConfig.customSoundFileName.replace(".ogg", "");
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.test.custom", displayName));
                            } else {
                                String displayName = selected.toUpperCase();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.test.default", displayName));
                            }
                        }
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("status").executes(context -> {
                        long currentTime = System.currentTimeMillis();

                        Text lastPlayed = (ModConfig.lastPlayedTime == 0)
                                ? Text.translatable("deathsound.status.never")
                                : Text.translatable("deathsound.status.time_ago", (currentTime - ModConfig.lastPlayedTime));

                        Text unitName = ModConfig.useFeet
                                ? Text.translatable("deathsound.status.unit.feet")
                                : Text.translatable("deathsound.status.unit.meters");

                        String soundDisplay = ModConfig.customSoundEnabled
                                ? "§b" + ModConfig.customSoundFileName + ".ogg"
                                : "§e" + ModConfig.selectedSound.toUpperCase();

                        context.getSource().sendFeedback(Text.translatable("deathsound.status.header"));

                        context.getSource().sendFeedback(Text.translatable("deathsound.status.row1",
                                (ModConfig.enabled ? "§aEnabled" : "§cDisabled"),
                                (ModConfig.monitorDeaths ? "§aEnabled" : "§cDisabled")));

                        context.getSource().sendFeedback(Text.translatable("deathsound.status.row2",
                                soundDisplay,
                                (int) (ModConfig.volume * 100),
                                String.format("%.1f", ModConfig.pitch)));

                        Text filterText = Text.translatable("deathsound.filter.mode." + ModConfig.filterMode.toLowerCase());
                        context.getSource().sendFeedback(Text.translatable("deathsound.status.row3",
                                filterText,
                                ModConfig.maxDistanceChunks));

                        context.getSource().sendFeedback(Text.translatable("deathsound.status.row5",
                                (ModConfig.notifyTrackedDeath ? "§aEnabled" : "§cDisabled")));

                        context.getSource().sendFeedback(Text.translatable("deathsound.status.row6",
                                (ModConfig.deathEffects ? "§aEnabled" : "§cDisabled"),
                                unitName));

                        context.getSource().sendFeedback(Text.translatable("deathsound.status.last_sound").append(lastPlayed));
                        context.getSource().sendFeedback(Text.translatable("deathsound.status.list_size", ModConfig.trackedPlayers.size()));
                        context.getSource().sendFeedback(Text.translatable("deathsound.status.footer"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("unit")
                            .then(ClientCommandManager.literal("m").executes(context -> {
                                if (!ModConfig.useFeet) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.unit.already_meters"));
                                    return 1;
                                }
                                ModConfig.useFeet = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.unit.set_meters"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("ft").executes(context -> {
                                if (ModConfig.useFeet) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.unit.already_feet"));
                                    return 1;
                                }
                                ModConfig.useFeet = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.unit.set_feet"));
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("monitor")
                            .then(ClientCommandManager.literal("deaths")
                                    .then(ClientCommandManager.literal("on").executes(context -> {
                                        ModConfig.monitorDeaths = true;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.translatable("deathsound.command.monitor.on"));
                                        return 1;
                                    }))
                                    .then(ClientCommandManager.literal("off").executes(context -> {
                                        ModConfig.monitorDeaths = false;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.translatable("deathsound.command.monitor.off"));
                                        return 1;
                                    }))
                            )
                            .then(ClientCommandManager.literal("pops")
                                    .then(ClientCommandManager.literal("on").executes(context -> {
                                        if (ModConfig.monitorPops) {
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.pops.already.on"));
                                            ModConfig.monitorPops = true;
                                            ModConfig.save();
                                        } else {
                                            ModConfig.monitorPops = true;
                                            ModConfig.save();
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.monitor.pops.on"));
                                        }
                                        return 1;
                                    }))
                                    .then(ClientCommandManager.literal("off").executes(context -> {
                                        if (!ModConfig.monitorPops) {
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.pops.already.off"));
                                            ModConfig.monitorPops = false;
                                            ModConfig.save();
                                        } else {
                                            ModConfig.monitorPops = false;
                                            ModConfig.save();
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.monitor.pops.off"));
                                        }
                                        return 1;
                                    }))
                            )
                    )
                    .then(ClientCommandManager.literal("setCustomSound")
                            .then(ClientCommandManager.argument("file", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        CustomSoundPack.syncSounds();
                                        File folder = new File(MinecraftClient.getInstance().runDirectory, "resourcepacks/" + PACK_NAME + "/assets/deathsound/sounds");
                                        if (folder.exists()) {
                                            File[] files = folder.listFiles((dir, name) -> name.endsWith(".ogg"));
                                            if (files != null) {
                                                for (File f : files) {
                                                    builder.suggest(f.getName());
                                                }
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String name = StringArgumentType.getString(context, "file");

                                        ModConfig.customSoundFileName = name;
                                        ModConfig.save();

                                        MinecraftClient client = MinecraftClient.getInstance();
                                        CustomSoundScreen.saveSoundsJson(client, name);
                                        String nameWithoutOgg = name.replace(".ogg", "");

                                        CustomSoundScreen.saveSoundsJson(client, nameWithoutOgg);

                                        client.getResourcePackManager().scanPacks();
                                        List<String> enabledPacks = new ArrayList<>(client.getResourcePackManager().getEnabledIds());
                                        String fullPackName = "file/" + PACK_NAME;

                                        if (!enabledPacks.contains(fullPackName)) {
                                            enabledPacks.add(fullPackName);
                                            client.getResourcePackManager().setEnabledProfiles(enabledPacks);
                                        } else {
                                            client.reloadResources();
                                        }
                                        CustomSoundPack.syncSounds();

                                        context.getSource().sendFeedback(Text.translatable("deathsound.command.set_sound.success", name));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("spamprotect")
                            .then(ClientCommandManager.literal("on").executes(context -> {
                                if (ModConfig.spamProtect) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.spamprotect.already_on"));
                                    return 1;
                                }
                                ModConfig.spamProtect = true;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.spamprotect.set_on"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("off").executes(context -> {
                                if (!ModConfig.spamProtect) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.spamprotect.already_off"));
                                    return 1;
                                }
                                ModConfig.spamProtect = false;
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.spamprotect.set_off"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("cooldown")
                                    .then(ClientCommandManager.argument("ms", IntegerArgumentType.integer(0, 60000))
                                            .executes(context -> {
                                                int ms = IntegerArgumentType.getInteger(context, "ms");
                                                ModConfig.cooldownMS = ms;
                                                ModConfig.save();
                                                context.getSource().sendFeedback(Text.translatable("deathsound.command.cooldown.set", ms, ms / 1000));
                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(ClientCommandManager.literal("filter")
                            .then(ClientCommandManager.literal("all").executes(context -> {
                                if (ModConfig.filterMode.equals("everyone")) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.filter.already_all"));
                                    return 1;
                                }
                                ModConfig.filterMode = "everyone";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.filter.set_all"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("others").executes(context -> {
                                if (ModConfig.filterMode.equals("others")) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.filter.already_others"));
                                    return 1;
                                }
                                ModConfig.filterMode = "others";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.filter.set_others"));
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("list").executes(context -> {
                                if (ModConfig.filterMode.equals("list")) {
                                    context.getSource().sendFeedback(Text.translatable("deathsound.command.filter.already_list"));
                                    return 1;
                                }
                                ModConfig.filterMode = "list";
                                ModConfig.save();
                                context.getSource().sendFeedback(Text.translatable("deathsound.command.filter.set_list"));
                                return 1;
                            }))
                    )
                    .then(ClientCommandManager.literal("delay")
                            .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(0, 100))
                                    .executes(context -> {
                                        int ticks = IntegerArgumentType.getInteger(context, "ticks");
                                        if (ModConfig.delayTicks == ticks) {
                                            context.getSource().sendFeedback(Text.translatable("deathsound.command.delay.already_set", ticks));
                                            return 1;
                                        }
                                        ModConfig.delayTicks = ticks;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Text.translatable("deathsound.command.delay.set", ticks));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("reset").executes(context -> {
                        ModConfig.resetToDefaults();
                        ModConfig.save();
                        context.getSource().sendFeedback(Text.translatable("deathsound.command.reset"));
                        return 1;
                    }))
            );
        });
    }

    private static int resetPlayerStat(CommandContext<FabricClientCommandSource> context, String mode) {
        String target = StringArgumentType.getString(context, "targetName");
        boolean removed = false;

        if (mode.equals("deaths") || mode.equals("both")) {
            if (StatsManager.playerDeathMap.remove(target) != null) removed = true;
        }
        if (mode.equals("pops") || mode.equals("both")) {
            if (StatsManager.playerPopsMap.remove(target) != null) removed = true;
        }

        if (removed) {
            StatsManager.save();
            context.getSource().sendFeedback(Text.translatable("deathsound.stats.player_reset_success", target, mode));
        } else {
            context.getSource().sendFeedback(Text.translatable("deathsound.stats.player_not_found", target));
        }
        return 1;
    }

    private static int spawnTestEffect(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> context, String effectName) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) return 0;

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        float yaw = client.player.getYaw();
        float pitch = client.player.getPitch();

        float radiansYaw = (float) Math.toRadians(yaw);
        float radiansPitch = (float) Math.toRadians(pitch);

        double dirX = -Math.sin(radiansYaw) * Math.cos(radiansPitch);
        double dirY = -Math.sin(radiansPitch);
        double dirZ = Math.cos(radiansYaw) * Math.cos(radiansPitch);

        double distance = 3.0;

        double tx = px + (dirX * distance);
        double ty = py + (dirY * distance);
        double tz = pz + (dirZ * distance);

        if (ty < client.player.getY()) {
            ty = client.player.getY();
        }

        String effect = (effectName == null) ?
                (net.wellwellwell.sound.ModConfig.selectedEffect == null ? "lightning" : net.wellwellwell.sound.ModConfig.selectedEffect.toLowerCase())
                : effectName.toLowerCase();

        if (effect.equals("lightning")) {
            net.minecraft.entity.LightningEntity lightning = new net.minecraft.entity.LightningEntity(net.minecraft.entity.EntityType.LIGHTNING_BOLT, client.world);
            lightning.refreshPositionAfterTeleport(tx, ty, tz);
            lightning.setCosmetic(true);
            client.world.addEntity(lightning);
        } else {
            ParticleEffect particleToSpawn = null;

            switch (effect) {
                case "explosion"    -> particleToSpawn = ParticleTypes.EXPLOSION;
                case "bad_omen"     -> particleToSpawn = ParticleTypes.TRIAL_OMEN;
                case "smoke"        -> particleToSpawn = ParticleTypes.LARGE_SMOKE;
                case "sonic_boom"   -> particleToSpawn = ParticleTypes.SONIC_BOOM;
                case "totem"        -> particleToSpawn = ParticleTypes.TOTEM_OF_UNDYING;
                case "firework"     -> particleToSpawn = ParticleTypes.FIREWORK;
                case "shriek"       -> particleToSpawn = new ShriekParticleEffect(0);
                default             -> particleToSpawn = ParticleTypes.EXPLOSION;
            }

            if (particleToSpawn != null) {
                if (effect.equals("sonic_boom") || effect.equals("shriek")) {
                    client.world.addParticle(particleToSpawn, tx, ty + 1.0, tz, 0, 1, 0);
                } else {
                    java.util.Random random = new java.util.Random();
                    int count = effect.equals("smoke") ? 50 : 70;

                    count = (int) Math.round(count * ModConfig.particleMultiplier);

                    if (ModConfig.particleMultiplier >= 1000) {
                        System.out.println("[Death Sound Mod] Warning: A particle multiplier that is too high might lag the game if your computer is older or as many particles are spawned at the same time.");
                    }

                    for (int i = 0; i < count; i++) {
                        double offsetX = (random.nextDouble() - 0.5) * 1.5;
                        double offsetY = random.nextDouble() * 2.0;
                        double offsetZ = (random.nextDouble() - 0.5) * 1.5;

                        double speedX = (random.nextDouble() - 0.5) * 0.2;
                        double speedY = (random.nextDouble() - 0.5) * 0.2;
                        double speedZ = (random.nextDouble() - 0.5) * 0.2;

                        client.world.addParticle(particleToSpawn, tx + offsetX, ty + offsetY, tz + offsetZ, speedX, speedY, speedZ);
                    }
                }
            }
        }

        context.getSource().sendFeedback(net.minecraft.text.Text.translatable("deathsound.command.test_effect.success"));
        return 1;
    }
}
