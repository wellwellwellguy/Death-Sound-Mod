package com.wellwellwell.sound.mixin;

import com.wellwellwell.sound.DeathSound;
import com.wellwellwell.sound.ModConfig;
import com.wellwellwell.sound.StatsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.wellwellwell.sound.DeathSound.CLASSIC_HURT;

@Mixin(ClientPacketListener.class)
public class DeathSoundMixin {

    @Unique
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Inject(method = "handleEntityEvent", at = @At("RETURN"))
    private void onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !ModConfig.enabled) return;

        Entity entity = packet.getEntity(client.level);
        if (!(entity instanceof Player player)) return;

        if (packet.getEventId() == 3) {
            handleDeath(player, client);
        }
        else if (packet.getEventId() == 35 && ModConfig.monitorPops) {
            double distance = client.player.distanceTo(player);
            DeathSound.onTotemPop(player, distance);
        }
    }

    @Unique
    private void handleDeath(Player player, Minecraft client) {
        long currentTime = System.currentTimeMillis();

        String name = player.getName().getString();
        boolean isMe = player.equals(client.player);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double dist = client.player.distanceTo(player);
        boolean isOnList = ModConfig.trackedPlayers.contains(name);

        boolean filterPass = switch (ModConfig.filterMode.toLowerCase()) {
            case "list" -> isOnList;
            case "others" -> !isMe;
            default -> true;
        };

        if (ModConfig.maxDistanceChunks <= 32) {
            double maxDist = ModConfig.maxDistanceChunks * 16.0;
            if (dist > maxDist) filterPass = false;
        }

        if (ModConfig.spamProtect && (currentTime - ModConfig.lastPlayedTime < ModConfig.cooldownMS)) {
            filterPass = false;
        }

        StatsManager.addDeath(name, filterPass);
        StatsManager.save();

        DeathSound.addDeathToHistory(name, px, py, pz, client.level.dimension().toString());

        if (ModConfig.notifyTrackedDeath && isOnList) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("deathsound.chat.tracked_death", name, (int) px, (int) py, (int) pz, formatDistance(dist)));
            }
        } else if (ModConfig.monitorDeaths) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("deathsound.chat.debug_msg", name, (int) px, (int) py, (int) pz, formatDistance(dist)));
            }
        }

        if (!filterPass) return;

        ModConfig.lastSoundTimestamp = currentTime;

        final SoundEvent finalSoundToPlay;
        if (ModConfig.customSoundEnabled) {
            var soundId = Identifier.parse("deathsound:custom_ogg");
            finalSoundToPlay = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(soundId);
        } else {
            finalSoundToPlay = getSelectedSound();
        }

        scheduler.schedule(() -> {
            client.execute(() -> {
                if (client.level != null && client.player != null && ModConfig.canPlaySound()) {
                    client.level.playLocalSound(
                            px, py, pz,
                            finalSoundToPlay,
                            SoundSource.MASTER,
                            ModConfig.volume,
                            ModConfig.pitch,
                            ModConfig.useDistance
                    );

                    if (ModConfig.deathEffects) {
                        String effect = ModConfig.selectedEffect == null ? "lightning" : ModConfig.selectedEffect.toLowerCase();

                        if (effect.equals("lightning")) {
                            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                                    .get(net.minecraft.resources.Identifier.parse("lightning_bolt"))
                                    .ifPresent(holder -> {
                                        @SuppressWarnings("unchecked")
                                        EntityType<LightningBolt> lightningType = (EntityType<LightningBolt>) holder.value();

                                        LightningBolt lightning = lightningType.create(client.level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                                        if (lightning != null) {
                                            lightning.setId(-net.minecraft.util.RandomSource.create().nextInt(100000) - 1);
                                            lightning.setPos(px, py, pz);
                                            lightning.setVisualOnly(true);
                                            client.level.addEntity(lightning);
                                        }
                                    });
                        }
                        else {
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
                                Random random = new Random();
                                int count = effect.equals("smoke") ? 50 : 70;
                                count = (int) Math.round(count * ModConfig.particleMultiplier);

                                if (ModConfig.particleMultiplier >= 10.0) {
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
                        }
                    }
                }
            });
        }, (long) ModConfig.delayTicks * 50L, TimeUnit.MILLISECONDS);
    }

    @Unique
    private String formatDistance(double meters) {
        String key = ModConfig.useFeet ? "deathsound.unit.feet" : "deathsound.unit.meters";
        double value = ModConfig.useFeet ? meters * 3.28084 : meters;
        return Component.translatable(key, String.format("%.1f", value)).getString();
    }

    @Unique
    private SoundEvent getSelectedSound() {
        return switch (ModConfig.selectedSound.toLowerCase()) {
            case "custom_ogg"        -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse("deathsound:custom_ogg"));
            case "classic_hurt"      -> CLASSIC_HURT;
            case "wither"            -> SoundEvents.WITHER_SPAWN;
            case "thunder"           -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case "explosion"         -> SoundEvents.GENERIC_EXPLODE.value();
            case "shrieker"          -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
            case "warden_heartbeat"  -> SoundEvents.WARDEN_HEARTBEAT;
            case "totem"             -> SoundEvents.TOTEM_USE;
            case "anvil"             -> SoundEvents.ANVIL_LAND;
            case "ghast"             -> SoundEvents.GHAST_SCREAM;
            case "enderdragon"       -> SoundEvents.ENDER_DRAGON_DEATH;
            case "elder_guardian"    -> SoundEvents.ELDER_GUARDIAN_CURSE;
            case "amethyst"          -> SoundEvents.AMETHYST_BLOCK_BREAK;
            case "villager_no"       -> SoundEvents.VILLAGER_NO;
            case "bell"              -> SoundEvents.BELL_RESONATE;
            case "creeper"           -> SoundEvents.CREEPER_PRIMED;
            case "level_up"          -> SoundEvents.PLAYER_LEVELUP;
            case "goat_scream"       -> SoundEvents.GOAT_SCREAMING_AMBIENT;
            case "plinth"            -> SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE;
            case "enderman"          -> SoundEvents.ENDERMAN_TELEPORT;
            default                  -> SoundEvents.WITHER_SPAWN;
        };
    }
}