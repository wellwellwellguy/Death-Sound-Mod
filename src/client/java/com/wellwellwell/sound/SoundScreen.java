package com.wellwellwell.sound;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.List;

public class SoundScreen extends Screen {
    private final Screen parent;
    private double scrollAmount = 0;

    private final List<String> installedSounds = List.of(
            "classic_hurt", "wither", "thunder", "explosion",
            "shrieker", "warden_heartbeat", "totem", "anvil", "ghast",
            "enderdragon", "elder_guardian", "amethyst", "villager_no", "bell",
            "creeper", "level_up", "goat_scream", "plinth", "enderman"
    );

    private final List<Button> soundButtons = new ArrayList<>();
    private final String selectedSoundKey;
    private String tempSelectedSound;

    private final int entryHeight = 22;
    private final int itemWidth = 280;
    private boolean isDraggingScrollbar = false;

    public SoundScreen(Screen parent) {
        super(Component.translatable("deathsound.screen.sounds.title"));
        this.parent = parent;

        this.selectedSoundKey = ModConfig.selectedSound;
        this.tempSelectedSound = this.selectedSoundKey;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.soundButtons.clear();

        int buttonWidth = 139;
        int gap = 2;
        int startX = this.width / 2 - (itemWidth / 2);

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.manage_list.cancel"), button -> {
            if (this.minecraft != null) this.minecraft.setScreenAndShow(this.parent);
        }).bounds(startX, this.height - 28, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.manage_list.save"), button -> {
            ModConfig.selectedSound = SoundScreen.this.tempSelectedSound;
            ModConfig.save();
            if (this.minecraft != null) this.minecraft.setScreenAndShow(this.parent);
        }).bounds(startX + buttonWidth + gap, this.height - 28, buttonWidth, 20).build());

        this.buildSoundList();
        this.clampScroll();
    }

    private void buildSoundList() {
        for (Button btn : soundButtons) {
            this.removeWidget(btn);
        }
        soundButtons.clear();

        for (String soundId : installedSounds) {
            String displayName = soundId.replace("_", " ").toUpperCase();

            Button button = Button.builder(
                            Component.literal(displayName),
                            btn -> this.tempSelectedSound = soundId
                    )
                    .bounds(this.width / 2 - itemWidth / 2, 0, itemWidth, 20)
                    .build();

            button.visible = true;
            this.soundButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        int listTop = 60;
        int listBottom = this.height - 40;
        int scrollbarLeft = this.width / 2 + 144;
        int scrollbarRight = scrollbarLeft + 6;

        if (mouseX >= scrollbarLeft && mouseX <= scrollbarRight && mouseY >= listTop && mouseY <= listBottom) {
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
        int listTop = 60;
        int listBottom = this.height - 40;
        int viewHeight = listBottom - listTop;
        int contentHeight = installedSounds.size() * entryHeight;

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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int listTop = 60;
        int listBottom = this.height - 40;

        guiGraphics.fill(0, listTop, this.width, listBottom, 0x50000000);

        int startY = listTop;
        for (int i = 0; i < soundButtons.size(); i++) {
            Button btn = soundButtons.get(i);
            String soundId = installedSounds.get(i);
            int renderY = (int) (startY + (i * entryHeight) - scrollAmount);

            btn.setY(renderY);

            boolean isSelected = soundId.equals(this.tempSelectedSound);

            if (isSelected) {
                btn.setAlpha(1f);
            } else if (btn.isHovered()) {
                btn.setAlpha(0.80f);
            } else {
                btn.setAlpha(0.75f);
            }

            if (renderY >= listTop && (renderY + btn.getHeight()) <= listBottom) {
                btn.visible = true;
            } else {
                btn.visible = false;
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.enableScissor(0, listTop, this.width, listBottom);
        for (int i = 0; i < soundButtons.size(); i++) {
            Button btn = soundButtons.get(i);
            if (btn.visible && installedSounds.get(i).equals(this.tempSelectedSound)) {
                int border = 0xFFFFFFFF;
                guiGraphics.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + 1, border);
                guiGraphics.fill(btn.getX(), btn.getY() + btn.getHeight() - 1, btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), border);
                guiGraphics.fill(btn.getX(), btn.getY(), btn.getX() + 1, btn.getY() + btn.getHeight(), border);
                guiGraphics.fill(btn.getX() + btn.getWidth() - 1, btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), border);
            }
        }
        guiGraphics.disableScissor();

        guiGraphics.fillGradient(0, listTop, this.width, listTop + 4, 0x50000000, 0x00000000);
        guiGraphics.fillGradient(0, listBottom - 4, this.width, listBottom, 0x00000000, 0x50000000);

        guiGraphics.fill(0, listTop - 2, this.width, listTop - 1, 0xFF000000);
        guiGraphics.fill(0, listTop - 1, this.width, listTop,     0xFF555555);
        guiGraphics.fill(0, listBottom,     this.width, listBottom + 1, 0xFF000000);
        guiGraphics.fill(0, listBottom + 1, this.width, listBottom + 2, 0xFF555555);

        this.drawScrollbar(guiGraphics, listTop, listBottom);

        Component headerText = Component.translatable("deathsound.screen.sounds.select_below");
        int textWidth = this.font.width(headerText);
        guiGraphics.text(this.font, headerText, this.width / 2 - (textWidth / 2), 25, 0xFFAAAAAA, true);
    }

    private void drawScrollbar(GuiGraphicsExtractor guiGraphics, int top, int bottom) {
        int contentHeight = installedSounds.size() * entryHeight;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int contentHeight = installedSounds.size() * entryHeight;
        int viewHeight = this.height - 100;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * 12, 0, maxScroll);
        return true;
    }

    private void clampScroll() {
        int contentHeight = installedSounds.size() * entryHeight;
        int viewHeight = this.height - 100;
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        if (this.scrollAmount > maxScroll) {
            this.scrollAmount = maxScroll;
        }
    }
}