package com.wellwellwell.sound;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.List;

public class ManageListScreen extends Screen {
    private final Screen parent;
    private EditBox nameInput;
    private final List<String> tempPlayers;
    private final List<Button> playerButtons = new ArrayList<>();

    private double scrollAmount = 0;
    private final int entryHeight = 22;
    private final int itemWidth = 280;
    private boolean isDraggingScrollbar = false;

    private final int listTop = 60;

    public ManageListScreen(Screen parent) {
        super(Component.translatable("deathsound.screen.manage_list.title"));
        this.parent = parent;
        this.tempPlayers = new ArrayList<>(ModConfig.trackedPlayers);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.playerButtons.clear();

        int centerX = this.width / 2;
        int startX = centerX - (itemWidth / 2);

        int inputWidth = 223;
        int addBtnWidth = 55;
        int gap = 2;

        this.nameInput = new EditBox(this.font, startX, 20, inputWidth, 20, Component.translatable("deathsound.screen.manage_list.input_label"));
        this.addRenderableWidget(this.nameInput);

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.manage_list.add"), button -> {
            String name = nameInput.getValue().trim();
            if (!name.isEmpty() && !tempPlayers.contains(name)) {
                tempPlayers.add(name);
                nameInput.setValue("");
                this.rebuildPlayerList();
                this.clampScroll();
            }
        }).bounds(startX + inputWidth + gap, 20, addBtnWidth, 20).build());
        int buttonWidth = 139;

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.manage_list.cancel"), button -> {
            if (this.minecraft != null) this.minecraft.setScreenAndShow(this.parent);
        }).bounds(startX, this.height - 28, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.manage_list.save"), button -> {
            ModConfig.trackedPlayers.clear();
            ModConfig.trackedPlayers.addAll(tempPlayers);
            ModConfig.save();
            if (this.minecraft != null) this.minecraft.setScreenAndShow(this.parent);
        }).bounds(startX + buttonWidth + gap, this.height - 28, buttonWidth, 20).build());

        this.rebuildPlayerList();
        this.clampScroll();
    }

    private void rebuildPlayerList() {
        for (Button btn : playerButtons) {
            this.removeWidget(btn);
        }
        playerButtons.clear();

        for (String playerName : tempPlayers) {
            Button button = Button.builder(
                            Component.literal(playerName),
                            btn -> {
                                tempPlayers.remove(playerName);
                                this.rebuildPlayerList();
                                this.clampScroll();
                            }
                    )
                    .bounds(this.width / 2 - itemWidth / 2, 0, itemWidth, 20)
                    .tooltip(Tooltip.create(Component.translatable("deathsound.screen.manage_list.remove_tooltip").withStyle(ChatFormatting.RED)))
                    .build();

            button.visible = true;
            this.playerButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int dynamicListBottom = this.height - 40;

        guiGraphics.fill(0, listTop, this.width, dynamicListBottom, 0x50000000);

        int startY = listTop;
        for (int i = 0; i < playerButtons.size(); i++) {
            Button btn = playerButtons.get(i);
            int renderY = (int) (startY + (i * entryHeight) - scrollAmount);

            btn.setY(renderY);

            if (btn.isHovered()) {
                btn.setAlpha(1.00f);
            } else {
                btn.setAlpha(0.90f);
            }

            if (renderY >= listTop && (renderY + btn.getHeight()) <= dynamicListBottom) {
                btn.visible = true;
            } else {
                btn.visible = false;
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.fillGradient(0, listTop, this.width, listTop + 4, 0x50000000, 0x00000000);
        guiGraphics.fillGradient(0, dynamicListBottom - 4, this.width, dynamicListBottom, 0x00000000, 0x50000000);

        guiGraphics.fill(0, listTop - 2, this.width, listTop - 1, 0xFF000000);
        guiGraphics.fill(0, listTop - 1, this.width, listTop,     0xFF555555);
        guiGraphics.fill(0, dynamicListBottom,     this.width, dynamicListBottom + 1, 0xFF000000);
        guiGraphics.fill(0, dynamicListBottom + 1, this.width, dynamicListBottom + 2, 0xFF555555);

        this.drawScrollbar(guiGraphics, listTop, dynamicListBottom);
    }

    private void drawScrollbar(GuiGraphicsExtractor guiGraphics, int top, int bottom) {
        int contentHeight = tempPlayers.size() * entryHeight;
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
        int contentHeight = tempPlayers.size() * entryHeight;

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
        int contentHeight = tempPlayers.size() * entryHeight;
        int viewHeight = dynamicListBottom - listTop;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * 12, 0, maxScroll);
        return true;
    }

    private void clampScroll() {
        int dynamicListBottom = this.height - 40;
        int contentHeight = tempPlayers.size() * entryHeight;
        int viewHeight = dynamicListBottom - listTop;
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        if (this.scrollAmount > maxScroll) {
            this.scrollAmount = maxScroll;
        }
    }
}