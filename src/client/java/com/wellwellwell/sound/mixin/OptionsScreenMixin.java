package com.wellwellwell.sound.mixin;

import com.wellwellwell.sound.DeathSoundScreen;
import com.wellwellwell.sound.ModConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
    private void addDSButtonToOptions(CallbackInfo ci) {
        if (!ModConfig.showMenuButton) return;
        for (GuiEventListener element : this.children()) {
            if (element instanceof AbstractWidget widget) {
                if (widget.getMessage().getString().isEmpty() || widget.getWidth() == 20) {
                    int xPos = widget.getX() + widget.getWidth() + 4;
                    int yPos = widget.getY();

                    if (xPos + 20 > this.width) {
                        xPos = widget.getX() - 24;
                    }

                    this.addRenderableWidget(Button.builder(
                                    Component.literal("DS"),
                                    button -> {
                                        if (this.minecraft != null) {
                                            this.minecraft.setScreenAndShow(new DeathSoundScreen(this));
                                        }
                                    })
                            .bounds(xPos, yPos, 20, 20)
                            .tooltip(Tooltip.create(Component.translatable("deathsound.menu.tooltip")))
                            .build());
                    break;
                }
            }
        }
    }
}