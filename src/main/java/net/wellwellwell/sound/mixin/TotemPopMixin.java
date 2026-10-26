package net.wellwellwell.sound.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.wellwellwell.sound.ModConfig;
import net.wellwellwell.sound.StatsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class TotemPopMixin {

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void onTotemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        if (packet.getStatus() == 35) {
            Entity entity = packet.getEntity(client.world);

            if (entity instanceof PlayerEntity target) {
                String playerName = target.getName().getString();

                StatsManager.addPop(playerName);

                if (target.equals(client.player)) {
                    return;
                }

                double distance = client.player.distanceTo(target);

                String posStr = String.format("%.0f, %.0f, %.0f", target.getX(), target.getY(), target.getZ());

                String distStr = String.format("%.1f m", distance);

                if (ModConfig.monitorPops) {
                    client.player.sendMessage(Text.translatable("deathsound.monitor.pop",
                            playerName,
                            posStr,
                            distStr
                    ), false);
                }
            }
        }
    }
}