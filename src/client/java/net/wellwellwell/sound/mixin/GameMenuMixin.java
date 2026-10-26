package net.wellwellwell.sound.mixin;

import net.wellwellwell.sound.DeathSoundScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
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
                    Component msg = button.getMessage();
                    if (msg.getContents() instanceof TranslatableContents trans) {
                        String key = trans.getKey();
                        return key.contains("shareToLan") || key.contains("report_bugs");
                    }
                    String rawText = msg.getString().toLowerCase();
                    return rawText.contains("lan") || rawText.contains("bugs");
                })
                .findFirst()
                .ifPresent(targetButton -> {
                    int x = targetButton.getX();
                    int y = targetButton.getY();
                    int width = targetButton.getWidth();

                    this.addRenderableWidget(Button.builder(
                                    Component.literal("DS"),
                                    button -> {
                                        if (this.minecraft != null) {
                                            this.minecraft.setScreen(new DeathSoundScreen(this));
                                        }
                                    })
                            .bounds(x + width + 2, y, 20, 20)
                            .tooltip(Tooltip.create(Component.literal("DeathSound Settings")))
                            .build());
                });
    }
}