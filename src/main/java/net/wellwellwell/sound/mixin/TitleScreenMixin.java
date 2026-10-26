package net.wellwellwell.sound.mixin;

import net.wellwellwell.sound.DeathSoundScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init()V")
    private void onInit(CallbackInfo info) {
        int xPos = this.width / 2 + 104;
        int yStart = this.height / 4 + 48;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("DS"), button -> {
                    if (this.client != null) {
                        this.client.setScreen(new DeathSoundScreen(this));
                    }
                })
                .dimensions(xPos, yStart + 48, 20, 20)
                .tooltip(Tooltip.of(Text.literal("DeathSound Settings")))
                .build());
    }
}