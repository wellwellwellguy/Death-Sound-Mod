package net.wellwellwell.sound.mixin;

import net.wellwellwell.sound.DeathSound;
import net.wellwellwell.sound.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public class DeathSoundMixin {

    @Inject(method = "handleEntityEvent", at = @At("RETURN"))
    private void onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!ModConfig.enabled) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) return;

        if (packet.getEventId() == 3) {
            Entity entity = packet.getEntity(mc.level);

            if (entity instanceof Player && entity.getUUID().equals(mc.player.getUUID())) {
                DeathSound.playDeathSound();
            }
        }
    }
}