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
        this.children().stream()
                .filter(element -> element instanceof Button)
                .map(element -> (Button) element)
                .filter(button -> {
                    String rawText = button.getMessage().getString().toLowerCase();
                    return rawText.contains("lan") || rawText.contains("bugs");
                })
                .findFirst()
                .ifPresent(targetButton -> {
                    int x = targetButton.x;
                    int y = targetButton.y;
                    int width = targetButton.getWidth();

                    this.addRenderableWidget(new Button(
                                    x + width + 2,
                                    y,
                                    20,
                                    20,
                                    Component.literal("DS"),
                                    button -> {
                                        if (this.minecraft != null) {
                                            this.minecraft.setScreen(new DeathSoundScreen(this));
                                        }
                                    }
                            )
                    );
                });
    }
}