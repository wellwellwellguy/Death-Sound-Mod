package net.wellwellwell.sound.mixin;

import net.minecraft.registry.entry.RegistryEntry;
import net.wellwellwell.sound.DeathSound;
import net.wellwellwell.sound.ModConfig;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(ClientPlayNetworkHandler.class)
public class DeathSoundMixin {

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
                double distMeters = Math.sqrt(player.squaredDistanceTo(client.player));
                long now = System.currentTimeMillis();

                DeathSound.deathHistory.add(0, new DeathSound.DeathEntry(playerName, tx, ty, tz, now));
                if (DeathSound.deathHistory.size() > 8) DeathSound.deathHistory.remove(8);

                if (ModConfig.debugMode) {
                    String debugMsg = String.format("§8[§6Debug§8] §c%s §7died at §f%.0f %.0f %.0f §8(§a%s away§8)",
                            playerName, tx, ty, tz, formatDistance(distMeters));
                    client.player.sendMessage(Text.literal(debugMsg), false);
                }

                if (ModConfig.notifyTrackedDeath && isOnList) {
                    String msg = String.format("§6[DeathSound] §7Tracked Player §e%s §7died at §f%.0f, %.0f, %.0f §8(§a%s away§8)",
                            playerName, tx, ty, tz, formatDistance(distMeters));
                    client.player.sendMessage(Text.literal(msg), false);
                }

                boolean shouldPlay = switch (ModConfig.filterMode.toLowerCase()) {
                    case "list" -> isOnList;
                    case "others" -> !playerName.equals(ownName);
                    default -> true;
                };

                if (!shouldPlay) return;

                if (ModConfig.maxDistanceChunks <= 19) {
                    double maxDist = ModConfig.maxDistanceChunks * 16.0;
                    if (distMeters > maxDist) return;
                }

                if (ModConfig.spamProtect && (now - ModConfig.lastPlayedTime < ModConfig.cooldownMS)) {
                    if (ModConfig.debugMode) client.player.sendMessage(Text.literal("§c[Debug] No sound because of cooldown."), false);
                    return;
                }
                ModConfig.lastPlayedTime = now;

                scheduler.schedule(() -> {
                    client.execute(() -> {
                        if (client.world != null) {
                            SoundEvent soundToPlay = ModConfig.customSoundEnabled
                                    ? SoundEvent.of(new Identifier("deathsound", "custom_" + ModConfig.lastSoundTimestamp))
                                    : getSelectedSound();

                            client.world.playSound(tx, ty, tz, soundToPlay,
                                    SoundCategory.MASTER, ModConfig.volume, ModConfig.pitch, false);

                            if (ModConfig.deathEffects) {
                                LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, client.world);
                                lightning.refreshPositionAfterTeleport(tx, ty, tz);
                                lightning.setCosmetic(true);
                                client.world.addEntity(lightning.getId(), lightning);
                            }
                        }
                    });
                }, ModConfig.delayTicks * 50L, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Unique
    private String formatDistance(double meters) {
        return ModConfig.useFeet ? String.format("%.1f ft", meters * 3.28084) : String.format("%.1f m", meters);
    }

    @Unique
    private SoundEvent getSelectedSound() {
        return switch (ModConfig.selectedSound.toLowerCase()) {
            case "thunder" -> SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER;
            case "explosion" -> SoundEvents.ENTITY_GENERIC_EXPLODE;
            case "shrieker" -> SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK;
            default -> SoundEvents.ENTITY_WITHER_SPAWN;
        };
    }
}