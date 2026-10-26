package net.wellwellwell.sound;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.List;

public class ManageListScreen extends Screen {
    private final Screen parent;
    private net.minecraft.client.gui.components.EditBox nameField;
    private double scrollAmount = 0;
    private final List<PlayerEntry> playerEntries = new ArrayList<>();
    private final List<String> tempTrackedPlayers;

    public ManageListScreen(Screen parent) {
        super(Component.literal("Manage Player List"));
        this.parent = parent;
        this.tempTrackedPlayers = new ArrayList<>(ModConfig.trackedPlayers);
    }

    private static class PlayerEntry {
        private final Button nameBtn;
        private final Button removeBtn;
        private final int baseY;

        public PlayerEntry(Button nameBtn, Button removeBtn, int baseY) {
            this.nameBtn = nameBtn;
            this.removeBtn = removeBtn;
            this.baseY = baseY;
        }
    }

    @Override
    protected void init() {
        this.updateWidgets();
    }

    private void updateWidgets() {
        String currentText = (this.nameField != null) ? this.nameField.getValue() : "";

        this.clearWidgets();
        this.playerEntries.clear();

        this.addRenderableWidget(Button.builder(Component.literal("§lPlayer List"), b -> {})
                .bounds(this.width / 2 - 100, 10, 200, 20)
                .build()).active = false;

        this.nameField = new net.minecraft.client.gui.components.EditBox(this.font, this.width / 2 - 100, 35, 200, 20, Component.literal("Player Name"));
        this.nameField.setValue(currentText);
        this.addWidget(this.nameField);

        this.addRenderableWidget(Button.builder(Component.literal("§cClear List"), button -> {
                    tempTrackedPlayers.clear();
                    this.updateWidgets();
                })
                .bounds(this.width / 2 - 100, 60, 98, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("§aAdd Player"), button -> {
                    addPlayer();
                })
                .bounds(this.width / 2 + 2, 60, 98, 20)
                .build());

        int currentY = 85;
        for (String playerName : tempTrackedPlayers) {
            Button nBtn = Button.builder(Component.literal("§7" + playerName), b -> {})
                    .bounds(this.width / 2 - 100, 0, 178, 20)
                    .build();
            nBtn.active = false;

            Button xBtn = Button.builder(Component.literal("§l§cX"), b -> {
                        tempTrackedPlayers.remove(playerName);
                        this.updateWidgets();
                    })
                    .bounds(this.width / 2 + 80, 0, 20, 20)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Remove")))
                    .build();

            this.playerEntries.add(new PlayerEntry(nBtn, xBtn, currentY));
            this.addWidget(nBtn);
            this.addWidget(xBtn);
            currentY += 22;
        }

        this.addRenderableWidget(Button.builder(Component.literal("§cCancel"), button -> {
                    if (this.minecraft != null) this.minecraft.setScreen(this.parent);
                })
                .bounds(this.width / 2 - 100, this.height - 25, 98, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("§aSave"), button -> {
                    ModConfig.trackedPlayers.clear();
                    ModConfig.trackedPlayers.addAll(tempTrackedPlayers);
                    ModConfig.save();
                    if (this.minecraft != null) this.minecraft.setScreen(this.parent);
                })
                .bounds(this.width / 2 + 2, this.height - 25, 98, 20)
                .build());
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
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, delta);

        int listTop = 85;
        int listBottom = this.height - 30;

        double scale = this.minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int) ((this.width / 2 - 110) * scale),
                (int) (this.minecraft.getWindow().getHeight() - (listBottom * scale)),
                (int) (220 * scale),
                (int) ((listBottom - listTop) * scale)
        );

        for (PlayerEntry entry : playerEntries) {
            int renderY = (int) (entry.baseY - scrollAmount);
            if (renderY + 20 > listTop && renderY < listBottom) {
                entry.nameBtn.setY(renderY);
                entry.removeBtn.setY(renderY);

                entry.nameBtn.render(poseStack, mouseX, mouseY, delta);
                entry.removeBtn.render(poseStack, mouseX, mouseY, delta);
            } else {
                entry.nameBtn.setY(-200);
                entry.removeBtn.setY(-200);
            }
        }
        RenderSystem.disableScissor();

        this.drawScrollbar(poseStack, listTop, listBottom);
        if (this.nameField != null) {
            this.nameField.render(poseStack, mouseX, mouseY, delta);
        }
    }

    private void drawScrollbar(PoseStack poseStack, int top, int bottom) {
        int contentHeight = playerEntries.size() * 22;
        int viewHeight = bottom - top;
        if (contentHeight <= viewHeight) return;

        int x = this.width / 2 + 105;
        int scrollbarHeight = (int) ((float) viewHeight / contentHeight * viewHeight);
        int scrollbarPos = (int) ((float) scrollAmount / (contentHeight - viewHeight) * (viewHeight - scrollbarHeight));

        fill(poseStack, x, top, x + 4, bottom, 0x88000000);
        fill(poseStack, x, top + scrollbarPos, x + 4, top + scrollbarPos + scrollbarHeight, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int contentHeight = playerEntries.size() * 22;
        int viewHeight = this.height - 115;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = Mth.clamp(this.scrollAmount - amount * 12, 0, maxScroll);
        return true;
    }
}