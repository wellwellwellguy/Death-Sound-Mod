package com.wellwellwell.sound.mixin;

import com.wellwellwell.sound.Screen.DeathSoundScreen;
import com.wellwellwell.sound.ModConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addDSButtonToTitle(CallbackInfo ci) {
        if (!ModConfig.showMenuButton) return;

        int xPos = this.width / 2 + 104 + 24;
        int yPos = this.height / 4 + 48 + 72 + 12;

        for (GuiEventListener listener : this.children()) {
            if (listener instanceof AbstractWidget widget) {
                Component message = widget.getMessage();

                if (message != null && message.getContents() instanceof TranslatableContents translatable) {
                    if ("menu.multiplayer".equals(translatable.getKey())) {
                        xPos = widget.getX() + widget.getWidth() + 4;
                        yPos = widget.getY();
                        break;
                    }
                }
            }
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
    }
}