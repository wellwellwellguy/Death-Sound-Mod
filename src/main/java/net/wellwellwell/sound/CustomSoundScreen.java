package net.wellwellwell.sound;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CustomSoundScreen extends Screen {
    private final Screen parent;
    private boolean tempCustomEnabled;
    private String selectedFileName;
    private double scrollAmount = 0;
    private boolean isDraggingScrollbar = false;

    private final List<String> availableSounds = new ArrayList<>();
    private final List<ButtonWidget> soundButtons = new ArrayList<>();

    private final int entryHeight = 22;
    private final int itemWidth = 280;

    public CustomSoundScreen(Screen parent) {
        super(Text.translatable("deathsound.screen.custom.title"));
        this.parent = parent;
        this.tempCustomEnabled = ModConfig.customSoundEnabled;
        this.selectedFileName = ModConfig.customSoundFileName;
        loadSoundFiles();
    }

    private void loadSoundFiles() {
        availableSounds.clear();
        File folder = new File(MinecraftClient.getInstance().runDirectory, "resourcepacks/DeathSound-Resources/assets/deathsound/sounds");
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".ogg"));
            if (files != null) {
                for (File f : files) availableSounds.add(f.getName().replace(".ogg", ""));
            }
        }
    }

    @Override
    protected void init() {
        this.updateWidgets();
    }

    private void updateWidgets() {
        this.clearChildren();
        this.soundButtons.clear();

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.manage_list.cancel"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 155, this.height - 28, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.manage_list.save"), button -> {
            ModConfig.customSoundEnabled = tempCustomEnabled;
            if (selectedFileName != null) {
                ModConfig.customSoundFileName = selectedFileName;
            }

            if (tempCustomEnabled && selectedFileName != null) {
                ModConfig.lastSoundTimestamp = System.currentTimeMillis();
                saveSoundsJson(this.client, selectedFileName);
                activateResourcePack();
                this.client.reloadResources();
            }

            ModConfig.save();
            this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 + 5, this.height - 28, 150, 20).build());

        int startX = this.width / 2 - (itemWidth / 2);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("\uD83D\uDDD8"), b -> {
            CustomSoundPack.syncSounds();
            this.loadSoundFiles();
            this.updateWidgets();
        }).tooltip(Tooltip.of(Text.of("Reload sounds"))).dimensions(startX, 36, 20, 20).build());

        Text statusText = tempCustomEnabled ? Text.translatable("deathsound.status.on") : Text.translatable("deathsound.status.off");
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.custom.toggle").append(statusText), b -> {
            tempCustomEnabled = !tempCustomEnabled;
            this.updateWidgets();
        }).dimensions(startX + 22, 36, 236, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("\uD83D\uDDC1"), b -> {
            File folder = new File(this.client.runDirectory, "config/deathsound/sounds");
            if (!folder.exists()) folder.mkdirs();
            Util.getOperatingSystem().open(folder);
        }).dimensions(startX + 260, 36, 20, 20).tooltip(Tooltip.of(Text.translatable("deathsound.screen.custom.open_folder"))).build());

        this.buildSoundList();
        this.clampScroll();
    }

    private void buildSoundList() {
        for (int i = 0; i < availableSounds.size(); i++) {
            String soundName = availableSounds.get(i);
            boolean isSelected = soundName.equals(selectedFileName);

            ButtonWidget sBtn = ButtonWidget.builder(Text.literal((isSelected ? "> " : "") + soundName + ".ogg"), b -> {
                selectedFileName = soundName;
                this.updateWidgets();
            }).dimensions(this.width / 2 - itemWidth / 2, 0, itemWidth, 20).build();

            sBtn.active = tempCustomEnabled;

            this.soundButtons.add(sBtn);
            this.addSelectableChild(sBtn);
        }
    }

    private void activateResourcePack() {
        if (this.client == null) return;
        var manager = this.client.getResourcePackManager();
        String packId = "file/DeathSound-Resources";
        java.util.Collection<String> enabledPacks = new java.util.HashSet<>(manager.getEnabledIds());
        if (!enabledPacks.contains(packId)) {
            enabledPacks.add(packId);
            manager.setEnabledProfiles(enabledPacks);
        }
    }

    public static void saveSoundsJson(MinecraftClient client, String name) {
        try {
            File file = new File(client.runDirectory, "resourcepacks/DeathSound-Resources/assets/deathsound/sounds.json");
            if (file.getParentFile() != null) file.getParentFile().mkdirs();

            String jsonContent = String.format(
                    "{\n  \"custom_ogg\": {\n    \"sounds\": [\n      {\n        \"name\": \"deathsound:%s\",\n        \"stream\": true\n      }\n    ]\n  }\n}",
                    name
            );
            java.nio.file.Files.writeString(file.toPath(), jsonContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listTop = 60;
        int listBottom = this.height - 40;
        int scrollbarLeft = this.width / 2 + 144;
        int scrollbarRight = scrollbarLeft + 6;

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (tempCustomEnabled && mouseX >= scrollbarLeft && mouseX <= scrollbarRight && mouseY >= listTop && mouseY <= listBottom) {
            this.isDraggingScrollbar = true;
            this.updateScrollFromMouse(mouseY);
            return true;
        }

        if (mouseY >= listTop && mouseY <= listBottom && tempCustomEnabled) {
            for (ButtonWidget btn : soundButtons) {
                if (btn.isMouseOver(mouseX, mouseY)) {
                    if (btn.getY() >= listTop && (btn.getY() + btn.getHeight()) <= listBottom) {
                        return btn.mouseClicked(mouseX, mouseY, button);
                    }
                }
            }
        }
        return false;
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
        if (button == 0 && this.isDraggingScrollbar && tempCustomEnabled) {
            this.updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void updateScrollFromMouse(double mouseY) {
        int listTop = 60;
        int listBottom = this.height - 40;
        int viewHeight = listBottom - listTop;
        int contentHeight = soundButtons.size() * entryHeight;

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

        int listTop = 60;
        int listBottom = this.height - 40;

        context.fill(0, listTop, this.width, listBottom, 0x50000000);
        context.enableScissor(0, listTop, this.width, listBottom);

        int startY = listTop;
        for (int i = 0; i < soundButtons.size(); i++) {
            ButtonWidget btn = soundButtons.get(i);
            int renderY = (int) (startY + (i * entryHeight) - scrollAmount);

            if (renderY + 20 > listTop && renderY < listBottom) {
                btn.setY(renderY);
                btn.render(context, mouseX, mouseY, delta);
            } else {
                btn.setY(-100);
            }
        }
        context.disableScissor();

        context.fillGradient(0, listTop, this.width, listTop + 4, 0x50000000, 0x00000000);
        context.fillGradient(0, listBottom - 4, this.width, listBottom, 0x00000000, 0x50000000);

        context.fill(0, listTop - 2, this.width, listTop - 1, 0xFF000000);
        context.fill(0, listTop - 1, this.width, listTop,     0xFF555555);
        context.fill(0, listBottom,     this.width, listBottom + 1, 0xFF000000);
        context.fill(0, listBottom + 1, this.width, listBottom + 2, 0xFF555555);

        if (tempCustomEnabled) {
            this.drawScrollbar(context, listTop, listBottom);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("deathsound.screen.custom.header"), this.width / 2, 25, 0xFFAAAAAA);
    }

    private void drawScrollbar(DrawContext context, int top, int bottom) {
        int contentHeight = soundButtons.size() * entryHeight;
        int viewHeight = bottom - top;
        if (contentHeight <= viewHeight) return;

        int x = this.width / 2 + 144;
        int scrollbarHeight = (int) ((float) viewHeight / contentHeight * viewHeight);
        scrollbarHeight = MathHelper.clamp(scrollbarHeight, 32, viewHeight);

        int scrollbarPos = (int) ((float) scrollAmount / (contentHeight - viewHeight) * (viewHeight - scrollbarHeight));

        context.fill(x, top, x + 6, bottom, 0xFF000000);
        context.fill(x, top + scrollbarPos, x + 6, top + scrollbarPos + scrollbarHeight, 0xFF808080);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!tempCustomEnabled) return false;
        int contentHeight = soundButtons.size() * entryHeight;
        int viewHeight = this.height - 100;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = MathHelper.clamp(this.scrollAmount - verticalAmount * 12, 0, maxScroll);
        return true;
    }

    private void clampScroll() {
        int contentHeight = soundButtons.size() * entryHeight;
        int viewHeight = this.height - 100;
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        if (this.scrollAmount > maxScroll) {
            this.scrollAmount = maxScroll;
        }
    }
}