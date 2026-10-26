package net.wellwellwell.sound.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.wellwellwell.sound.DeathSound;
import net.wellwellwell.sound.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(ClientPacketListener.class)
public class DeathSoundMixin {

    @Shadow private ClientLevel level;

    @Unique
    private final ScheduledExecutorService deathsound$scheduler = Executors.newSingleThreadScheduledExecutor();

    @Inject(method = "handleEntityEvent", at = @At("RETURN"))
    private void onPlayerDeath(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!ModConfig.enabled || this.level == null) return;

        if (packet.getEventId() == 3) {
            Entity entity = packet.getEntity(this.level);

            if (entity instanceof Player player) {
                Minecraft client = Minecraft.getInstance();
                if (client == null || client.player == null) return;

                String playerName = player.getName().getString();
                String ownName = client.player.getName().getString();
                boolean isOnList = ModConfig.trackedPlayers.contains(playerName);

                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();

                double distMeters = Math.sqrt(player.distanceToSqr(client.player));
                long now = System.currentTimeMillis();

                DeathSound.deathHistory.add(0, new DeathSound.DeathEntry(playerName, x, y, z, now));
                if (DeathSound.deathHistory.size() > 8) DeathSound.deathHistory.remove(8);

                if (ModConfig.notifyTrackedDeath && isOnList) {
                    String msg = String.format("§7Tracked Player §e%s §7died at §f%.0f, %.0f, %.0f §8(§a%s away§8)",
                            playerName, x, y, z, deathsound$formatDistance(distMeters));
                    client.player.sendSystemMessage(Component.literal(msg));
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

                ModConfig.lastPlayedTime = now;

                deathsound$scheduler.schedule(() -> {
                    client.execute(() -> {
                        if (client.level != null) {
                            SoundEvent soundToPlay = deathsound$getSelectedSound();
                            client.level.playLocalSound(x, y, z, soundToPlay, SoundSource.PLAYERS, ModConfig.volume, ModConfig.pitch, false);
                        }
                    });
                }, ModConfig.delayTicks * 50L, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Unique
    private String deathsound$formatDistance(double meters) {
        return ModConfig.useFeet ? String.format("%.1f ft", meters * 3.28084) : String.format("%.1f m", meters);
    }

    @Unique
    private SoundEvent deathsound$getSelectedSound() {
        return switch (ModConfig.selectedSound.toLowerCase()) {
            case "thunder" -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case "explosion" -> SoundEvents.GENERIC_EXPLODE;
            case "shrieker" -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
            default -> SoundEvents.WITHER_SPAWN;
        };
    }
}