package net.wellwellwell.sound;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
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

    public ManageListScreen(Screen parent) {
        super(Text.literal("Manage Player List"));
        this.parent = parent;
        this.tempTrackedPlayers = new ArrayList<>(ModConfig.trackedPlayers);
    }

    private record PlayerEntry(ButtonWidget nameBtn, ButtonWidget removeBtn, int baseY) {}

    @Override
    protected void init() {
        this.updateWidgets();
    }

    private void updateWidgets() {
        String currentText = (this.nameField != null) ? this.nameField.getText() : "";

        this.clearChildren();
        this.playerEntries.clear();

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§lPlayer List"), b -> {})
                .dimensions(this.width / 2 - 100, 10, 200, 20).build()).active = false;

        this.nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 35, 200, 20, Text.literal("Player Name"));
        this.nameField.setText(currentText);
        this.addSelectableChild(this.nameField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§cClear List"), button -> {
            tempTrackedPlayers.clear();
            this.updateWidgets();
        }).dimensions(this.width / 2 - 100, 60, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§aAdd Player"), button -> {
            addPlayer();
        }).dimensions(this.width / 2 + 2, 60, 98, 20).build());

        int currentY = 85;
        for (String playerName : tempTrackedPlayers) {
            ButtonWidget nBtn = ButtonWidget.builder(Text.literal("§7" + playerName), b -> {})
                    .dimensions(this.width / 2 - 100, 0, 178, 20).build();
            nBtn.active = false;

            ButtonWidget xBtn = ButtonWidget.builder(Text.literal("§l§cX"), b -> {
                        tempTrackedPlayers.remove(playerName);
                        this.updateWidgets();
                    })
                    .dimensions(this.width / 2 + 80, 0, 20, 20)
                    .tooltip(Tooltip.of(Text.literal("Click to remove")))
                    .build();

            this.playerEntries.add(new PlayerEntry(nBtn, xBtn, currentY));
            this.addSelectableChild(nBtn);
            this.addSelectableChild(xBtn);
            currentY += 22;
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§cCancel"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 100, this.height - 25, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§aSave"), button -> {
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            addPlayer();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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
        if (this.nameField != null) {
            this.nameField.render(context, mouseX, mouseY, delta);
        }
    }

    private void drawScrollbar(DrawContext context, int top, int bottom) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int contentHeight = playerEntries.size() * 22;
        int viewHeight = this.height - 115;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = MathHelper.clamp(this.scrollAmount - amount * 12, 0, maxScroll);
        return true;
    }
}