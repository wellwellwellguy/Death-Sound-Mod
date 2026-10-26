package net.wellwellwell.sound;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.util.ArrayList;
import java.util.List;

public class ManageListScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private double scrollAmount = 0;
    private final List<PlayerEntry> playerEntries = new ArrayList<>();
    private final List<String> tempTrackedPlayers;
    private boolean isDraggingScrollbar = false;

    public ManageListScreen(Screen parent) {
        super(Text.translatable("deathsound.screen.manage_list.title"));
        this.parent = parent;
        this.tempTrackedPlayers = new ArrayList<>(ModConfig.trackedPlayers);
    }

    private record PlayerEntry(ButtonWidget nameBtn, int baseY) {}

    @Override
    protected void init() {
        this.updateWidgets();
    }

    private void updateWidgets() {
        String currentText = (this.nameField != null) ? this.nameField.getText() : "";

        this.clearChildren();
        this.playerEntries.clear();

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.manage_list.header"), b -> {})
                .dimensions(this.width / 2 - 100, 10, 200, 20).build()).active = false;

        this.nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 35, 200, 20, Text.translatable("deathsound.screen.manage_list.input_label"));
        this.nameField.setText(currentText);

        this.nameField.setFocused(true);
        this.addSelectableChild(this.nameField);
        this.setFocused(this.nameField);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.manage_list.clear"), button -> {
            tempTrackedPlayers.clear();
            this.updateWidgets();
        }).dimensions(this.width / 2 - 100, 60, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.manage_list.add"), button -> {
            addPlayer();
        }).dimensions(this.width / 2 + 2, 60, 98, 20).build());

        List<String> playersToRender = new ArrayList<>(tempTrackedPlayers);
        int currentY = 87;

        Tooltip removeTooltip = Tooltip.of(Text.translatable("deathsound.screen.manage_list.remove_tooltip"));

        for (String playerName : playersToRender) {
            ButtonWidget nBtn = ButtonWidget.builder(Text.literal("§7" + playerName), b -> {
                        tempTrackedPlayers.remove(playerName);
                        this.updateWidgets();
                    })
                    .dimensions(this.width / 2 - 100, 0, 200, 20)
                    .tooltip(removeTooltip)
                    .build();

            this.playerEntries.add(new PlayerEntry(nBtn, currentY));
            this.addSelectableChild(nBtn);
            currentY += 22;
        }

        this.clampScroll();

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.manage_list.cancel"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 100, this.height - 25, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.manage_list.save"), button -> {
            ModConfig.trackedPlayers.clear();
            ModConfig.trackedPlayers.addAll(tempTrackedPlayers);
            ModConfig.save();
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 + 2, this.height - 25, 98, 20).build());
    }

    private void addPlayer() {
        String name = nameField.getText().trim();
        if (!name.isEmpty() && !tempTrackedPlayers.contains(name)) {
            tempTrackedPlayers.add(name);
            nameField.setText("");
            this.updateWidgets();
        }
    }

    @Override
    public void tick() {
        if (this.nameField != null && !this.nameField.isFocused()) {
            this.nameField.setFocused(true);
            this.setFocused(this.nameField);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            addPlayer();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listTop = 85;
        int listBottom = this.height - 30;
        int scrollbarLeft = this.width / 2 + 104;
        int scrollbarRight = scrollbarLeft + 6;

        if (mouseX >= scrollbarLeft && mouseX <= scrollbarRight && mouseY >= listTop && mouseY <= listBottom) {
            this.isDraggingScrollbar = true;
            this.updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && this.isDraggingScrollbar) {
            this.updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void updateScrollFromMouse(double mouseY) {
        int listTop = 85;
        int listBottom = this.height - 30;
        int viewHeight = listBottom - listTop;
        int contentHeight = playerEntries.size() * 22;

        if (contentHeight <= viewHeight) return;

        int scrollbarHeight = (int) ((float) viewHeight / contentHeight * viewHeight);
        scrollbarHeight = MathHelper.clamp(scrollbarHeight, 32, viewHeight);

        double maxScroll = contentHeight - viewHeight;
        double clickedPos = (mouseY - listTop) - (scrollbarHeight / 2.0);

        double scrollPercent = clickedPos / (double) (viewHeight - scrollbarHeight);
        scrollPercent = MathHelper.clamp(scrollPercent, 0.0, 1.0);

        this.scrollAmount = scrollPercent * maxScroll;
        this.clampScroll();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int listTop = 85;
        int listBottom = this.height - 30;

        context.fill(0, listTop, this.width, listBottom, 0x50000000);
        context.enableScissor(0, listTop, this.width, listBottom);

        for (PlayerEntry entry : playerEntries) {
            int renderY = (int) (entry.baseY - scrollAmount);

            if (renderY + 20 > listTop && renderY < listBottom) {
                entry.nameBtn().setY(renderY);
                entry.nameBtn().render(context, mouseX, mouseY, delta);
            } else {
                entry.nameBtn().setY(-100);
            }
        }
        context.disableScissor();

        context.fillGradient(0, listTop, this.width, listTop + 4, 0x50000000, 0x00000000);
        context.fillGradient(0, listBottom - 4, this.width, listBottom, 0x00000000, 0x50000000);

        context.fill(0, listTop - 2, this.width, listTop - 1, 0xFF000000);
        context.fill(0, listTop - 1, this.width, listTop,     0xFF555555);
        context.fill(0, listBottom,     this.width, listBottom + 1, 0xFF000000);
        context.fill(0, listBottom + 1, this.width, listBottom + 2, 0xFF555555);

        this.drawScrollbar(context, listTop, listBottom);

        if (this.nameField != null) {
            this.nameField.render(context, mouseX, mouseY, delta);
        }
    }


    private void drawScrollbar(DrawContext context, int top, int bottom) {
        int contentHeight = playerEntries.size() * 22;
        int viewHeight = bottom - top;
        if (contentHeight <= viewHeight) return;

        int x = this.width / 2 + 104;
        int scrollbarHeight = (int) ((float) viewHeight / contentHeight * viewHeight);
        scrollbarHeight = MathHelper.clamp(scrollbarHeight, 32, viewHeight);

        int scrollbarPos = (int) ((float) scrollAmount / (contentHeight - viewHeight) * (viewHeight - scrollbarHeight));

        context.fill(x, top, x + 6, bottom, 0xFF000000);
        context.fill(x, top + scrollbarPos, x + 6, top + scrollbarPos + scrollbarHeight, 0xFF808080);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentHeight = playerEntries.size() * 22;
        int viewHeight = this.height - 115;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = MathHelper.clamp(this.scrollAmount - verticalAmount * 12, 0, maxScroll);
        return true;
    }

    private void clampScroll() {
        int contentHeight = playerEntries.size() * 22;
        int viewHeight = this.height - 115;
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        if (this.scrollAmount > maxScroll) {
            this.scrollAmount = maxScroll;
        }
    }
}