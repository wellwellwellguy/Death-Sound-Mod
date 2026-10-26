package net.wellwellwell.sound.mixin;

import net.wellwellwell.sound.DeathSoundScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import net.wellwellwell.sound.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuMixin extends Screen {
    protected GameMenuMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("RETURN"))
    private void addDeathSoundButtons(CallbackInfo ci) {

        if (!ModConfig.showMenuButton) return;

        int xPosition = this.width / 2 + 104;
        int yPosition = this.height / 4 + 56;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("DS"),
                        button -> {
                            if (this.client != null) {
                                this.client.setScreen(new DeathSoundScreen(this));
                            }
                        })
                .dimensions(xPosition, yPosition + 24, 20, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.menu.tooltip")))
                .build());
    }
}