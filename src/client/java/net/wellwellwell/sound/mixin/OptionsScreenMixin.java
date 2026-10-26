package net.wellwellwell.sound.mixin;

import net.wellwellwell.sound.DeathSoundScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) {
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
                        return trans.getKey().contains("accessibility");
                    }
                    return msg.getString().toLowerCase().contains("accessibility");
                })
                .findFirst()
                .ifPresent(accessibilityButton -> {
                    int x = accessibilityButton.getX();
                    int y = accessibilityButton.getY();
                    int width = accessibilityButton.getWidth();

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