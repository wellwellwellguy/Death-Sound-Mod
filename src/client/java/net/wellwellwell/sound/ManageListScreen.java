package net.wellwellwell.sound;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.List;

public class ManageListScreen extends Screen {
    private final Screen parent;
    private EditBox nameField;
    private double scrollAmount = 0;
    private final List<PlayerEntry> playerEntries = new ArrayList<>();
    private final List<String> tempTrackedPlayers;

    public ManageListScreen(Screen parent) {
        super(Component.literal("Manage Player List"));
        this.parent = parent;
        this.tempTrackedPlayers = new ArrayList<>(ModConfig.trackedPlayers);
    }

    private record PlayerEntry(Button nameBtn, Button removeBtn, int baseY) {}

    @Override
    protected void init() {
        this.updateWidgets();
    }

    private void updateWidgets() {
        String currentText = (this.nameField != null) ? this.nameField.getValue() : "";

        this.clearWidgets();
        this.playerEntries.clear();

        this.addRenderableWidget(Button.builder(Component.literal("§lPlayer List"), b -> {})
                .bounds(this.width / 2 - 100, 10, 200, 20).build()).active = false;

        this.nameField = new EditBox(this.font, this.width / 2 - 100, 35, 200, 20, Component.literal("Player Name"));
        this.nameField.setValue(currentText);
        this.addRenderableWidget(this.nameField);

        this.addRenderableWidget(Button.builder(Component.literal("§cClear List"), button -> {
            tempTrackedPlayers.clear();
            this.updateWidgets();
        }).bounds(this.width / 2 - 100, 60, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§aAdd Player"), button -> {
            addPlayer();
        }).bounds(this.width / 2 + 2, 60, 98, 20).build());

        int currentY = 85;
        for (String playerName : tempTrackedPlayers) {
            Button nBtn = Button.builder(Component.literal("§7" + playerName), b -> {})
                    .bounds(this.width / 2 - 100, 0, 178, 20).build();
            nBtn.active = false;

            Button xBtn = Button.builder(Component.literal("§l§cX"), b -> {
                        tempTrackedPlayers.remove(playerName);
                        this.updateWidgets();
                    })
                    .bounds(this.width / 2 + 80, 0, 20, 20)
                    .tooltip(Tooltip.create(Component.literal("Click to remove")))
                    .build();

            this.playerEntries.add(new PlayerEntry(nBtn, xBtn, currentY));
            this.addRenderableWidget(nBtn);
            this.addRenderableWidget(xBtn);
            currentY += 22;
        }

        this.addRenderableWidget(Button.builder(Component.literal("§cCancel"), button -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 100, this.height - 25, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§aSave"), button -> {
            ModConfig.trackedPlayers.clear();
            ModConfig.trackedPlayers.addAll(tempTrackedPlayers);
            ModConfig.save();
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 + 2, this.height - 25, 98, 20).build());
    }

    private void addPlayer() {
        String name = nameField.getValue().trim();
        if (!name.isEmpty() && !tempTrackedPlayers.contains(name)) {
            tempTrackedPlayers.add(name);
            nameField.setValue("");
            this.updateWidgets();
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
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);

        super.render(context, mouseX, mouseY, delta);

        int listTop = 85;
        int listBottom = this.height - 30;

        context.enableScissor(this.width / 2 - 110, listTop, this.width / 2 + 110, listBottom);
        for (PlayerEntry entry : playerEntries) {
            int renderY = (int) (entry.baseY - scrollAmount);
            if (renderY + 20 > listTop && renderY < listBottom) {
                entry.nameBtn().setY(renderY);
                entry.removeBtn().setY(renderY);
                entry.nameBtn().render(context, mouseX, mouseY, delta);
                entry.removeBtn().render(context, mouseX, mouseY, delta);
            } else {
                entry.nameBtn().setY(-200);
                entry.removeBtn().setY(-200);
            }
        }
        context.disableScissor();

        this.drawScrollbar(context, listTop, listBottom);
    }

    private void drawScrollbar(GuiGraphics context, int top, int bottom) {
        int contentHeight = playerEntries.size() * 22;
        int viewHeight = bottom - top;
        if (contentHeight <= viewHeight) return;

        int x = this.width / 2 + 105;
        int scrollbarHeight = (int) ((float) viewHeight / contentHeight * viewHeight);
        int scrollbarPos = (int) ((float) scrollAmount / (contentHeight - viewHeight) * (viewHeight - scrollbarHeight));

        context.fill(x, top, x + 4, bottom, 0x88000000);
        context.fill(x, top + scrollbarPos, x + 4, top + scrollbarPos + scrollbarHeight, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) { // amount wurde zu amountX, amountY
        int contentHeight = playerEntries.size() * 22;
        int viewHeight = this.height - 115;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = Mth.clamp(this.scrollAmount - amountY * 12, 0, maxScroll);
        return true;
    }
}