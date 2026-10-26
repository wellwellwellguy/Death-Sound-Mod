package net.wellwellwell.sound.mixin;

import net.minecraft.network.chat.Component;
import net.wellwellwell.sound.DeathSoundScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.components.Button;
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
        int fixedX = this.width / 2 + 104;
        int fixedY = this.height / 2 - 12;

        this.addRenderableWidget(Button.builder(Component.literal("DS"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new DeathSoundScreen(this));
                    }
                })
                .bounds(fixedX, fixedY, 20, 20)
                .build());
    }
}