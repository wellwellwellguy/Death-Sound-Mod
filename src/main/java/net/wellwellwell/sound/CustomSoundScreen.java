package net.wellwellwell.sound;

import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.wellwellwell.sound.CustomSoundPack.PACK_NAME;

public class CustomSoundScreen extends Screen {
    private final Screen parent;
    private boolean tempCustomEnabled;
    private String selectedFileName;
    private double scrollAmount = 0;

    private final List<String> availableSounds = new ArrayList<>();
    private final List<ButtonWidget> soundButtons = new ArrayList<>();

    public CustomSoundScreen(Screen parent) {
        super(Text.literal("Custom Sound Selection"));
        this.parent = parent;
        this.tempCustomEnabled = ModConfig.customSoundEnabled;
        this.selectedFileName = ModConfig.customSoundFileName;
        loadSoundFiles();
    }

    private void loadSoundFiles() {
        availableSounds.clear();
        File folder = new File(net.minecraft.client.MinecraftClient.getInstance().runDirectory, "resourcepacks/DeathSound-Resources/assets/deathsound/sounds");
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
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 100;
        this.clearChildren();
        this.soundButtons.clear();

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§lCustom DeathSounds"), b -> {})
                .dimensions(x, y - 40, 200, 20).build()).active = false;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Custom Sounds: " + (tempCustomEnabled ? "§aON" : "§cOFF")), b -> {
            tempCustomEnabled = !tempCustomEnabled;
            this.updateWidgets();
        }).dimensions(x, y - 15, 178, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§a\uD83D\uDDC1"), b -> {
            if (this.client != null) {
                File folder = new File(this.client.runDirectory, "resourcepacks/DeathSound-Resources/assets/deathsound/sounds");
                if (!folder.exists()) folder.mkdirs();
                Util.getOperatingSystem().open(folder);
            }
        }).dimensions(x + 180, y - 15, 20, 20).tooltip(Tooltip.of(Text.literal("Open Folder"))).build());

        int listStartY = y + 20;
        for (int i = 0; i < availableSounds.size(); i++) {
            String soundName = availableSounds.get(i);
            boolean isSelected = soundName.equals(selectedFileName);

            ButtonWidget sBtn = ButtonWidget.builder(Text.literal((isSelected ? "§a> " : "") + soundName + ".ogg"), b -> {
                selectedFileName = soundName;
                this.updateWidgets();
            }).dimensions(x, listStartY + (i * 22), 200, 20).build();

            sBtn.active = tempCustomEnabled;
            this.soundButtons.add(sBtn);
            this.addSelectableChild(sBtn);
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§cCancel"), b -> this.client.setScreen(this.parent))
                .dimensions(x, this.height - 25, 98, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§aApply"), b -> {
            ModConfig.customSoundEnabled = tempCustomEnabled;
            if (selectedFileName != null) ModConfig.customSoundFileName = selectedFileName;

            if (tempCustomEnabled && this.client != null) {
                ModConfig.lastSoundTimestamp = System.currentTimeMillis();
                CustomSoundScreen.saveSoundsJson(this.client, ModConfig.lastSoundTimestamp, selectedFileName);
                activateResourcePack();
            }

            ModConfig.save();
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(x + 102, this.height - 25, 98, 20).build());
    }

    private void activateResourcePack() {
        if (this.client == null) return;
        var manager = this.client.getResourcePackManager();
        String packId = "file/DeathSound-Resources";

        manager.scanPacks();
        Collection<String> enabledPacks = new ArrayList<>(manager.getEnabledNames());

        if (!enabledPacks.contains(packId)) {
            enabledPacks.add(packId);
            manager.setEnabledProfiles(enabledPacks);
        } else {
            this.client.reloadResources();
        }
    }

    public static void saveSoundsJson(MinecraftClient client, long ts, String name) {
        if (client == null || name == null) return;
        try {
            File folder = new File(client.runDirectory, "resourcepacks/DeathSound-Resources/assets/deathsound");
            if (!folder.exists()) folder.mkdirs();

            File file = new File(folder, "sounds.json");
            String cleanName = name.toLowerCase().replace(".ogg", "");
            String json = String.format("{\"custom_%d\":{\"sounds\":[{\"name\":\"deathsound:%s\",\"stream\":true}]}}", ts, cleanName);

            java.nio.file.Files.writeString(file.toPath(), json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        super.render(context, mouseX, mouseY, delta);

        int listTop = this.height / 2 - 80;
        int listBottom = this.height - 35;

        context.enableScissor(this.width / 2 - 110, listTop, this.width / 2 + 110, listBottom);
        for (int i = 0; i < soundButtons.size(); i++) {
            ButtonWidget btn = soundButtons.get(i);
            int baseY = (this.height / 2 - 100) + 20 + (i * 22);
            int renderY = (int) (baseY - scrollAmount);

            if (renderY + 20 > listTop && renderY < listBottom) {
                btn.setY(renderY);
                btn.render(context, mouseX, mouseY, delta);
            } else {
                btn.setY(-200);
            }
        }
        context.disableScissor();

        if (tempCustomEnabled) {
            drawScrollbar(context, listTop, listBottom);
        }
    }

    private void drawScrollbar(DrawContext context, int top, int bottom) {
        int contentHeight = soundButtons.size() * 22;
        int viewHeight = bottom - top;
        if (contentHeight <= viewHeight) return;

        int x = this.width / 2 + 105;
        int scrollbarHeight = (int) ((float) viewHeight / contentHeight * viewHeight);
        int scrollbarPos = (int) ((float) scrollAmount / (contentHeight - viewHeight) * (viewHeight - scrollbarHeight));

        context.fill(x, top, x + 2, bottom, 0x88000000);
        context.fill(x, top + scrollbarPos, x + 2, top + scrollbarPos + scrollbarHeight, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!tempCustomEnabled) return false;

        int contentHeight = soundButtons.size() * 22;
        int viewHeight = (this.height - 35) - (this.height / 2 - 80);
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = MathHelper.clamp(this.scrollAmount - amount * 15, 0, maxScroll);
        return true;
    }
}