package com.wellwellwell.sound.mixin;

import com.wellwellwell.sound.ModConfig;
import com.wellwellwell.sound.StatsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class TotemPopMixin {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void onTotemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !ModConfig.enabled) return;

        if (packet.getEventId() == 35) {
            Entity entity = packet.getEntity(minecraft.level);


            if (entity instanceof Player target) {

                String playerName = target.getName().getString();
                StatsManager.addPop(playerName);
                StatsManager.save();


                if (ModConfig.monitorPops) {
                    double distance = minecraft.player.distanceTo(target);
                    String posStr = String.format("%.0f, %.0f, %.0f", target.getX(), target.getY(), target.getZ());

                    String distStr = ModConfig.useFeet
                            ? String.format("%.1f ft", distance * 3.28084)
                            : String.format("%.1f m", distance);

                    minecraft.execute(() -> {
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(
                                    Component.translatable("deathsound.monitor.pop",
                                            playerName,
                                            posStr,
                                            distStr
                                    )
                            );
                        }

                        //For some reason the mod made totem pops invisible and nor play any sound. here it makes that manually
                        if (minecraft.level != null) {
                            minecraft.particleEngine.createTrackingEmitter(target, ParticleTypes.TOTEM_OF_UNDYING, 30);

                            minecraft.level.playLocalSound(
                                    target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.TOTEM_USE,
                                    net.minecraft.sounds.SoundSource.PLAYERS,
                                    1.0F, 1.0F, false
                            );
                        }
                    });

                    ci.cancel();
                }
            }
        }
    }
}
