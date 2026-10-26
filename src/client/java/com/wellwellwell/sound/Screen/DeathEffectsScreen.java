package com.wellwellwell.sound.Screen;

import com.wellwellwell.sound.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class DeathEffectsScreen extends Screen {
    private final Screen parent;
    private double scrollAmount = 0;

    private final List<String> availableEffects = List.of(
            "lightning", "explosion", "bad_omen", "smoke",
            "shriek", "sonic_boom", "totem", "firework"
    );

    private final List<Button> effectButtons = new ArrayList<>();
    private boolean tempDeathEffects;
    private String tempSelectedEffect;

    private final int entryHeight = 22;
    private final int itemWidth = 280;
    private boolean isDraggingScrollbar = false;

    private final int listTop = 60;

    public DeathEffectsScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;

        this.tempDeathEffects = ModConfig.deathEffects;
        this.tempSelectedEffect = (ModConfig.selectedEffect == null || ModConfig.selectedEffect.isEmpty())
                ? "lightning" : ModConfig.selectedEffect;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.effectButtons.clear();

        int centerX = this.width / 2;
        int startX = centerX - (itemWidth / 2);
        int gap = 2;

        this.addRenderableWidget(Button.builder(getDeathEffectsText(), b -> {
            this.tempDeathEffects = !this.tempDeathEffects;
            b.setMessage(getDeathEffectsText());
            this.updateButtonActivity();
        }).bounds(startX, 20, itemWidth, 20).build());

        int buttonWidth = 139;

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.manage_list.cancel"), button -> {
            if (this.minecraft != null) this.minecraft.setScreenAndShow(this.parent);
        }).bounds(startX, this.height - 28, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.manage_list.save"), button -> {
            ModConfig.deathEffects = this.tempDeathEffects;
            ModConfig.selectedEffect = this.tempSelectedEffect;
            ModConfig.save();
            if (this.minecraft != null) this.minecraft.setScreenAndShow(this.parent);
        }).bounds(startX + buttonWidth + gap, this.height - 28, buttonWidth, 20).build());

        this.buildEffectList();
        this.clampScroll();
    }

    private Component getDeathEffectsText() {
        return Component.translatable("deathsound.screen.settings.effects.toggle",
                tempDeathEffects ? Component.translatable("deathsound.status.on").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("deathsound.status.off").withStyle(ChatFormatting.RED));
    }

    private void buildEffectList() {
        for (Button btn : effectButtons) {
            this.removeWidget(btn);
        }
        effectButtons.clear();

        for (String effectId : availableEffects) {
            String displayName = effectId.replace("_", " ").toUpperCase();

            Button button = Button.builder(
                            Component.literal(displayName),
                            b -> this.tempSelectedEffect = effectId
                    )
                    .bounds(this.width / 2 - itemWidth / 2, 0, itemWidth, 20)
                    .build();

            button.visible = true;
            this.effectButtons.add(button);
            this.addRenderableWidget(button);
        }
        this.updateButtonActivity();
    }

    private void updateButtonActivity() {
        for (Button btn : effectButtons) {
            btn.active = this.tempDeathEffects;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int dynamicListBottom = this.height - 40;

        guiGraphics.fill(0, listTop, this.width, dynamicListBottom, 0x50000000);

        int startY = listTop;
        for (int i = 0; i < effectButtons.size(); i++) {
            Button btn = effectButtons.get(i);
            String effectId = availableEffects.get(i);
            int renderY = (int) (startY + (i * entryHeight) - scrollAmount);

            btn.setY(renderY);

            boolean isSelected = effectId.equals(this.tempSelectedEffect);

            if (isSelected && tempDeathEffects) {
                btn.setAlpha(1.0f);
            } else if (btn.isHovered() && tempDeathEffects) {
                btn.setAlpha(0.90f);
            } else {
                btn.setAlpha(0.80f);
            }

            if (renderY >= listTop && (renderY + btn.getHeight()) <= dynamicListBottom) {
                btn.visible = true;
            } else {
                btn.visible = false;
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.enableScissor(0, listTop, this.width, dynamicListBottom);
        for (int i = 0; i < effectButtons.size(); i++) {
            Button btn = effectButtons.get(i);
            if (btn.visible && availableEffects.get(i).equals(this.tempSelectedEffect) && tempDeathEffects) {
                int border = 0xFFFFFFFF;
                guiGraphics.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + 1, border);
                guiGraphics.fill(btn.getX(), btn.getY() + btn.getHeight() - 1, btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), border);
                guiGraphics.fill(btn.getX(), btn.getY(), btn.getX() + 1, btn.getY() + btn.getHeight(), border);
                guiGraphics.fill(btn.getX() + btn.getWidth() - 1, btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), border);
            }
        }
        guiGraphics.disableScissor();

        guiGraphics.fillGradient(0, listTop, this.width, listTop + 4, 0x50000000, 0x00000000);
        guiGraphics.fillGradient(0, dynamicListBottom - 4, this.width, dynamicListBottom, 0x00000000, 0x50000000);

        guiGraphics.fill(0, listTop - 2, this.width, listTop - 1, 0xFF000000);
        guiGraphics.fill(0, listTop - 1, this.width, listTop,     0xFF555555);
        guiGraphics.fill(0, dynamicListBottom,     this.width, dynamicListBottom + 1, 0xFF000000);
        guiGraphics.fill(0, dynamicListBottom + 1, this.width, dynamicListBottom + 2, 0xFF555555);

        this.drawScrollbar(guiGraphics, listTop, dynamicListBottom);
    }

    private void drawScrollbar(GuiGraphicsExtractor guiGraphics, int top, int bottom) {
        int contentHeight = availableEffects.size() * entryHeight;
        int viewHeight = bottom - top;
        if (contentHeight <= viewHeight) return;

        int x = this.width / 2 + 144;
        int scrollbarHeight = (int) ((float) viewHeight / contentHeight * viewHeight);
        scrollbarHeight = Mth.clamp(scrollbarHeight, 32, viewHeight);

        int scrollbarPos = (int) ((float) scrollAmount / (contentHeight - viewHeight) * (viewHeight - scrollbarHeight));

        guiGraphics.fill(x, top, x + 6, bottom, 0xFF000000);
        guiGraphics.fill(x, top + scrollbarPos, x + 6, top + scrollbarPos + scrollbarHeight, 0xFF808080);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        int dynamicListBottom = this.height - 40;
        int scrollbarLeft = this.width / 2 + 144;
        int scrollbarRight = scrollbarLeft + 6;

        if (mouseX >= scrollbarLeft && mouseX <= scrollbarRight && mouseY >= listTop && mouseY <= dynamicListBottom) {
            if (button == 0) {
                this.isDraggingScrollbar = true;
                this.updateScrollFromMouse(mouseY);
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() == 0 && this.isDraggingScrollbar) {
            this.updateScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    private void updateScrollFromMouse(double mouseY) {
        int dynamicListBottom = this.height - 40;
        int viewHeight = dynamicListBottom - listTop;
        int contentHeight = availableEffects.size() * entryHeight;

        if (contentHeight <= viewHeight) return;

        int scrollbarHeight = (int) ((float) viewHeight / contentHeight * viewHeight);
        scrollbarHeight = Mth.clamp(scrollbarHeight, 32, viewHeight);

        double maxScroll = contentHeight - viewHeight;
        double clickedPos = (mouseY - listTop) - (scrollbarHeight / 2.0);

        double scrollPercent = clickedPos / (double) (viewHeight - scrollbarHeight);
        scrollPercent = Mth.clamp(scrollPercent, 0.0, 1.0);

        this.scrollAmount = scrollPercent * maxScroll;
        this.clampScroll();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int dynamicListBottom = this.height - 40;
        int contentHeight = availableEffects.size() * entryHeight;
        int viewHeight = dynamicListBottom - listTop;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * 12, 0, maxScroll);
        return true;
    }

    private void clampScroll() {
        int dynamicListBottom = this.height - 40;
        int contentHeight = availableEffects.size() * entryHeight;
        int viewHeight = dynamicListBottom - listTop;
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        if (this.scrollAmount > maxScroll) {
            this.scrollAmount = maxScroll;
        }
    }
}