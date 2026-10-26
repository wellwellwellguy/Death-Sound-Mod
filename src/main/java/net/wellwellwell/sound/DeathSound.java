package net.wellwellwell.sound;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;

import java.util.logging.Logger;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class DeathSound implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
    }

    @Override
    public void onInitializeClient() {
        System.out.println("Death Sound Mod has been loaded. (100%)");



        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("deathsound")
                    .then(literal("on").executes(c -> {
                        ModConfig.enabled = true;
                        ModConfig.save();
                        sendFeedback("§aEnabled");
                        return 1;
                    }))
                    .then(literal("off").executes(c -> {
                        ModConfig.enabled = false;
                        ModConfig.save();
                        sendFeedback("§cDisabled");
                        return 1;
                    }))
                    .then(literal("test").executes(c -> {
                        playDeathSound();
                        return 1;
                    }))
                    .then(literal("volume")
                            .then(argument("value", FloatArgumentType.floatArg(0, 100))
                                    .executes(c -> {
                                        ModConfig.volume = (FloatArgumentType.getFloat(c, "value") / 100);
                                        ModConfig.save();
                                        sendFeedback("§eVolume: " + (ModConfig.volume * 100) + "%");
                                        return 1;
                                    })))
                    .then(literal("pitch")
                            .then(argument("value", FloatArgumentType.floatArg(0, 2))
                                    .executes(c -> {
                                        ModConfig.pitch = FloatArgumentType.getFloat(c, "value");
                                        ModConfig.save();
                                        sendFeedback("§ePitch: " + ModConfig.pitch);
                                        return 1;
                                    })))
                    .then(literal("reset").executes(c -> {
                        ModConfig.reset();
                        sendFeedback("§cReset to defaults.");
                        return 1;
                    }))
            );
        });
    }

    public static void playDeathSound() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.level != null && client.player != null) {
                client.level.playLocalSound(
                        client.player.getX(), client.player.getY(), client.player.getZ(),
                        SoundEvents.WITHER_SPAWN,
                        SoundSource.MASTER,
                        ModConfig.volume,
                        ModConfig.pitch,
                        false
                );
            }
        });
    }

    private void sendFeedback(String msg) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendMessage(new TextComponent("§6[DeathSound] " + msg), client.player.getUUID());
        }
    }
}
