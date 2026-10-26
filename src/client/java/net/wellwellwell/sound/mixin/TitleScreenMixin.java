package net.wellwellwell.sound.mixin;

import net.wellwellwell.sound.DeathSoundScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init")
    private void onInit(CallbackInfo info) {
        int xPos = this.width / 2 + 104;
        int yStart = this.height / 4 + 48;

        this.addRenderableWidget(Button.builder(Component.literal("DS"), button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new DeathSoundScreen(this));
                    }
                })
                .bounds(xPos, yStart + 48, 20, 20)
                .tooltip(Tooltip.create(Component.literal("DeathSound Settings")))
                .build());
    }
}