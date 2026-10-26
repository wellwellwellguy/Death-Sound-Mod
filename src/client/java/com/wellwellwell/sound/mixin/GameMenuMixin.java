package com.wellwellwell.sound.mixin;

import com.wellwellwell.sound.Screen.DeathSoundScreen;
import com.wellwellwell.sound.ModConfig;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class GameMenuMixin extends Screen {
    protected GameMenuMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addDeathSoundButtons(CallbackInfo ci) {
        if (!ModConfig.showMenuButton) return;
        int xPosition = this.width / 2 + 104;
        int yPosition = this.height / 4 + 56 + 24;

        this.addRenderableWidget(Button.builder(
                        Component.literal("DS"),
                        button -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreenAndShow(new DeathSoundScreen(this));
                            }
                        })
                .bounds(xPosition, yPosition, 20, 20)
                .tooltip(Tooltip.create(Component.literal("Deathsound Settings")))
                .build());
    }
}