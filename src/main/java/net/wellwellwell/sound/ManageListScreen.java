package net.wellwellwell.sound;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
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
        super(new TextComponent("Manage Player List"));
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

        this.addRenderableWidget(new Button(this.width / 2 - 100, 10, 200, 20, new TextComponent("§lPlayer List"), b -> {})).active = false;

        this.nameField = new net.minecraft.client.gui.components.EditBox(this.font, this.width / 2 - 100, 35, 200, 20, new TextComponent("Player Name"));
        this.nameField.setValue(currentText);
        this.addWidget(this.nameField);

        this.addRenderableWidget(new Button(this.width / 2 - 100, 60, 98, 20, new TextComponent("§cClear List"), button -> {
            tempTrackedPlayers.clear();
            this.updateWidgets();
        }));

        this.addRenderableWidget(new Button(this.width / 2 + 2, 60, 98, 20, new TextComponent("§aAdd Player"), button -> {
            addPlayer();
        }));

        int currentY = 85;
        for (String playerName : tempTrackedPlayers) {
            Button nBtn = new Button(this.width / 2 - 100, 0, 178, 20, new TextComponent("§7" + playerName), b -> {});
            nBtn.active = false;

            Button xBtn = new Button(this.width / 2 + 80, 0, 20, 20, new TextComponent("§l§cX"), b -> {
                tempTrackedPlayers.remove(playerName);
                this.updateWidgets();
            }, (button, poseStack, mouseX, mouseY) -> {
                this.renderTooltip(poseStack, new TextComponent("Remove"), mouseX, mouseY);
            });

            this.playerEntries.add(new PlayerEntry(nBtn, xBtn, currentY));
            this.addWidget(nBtn);
            this.addWidget(xBtn);
            currentY += 22;
        }

        this.addRenderableWidget(new Button(this.width / 2 - 100, this.height - 25, 98, 20, new TextComponent("§cCancel"), button -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }));

        this.addRenderableWidget(new Button(this.width / 2 + 2, this.height - 25, 98, 20, new TextComponent("§aSave"), button -> {
            ModConfig.trackedPlayers.clear();
            ModConfig.trackedPlayers.addAll(tempTrackedPlayers);
            ModConfig.save();
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }));
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
        this.renderBackground(poseStack); // In 1.19 Standard-Hintergrund
        super.render(poseStack, mouseX, mouseY, delta);

        int listTop = 85;
        int listBottom = this.height - 30;

        // FIX: Scissor in 1.19
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
                entry.nameBtn.y = renderY;
                entry.removeBtn.y = renderY;
                entry.nameBtn.render(poseStack, mouseX, mouseY, delta);
                entry.removeBtn.render(poseStack, mouseX, mouseY, delta);
            } else {
                entry.nameBtn.y = -200;
                entry.removeBtn.y = -200;
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