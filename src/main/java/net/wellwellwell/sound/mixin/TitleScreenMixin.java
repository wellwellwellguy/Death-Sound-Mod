package net.wellwellwell.sound.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.wellwellwell.sound.DeathSoundScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init()V")
    private void onInit(CallbackInfo info) {
        int xPos = this.width / 2 + 104;
        int yStart = this.height / 4 + 48;

        this.addRenderableWidget(new Button(
                xPos,
                yStart + 48,
                20,
                20,
                new TextComponent("DS"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new DeathSoundScreen(this));
                    }
                },
                (button, poseStack, mouseX, mouseY) -> {
                    this.renderTooltip(poseStack, new TextComponent("DeathSound Settings"), mouseX, mouseY);
                }
        ));
    }
}