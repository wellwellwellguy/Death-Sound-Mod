package com.wellwellwell.sound.Screen;

import com.wellwellwell.sound.CustomSoundPack;
import com.wellwellwell.sound.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CustomSoundScreen extends Screen {
    private final Screen parent;
    private boolean tempCustomEnabled;
    private String selectedFileName;

    private final List<String> availableSounds = new ArrayList<>();
    private final List<Button> soundButtons = new ArrayList<>();

    private double scrollAmount = 0;
    private final int entryHeight = 22;
    private final int itemWidth = 280;
    private boolean isDraggingScrollbar = false;

    private final int listTop = 60;

    public CustomSoundScreen(Screen parent) {
        super(Component.translatable("deathsound.screen.custom.title"));
        this.parent = parent;
        this.tempCustomEnabled = ModConfig.customSoundEnabled;
        this.selectedFileName = ModConfig.customSoundFileName;
        loadSoundFiles();
    }

    private void loadSoundFiles() {
        availableSounds.clear();
        File folder = new File(Minecraft.getInstance().gameDirectory, "resourcepacks/DeathSound-Resources/assets/deathsound/sounds");
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".ogg"));
            if (files != null) {
                for (File f : files) availableSounds.add(f.getName().replace(".ogg", ""));
            }
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.soundButtons.clear();

        int centerX = this.width / 2;
        int startX = centerX - (itemWidth / 2);
        int gap = 2;

        int utilityWidth = 20;
        int toggleWidth = 236;

        this.addRenderableWidget(Button.builder(Component.literal("§b\uD83D\uDDD8"), b -> {
                    CustomSoundPack.syncSounds();
                    this.loadSoundFiles();
                    this.init();
                }).bounds(startX, 20, utilityWidth, 20)
                .tooltip(Tooltip.create(Component.literal("Sync Sounds from Config")))
                .build());

        Component statusText = tempCustomEnabled ?
                Component.translatable("deathsound.status.on").withStyle(ChatFormatting.GREEN) :
                Component.translatable("deathsound.status.off").withStyle(ChatFormatting.RED);

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.custom.toggle").append(statusText), b -> {
            tempCustomEnabled = !tempCustomEnabled;
            this.init();
        }).bounds(startX + utilityWidth + gap, 20, toggleWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§a\uD83D\uDDC1"), b -> {
                    File folder = new File(this.minecraft.gameDirectory, "config/deathsound/sounds");
                    if (!folder.exists()) folder.mkdirs();
                    com.mojang.blaze3d.Blaze3D.openUri(folder.toURI());
                }).bounds(startX + utilityWidth + gap + toggleWidth + gap, 20, utilityWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.custom.open_folder")))
                .build());


        int buttonWidth = 139;

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.settings.cancel"), button -> {
            if (this.minecraft != null) this.minecraft.setScreenAndShow(this.parent);
        }).bounds(startX, this.height - 28, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.settings.save"), button -> {
            ModConfig.customSoundEnabled = tempCustomEnabled;
            if (selectedFileName != null) {
                ModConfig.customSoundFileName = selectedFileName;
            }
            if (tempCustomEnabled && selectedFileName != null) {
                saveSoundsJson(this.minecraft, selectedFileName);
                if (this.minecraft != null) {
                    this.minecraft.reloadResourcePacks();
                }
            }
            ModConfig.save();
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(this.parent);
            }
        }).bounds(startX + buttonWidth + gap, this.height - 28, buttonWidth, 20).build());

        this.rebuildSoundList();
        this.clampScroll();
    }

    private void rebuildSoundList() {
        for (Button btn : soundButtons) {
            this.removeWidget(btn);
        }
        soundButtons.clear();

        for (String soundName : availableSounds) {
            Button button = Button.builder(
                            Component.literal(soundName + ".ogg"),
                            btn -> this.selectedFileName = soundName
                    )
                    .bounds(this.width / 2 - itemWidth / 2, 0, itemWidth, 20)
                    .build();

            button.active = tempCustomEnabled;
            button.visible = true;
            this.soundButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int dynamicListBottom = this.height - 40;

        guiGraphics.fill(0, listTop, this.width, dynamicListBottom, 0x50000000);

        int startY = listTop;
        for (int i = 0; i < soundButtons.size(); i++) {
            Button btn = soundButtons.get(i);
            String soundName = availableSounds.get(i);
            int renderY = (int) (startY + (i * entryHeight) - scrollAmount);

            btn.setY(renderY);

            boolean isSelected = soundName.equals(this.selectedFileName);

            if (isSelected && tempCustomEnabled) {
                btn.setAlpha(1.0f);
            } else if (btn.isHovered() && tempCustomEnabled) {
                btn.setAlpha(0.80f);
            } else {
                btn.setAlpha(0.75f);
            }

            if (renderY >= listTop && (renderY + btn.getHeight()) <= dynamicListBottom) {
                btn.visible = true;
            } else {
                btn.visible = false;
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.enableScissor(0, listTop, this.width, dynamicListBottom);
        for (int i = 0; i < soundButtons.size(); i++) {
            Button btn = soundButtons.get(i);
            if (btn.visible && availableSounds.get(i).equals(this.selectedFileName) && tempCustomEnabled) {
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
        int contentHeight = availableSounds.size() * entryHeight;
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
        int contentHeight = availableSounds.size() * entryHeight;

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
        int contentHeight = availableSounds.size() * entryHeight;
        int viewHeight = dynamicListBottom - listTop;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * 12, 0, maxScroll);
        return true;
    }

    private void clampScroll() {
        int dynamicListBottom = this.height - 40;
        int contentHeight = availableSounds.size() * entryHeight;
        int viewHeight = dynamicListBottom - listTop;
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        if (this.scrollAmount > maxScroll) {
            this.scrollAmount = maxScroll;
        }
    }

    public static void saveSoundsJson(Minecraft client, String name) {
        try {
            File assetsDir = new File(client.gameDirectory, "resourcepacks/DeathSound-Resources/assets/deathsound/");
            if (!assetsDir.exists()) assetsDir.mkdirs();

            File jsonFile = new File(assetsDir, "sounds.json");
            String fileNameOnly = name.toLowerCase().replace(".ogg", "");

            String json = "{\n" +
                    "  \"custom_ogg\": {\n" +
                    "    \"sounds\": [\n" +
                    "      {\n" +
                    "        \"name\": \"deathsound:" + fileNameOnly + "\",\n" +
                    "        \"stream\": true\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }\n" +
                    "}";

            java.nio.file.Files.writeString(jsonFile.toPath(), json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}