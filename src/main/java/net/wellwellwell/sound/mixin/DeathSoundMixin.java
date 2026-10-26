package net.wellwellwell.sound.mixin;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ShriekParticleEffect;
import net.wellwellwell.sound.DeathSound;
import net.wellwellwell.sound.ModConfig;
import net.wellwellwell.sound.StatsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.minecraft.sound.SoundEvents.*;
import static net.wellwellwell.sound.DeathSound.CLASSIC_HURT_EVENT;

@Mixin(ClientPlayNetworkHandler.class)
public class DeathSoundMixin {

    @Unique
    private static final Logger log = LoggerFactory.getLogger(DeathSoundMixin.class);
    @Shadow private ClientWorld world;

    @Unique
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Inject(method = "onEntityStatus", at = @At("RETURN"))
    private void onPlayerDeath(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (!ModConfig.enabled || this.world == null) return;

        if (packet.getStatus() == 3) {
            Entity entity = packet.getEntity(this.world);

            if (entity instanceof PlayerEntity player) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null || client.player == null) return;

                String playerName = player.getName().getString();
                String ownName = client.player.getName().getString();
                boolean isOnList = ModConfig.trackedPlayers.contains(playerName);

                double tx = player.getX();
                double ty = player.getY();
                double tz = player.getZ();
                double distMeters =Math.round(Math.sqrt(player.squaredDistanceTo(client.player)));
                long now = System.currentTimeMillis();

                boolean filterPass = switch (ModConfig.filterMode.toLowerCase()) {
                    case "list" -> isOnList;
                    case "others" -> !playerName.equals(ownName);
                    default -> true;
                };

                if (ModConfig.maxDistanceChunks <= 32) {
                    double maxDist = ModConfig.maxDistanceChunks * 16.0;
                    if (distMeters > maxDist) filterPass = false;
                }

                if (ModConfig.spamProtect && (now - ModConfig.lastPlayedTime < ModConfig.cooldownMS)) {
                    filterPass = false;
                }

                StatsManager.addDeath(playerName, filterPass);

                DeathSound.deathHistory.add(0, new DeathSound.DeathEntry(playerName, tx, ty, tz, now));
                if (DeathSound.deathHistory.size() > 8) DeathSound.deathHistory.remove(8);

                if (ModConfig.notifyTrackedDeath && isOnList) {
                    client.player.sendMessage(Text.translatable("deathsound.chat.tracked_death",
                            playerName,(int) tx,(int) ty,(int) tz, formatDistance(distMeters)), false);
                }
                else if (ModConfig.monitorDeaths) {
                    client.player.sendMessage(Text.translatable("deathsound.chat.debug_msg",
                            playerName,(int) tx,(int) ty,(int) tz, formatDistance(distMeters)), false);
                }

                if (!filterPass) return;
                /*
                 After this only play sound & particle logic. If ModConfig.filterMode is list or others and the sound isn't played the particles aren't spawned
                 */

                ModConfig.lastPlayedTime = now;

                final SoundEvent finalSoundToPlay;
                if (ModConfig.customSoundEnabled) {
                    finalSoundToPlay = SoundEvent.of(Identifier.of("deathsound", "custom_ogg"));
                } else {
                    finalSoundToPlay = getSelectedSound();
                }

                scheduler.schedule(() -> {
                    client.execute(() -> {
                        if (client.world != null) {
                            client.world.playSound(
                                    tx, ty, tz,
                                    finalSoundToPlay,
                                    SoundCategory.MASTER,
                                    ModConfig.volume,
                                    ModConfig.pitch,
                                    ModConfig.useDistance
                            );

                            //Particle logic

                            if (ModConfig.deathEffects) {
                                String effect = ModConfig.selectedEffect == null ? "lightning" : ModConfig.selectedEffect.toLowerCase();

                                if (effect.equals("lightning")) {
                                    LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, client.world);
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
                                        case "firework" -> particleToSpawn = ParticleTypes.FIREWORK;
                                        case "shriek"       -> {
                                            particleToSpawn = new ShriekParticleEffect(0);
                                        }
                                        default             -> particleToSpawn = ParticleTypes.EXPLOSION;
                                    }

                                    if (particleToSpawn != null) {
                                        if (effect.equals("sonic_boom") || effect.equals("shriek")) {
                                            client.world.addParticle(particleToSpawn, tx, ty + 1.0, tz, 0, 1, 0);   // sonic boom and shriek needs velocity variables
                                        } else {
                                            Random random = new Random();
                                            int count = effect.equals("smoke") ? 50 : 70;

                                            count = (int) Math.round(count * ModConfig.particleMultiplier);

                                            if (ModConfig.particleMultiplier >= 100) {
                                                System.out.println("[Death Sound] Warning: A particle multiplier >10 might lag the game if your computer ist older or many particles are spawned at the same time.");
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
                            }
                        }
                    });

                }, ModConfig.delayTicks * 50L, TimeUnit.MILLISECONDS);  // Delay
            }
        }
    }

    @Unique
    private String formatDistance(double meters) {
        String key = ModConfig.useFeet ? "deathsound.unit.feet" : "deathsound.unit.meters";
        double value = ModConfig.useFeet ? meters * 3.28084 : meters;
        return Text.translatable(key, value).getString();
    }

    @Unique
    private SoundEvent getSelectedSound() {                          // get the selected sound / custom sound
        String selected = ModConfig.selectedSound;
        return switch (selected) {
            case "custom_ogg"        -> SoundEvent.of(Identifier.of("deathsound", "custom_ogg"));
            case "classic_hurt"      -> CLASSIC_HURT_EVENT;
            case "wither"            -> ENTITY_WITHER_SPAWN;
            case "thunder"           -> ENTITY_LIGHTNING_BOLT_THUNDER;
            case "explosion"         -> ENTITY_GENERIC_EXPLODE.value();
            case "shrieker"          -> BLOCK_SCULK_SHRIEKER_SHRIEK;
            case "warden_heartbeat"  -> ENTITY_WARDEN_HEARTBEAT;
            case "totem"             -> ITEM_TOTEM_USE;
            case "anvil"             -> BLOCK_ANVIL_LAND;
            case "ghast"             -> ENTITY_GHAST_SCREAM;
            case "enderdragon"       -> ENTITY_ENDER_DRAGON_DEATH;
            case "elder_guardian"    -> ENTITY_ELDER_GUARDIAN_CURSE;
            case "amethyst"          -> BLOCK_AMETHYST_BLOCK_BREAK;
            case "villager_no"       -> ENTITY_VILLAGER_NO;
            case "bell"              -> BLOCK_BELL_USE;
            case "creeper"           -> ENTITY_CREEPER_PRIMED;
            case "level_up"          -> ENTITY_PLAYER_LEVELUP;
            case "goat_scream"       -> ENTITY_GOAT_SCREAMING_AMBIENT;
            case "plinth"            -> BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE;
            case "enderman"          -> ENTITY_ENDERMAN_TELEPORT;
            default                  -> ENTITY_WITHER_SPAWN;
        };
    }
}