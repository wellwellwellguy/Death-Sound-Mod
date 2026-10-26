package net.wellwellwell.sound.mixin;

import net.wellwellwell.sound.DeathSoundScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addDeathSoundButton(CallbackInfo ci) {
        this.children().stream()
                .filter(element -> element instanceof ButtonWidget)
                .map(element -> (ButtonWidget) element)
                .filter(button -> {
                    Text msg = button.getMessage();
                    if (msg.getContent() instanceof TranslatableTextContent trans) {
                        return trans.getKey().contains("accessibility");
                    }
                    return msg.getString().toLowerCase().contains("accessibility");
                })
                .findFirst()
                .ifPresent(accessibilityButton -> {
                    int x = accessibilityButton.getX();
                    int y = accessibilityButton.getY();
                    int width = accessibilityButton.getWidth();

                    this.addDrawableChild(ButtonWidget.builder(
                                    Text.literal("DS"),
                                    button -> {
                                        if (this.client != null) {
                                            this.client.setScreen(new DeathSoundScreen(this));
                                        }
                                    })
                            .dimensions(x + width + 2, y, 20, 20)
                            .tooltip(Tooltip.of(Text.literal("DeathSound Settings")))
                            .build());
                });
    }
}