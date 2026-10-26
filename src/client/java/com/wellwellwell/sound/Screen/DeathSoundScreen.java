package com.wellwellwell.sound.Screen;

import com.wellwellwell.sound.CustomSoundPack;
import com.wellwellwell.sound.ModConfig;
import com.wellwellwell.sound.SoundScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

public class DeathSoundScreen extends Screen {

    private final Screen parent;

    private boolean tempEnabled;
    private boolean tempSpamProtect;
    private int tempDelayTicks;
    private float tempVolume;
    private float tempPitch;
    private String tempFilterMode;
    private int tempMaxDistanceChunks;
    private boolean tempNotifyTracked;
    private boolean tempUseFeet;
    private boolean tempMonitorPops;
    private boolean tempShowMenuButton;
    private boolean tempMonitorDeaths;

    public DeathSoundScreen(Screen parent) {
        super(Component.translatable("deathsound.screen.settings.title"));
        this.parent = parent;
        loadTempValues();
    }

    private void loadTempValues() {
        this.tempEnabled = ModConfig.enabled;
        this.tempSpamProtect = ModConfig.spamProtect;
        this.tempDelayTicks = ModConfig.delayTicks;
        this.tempVolume = ModConfig.volume;
        this.tempPitch = ModConfig.pitch;
        this.tempFilterMode = (ModConfig.filterMode == null) ? "everyone" : ModConfig.filterMode;
        this.tempMaxDistanceChunks = ModConfig.maxDistanceChunks;
        this.tempNotifyTracked = ModConfig.notifyTrackedDeath;
        this.tempUseFeet = ModConfig.useFeet;
        this.tempMonitorPops = ModConfig.monitorPops;
        this.tempShowMenuButton = ModConfig.showMenuButton;
        this.tempMonitorDeaths = ModConfig.monitorDeaths;
    }

    @Override
    protected void init() {
        int fullWidth = 250;
        int halfWidth = 124;
        int gap = 2;

        int x = this.width / 2 - (fullWidth / 2);
        int y = this.height / 2 - 73;

        // Header
        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.settings.header"), b -> {})
                .bounds(x, y - 47, fullWidth, 20).build())
                .active = false;

        // Mod Status
        this.addRenderableWidget(Button.builder(getModEnabledText(), b -> {
            tempEnabled = !tempEnabled;
            b.setMessage(getModEnabledText());
        }).bounds(x, y - 22, fullWidth, 20).build());

        // Spam Protection
        this.addRenderableWidget(Button.builder(getSpamProtectText(), b -> {
                    tempSpamProtect = !tempSpamProtect;
                    b.setMessage(getSpamProtectText());
                }).bounds(x, y, fullWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.spam_protect.tooltip")))
                .build());

        // Notify Tracked
        this.addRenderableWidget(Button.builder(getNotifyTrackedText(), b -> {
                    tempNotifyTracked = !tempNotifyTracked;
                    b.setMessage(getNotifyTrackedText());
                }).bounds(x, y + 22, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.notify_list.tooltip")))
                .build());

        // Unit System
        this.addRenderableWidget(Button.builder(getUnitSystemText(), b -> {
                    tempUseFeet = !tempUseFeet;
                    b.setMessage(getUnitSystemText());
                }).bounds(x + halfWidth + gap, y + 22, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.unit.tooltip")))
                .build());

        // Monitor Pops
        this.addRenderableWidget(Button.builder(getMonitorPopsText(), b -> {
                    tempMonitorPops = !tempMonitorPops;
                    b.setMessage(getMonitorPopsText());
                }).bounds(x, y + 44, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.monitorPops.tooltip")))
                .build());

        // Monitor Deaths
        this.addRenderableWidget(Button.builder(getMonitorDeathsText(), b -> {
                    tempMonitorDeaths = !tempMonitorDeaths;
                    b.setMessage(getMonitorDeathsText());
                }).bounds(x + halfWidth + gap, y + 44, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.monitorDeaths.tooltip")))
                .build());

        // Filter Mode
        this.addRenderableWidget(Button.builder(getFilterText(), b -> {
                    tempFilterMode = switch (tempFilterMode.toLowerCase()) {
                        case "everyone" -> "others";
                        case "others" -> "list";
                        default -> "everyone";
                    };
                    b.setMessage(getFilterText());
                }).bounds(x, y + 66, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.filter.tooltip")))
                .build());

        // Death Effects Screen
        this.addRenderableWidget(Button.builder(getDeathEffectsText(), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreenAndShow(new DeathEffectsScreen(this));
                    }
                }).bounds(x + halfWidth + gap, y + 66, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.effects.tooltip")))
                .build());

        // Open Sound Menu
        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.settings.open_sound_menu"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreenAndShow(new SoundScreen(this));
                    }
                })
                .bounds(x, y + 88, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.sound.tooltip")))
                .build());

        // Custom Sounds Screen
        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.settings.custom_sounds"), b -> {
                    CustomSoundPack.syncSounds();
                    if (this.minecraft != null) {
                        this.minecraft.setScreenAndShow(new CustomSoundScreen(this));
                    }
                }).bounds(x + halfWidth + gap, y + 88, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.custom_sounds.tooltip")))
                .build());

        // Sliders (Nebeneinander)
        this.addRenderableWidget(new DelaySlider(x, y + 110, halfWidth, 20, tempDelayTicks, v -> tempDelayTicks = v));
        this.addRenderableWidget(new VolumeSlider(x + halfWidth + gap, y + 110, halfWidth, 20, tempVolume, v -> tempVolume = v));
        this.addRenderableWidget(new PitchSlider(x, y + 132, halfWidth, 20, tempPitch, v -> tempPitch = v));
        this.addRenderableWidget(new DistanceSlider(x + halfWidth + gap, y + 132, halfWidth, 20, tempMaxDistanceChunks, v -> tempMaxDistanceChunks = v));

        // Reset Button
        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.settings.reset"), b -> {
                    System.out.println("Reset all DeathSound settings.");
                    tempEnabled = true;
                    tempDelayTicks = 0;
                    tempVolume = 0.5f;
                    tempPitch = 1.0f;
                    tempSpamProtect = true;
                    tempFilterMode = "everyone";
                    tempMaxDistanceChunks = 32;
                    tempNotifyTracked = false;
                    tempUseFeet = false;
                    tempMonitorPops = false;
                    tempMonitorDeaths = false;
                    tempShowMenuButton = true;
                    this.updateWidgets();
                    if (this.minecraft != null) {
                        this.minecraft.setScreenAndShow(new DeathSoundScreen(this.parent));
                    }
                }).bounds(x, y + 156, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.reset.tooltip")))
                .build());

        // Cancel Button
        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.settings.cancel"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreenAndShow(this.parent);
                    }
                })
                .bounds(x + halfWidth + gap, y + 156, halfWidth, 20)
                .build());

        // Manage List Button
        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.settings.manage_list"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreenAndShow(new ManageListScreen(this));
                    }
                }).bounds(x, y + 178, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("deathsound.screen.settings.manage_list.tooltip")))
                .build());

        // Save Button
        this.addRenderableWidget(Button.builder(Component.translatable("deathsound.screen.settings.save"), b -> {
            saveConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(this.parent);
            }
        }).bounds(x + halfWidth + gap, y + 178, halfWidth, 20).build());

        int buttonWidth = 120;
        int buttonHeight = 20;
        int xPos = this.width - buttonWidth - 10;
        int yPos = this.height - buttonHeight - 10;

        this.addRenderableWidget(Button.builder(getMenuButtonText(), b -> {
                    tempShowMenuButton = !tempShowMenuButton;
                    b.setMessage(getMenuButtonText());
                })
                .bounds(xPos, yPos, buttonWidth, buttonHeight)
                .build());
    }

    private void saveConfig() {
        ModConfig.enabled = tempEnabled;
        ModConfig.spamProtect = tempSpamProtect;
        ModConfig.delayTicks = tempDelayTicks;
        ModConfig.volume = tempVolume;
        ModConfig.pitch = tempPitch;
        ModConfig.filterMode = tempFilterMode;
        ModConfig.maxDistanceChunks = tempMaxDistanceChunks;
        ModConfig.notifyTrackedDeath = tempNotifyTracked;
        ModConfig.useFeet = tempUseFeet;
        ModConfig.monitorPops = tempMonitorPops;
        ModConfig.monitorDeaths = tempMonitorDeaths;
        ModConfig.showMenuButton = tempShowMenuButton;
        ModConfig.save();
    }

    private void updateWidgets() {
        this.clearWidgets();
        this.init();
    }

    private Component getStatusText(boolean val) {
        return Component.translatable(val ? "deathsound.status.on" : "deathsound.status.off");
    }

    private Component getModEnabledText() { return Component.translatable("deathsound.screen.settings.mod_status", getStatusText(tempEnabled)); }
    private Component getSpamProtectText() { return Component.translatable("deathsound.screen.settings.spam_protect", getStatusText(tempSpamProtect)); }
    private Component getNotifyTrackedText() { return Component.translatable("deathsound.screen.settings.notify_list", getStatusText(tempNotifyTracked)); }
    private Component getDeathEffectsText() { return Component.translatable("deathsound.screen.settings.effects"); }
    private Component getMonitorPopsText() { return Component.translatable("deathsound.screen.settings.monitorPops", getStatusText(tempMonitorPops)); }
    private Component getMonitorDeathsText() { return Component.translatable("deathsound.screen.settings.monitorDeaths", getStatusText(tempMonitorDeaths)); }
    private Component getUnitSystemText() { return Component.translatable("deathsound.screen.settings.unit", Component.translatable(tempUseFeet ? "deathsound.unit.screen.feets" : "deathsound.unit.screen.meters")); }

    private Component getFilterText() {
        return Component.translatable("deathsound.screen.settings.filter", Component.translatable("deathsound.filter.mode." + tempFilterMode.toLowerCase()));
    }

    private Component getMenuButtonText() {
        return Component.translatable("deathsound.screen.menu_button.title",
                tempShowMenuButton ? Component.translatable("deathsound.screen.menu_button.shown")
                        : Component.translatable("deathsound.screen.menu_button.hidden"));
    }

    private static class VolumeSlider extends AbstractSliderButton {
        private final java.util.function.Consumer<Float> c;
        public VolumeSlider(int x, int y, int w, int h, float v, java.util.function.Consumer<Float> c) { super(x, y, w, h, Component.empty(), (double)v); this.c = c; updateMessage(); }
        @Override protected void updateMessage() {
            Component val = Component.literal((int)(this.value * 100) + "%").withStyle(ChatFormatting.AQUA);
            setMessage(Component.translatable("deathsound.screen.settings.volume", val));
        }
        @Override protected void applyValue() { c.accept((float)this.value); }
    }

    private static class PitchSlider extends AbstractSliderButton {
        private final java.util.function.Consumer<Float> c;
        public PitchSlider(int x, int y, int w, int h, float v, java.util.function.Consumer<Float> c) { super(x, y, w, h, Component.empty(), (double)(v / 2.0f)); this.c = c; updateMessage(); }
        @Override protected void updateMessage() {
            float p = (float)this.value * 2.0f; p = Math.round(p * 10.0f) / 10.0f;
            if (p <= 0.0f) { setMessage(Component.translatable("deathsound.screen.settings.pitch_off")); }
            else {
                Component val = Component.literal(String.format(Locale.US, "%.1f", p)).withStyle(ChatFormatting.AQUA);
                setMessage(Component.translatable("deathsound.screen.settings.pitch", val));
            }
        }
        @Override protected void applyValue() {
            float p = (float)this.value * 2.0f; p = Math.round(p * 10.0f) / 10.0f;
            c.accept(p);
        }
    }

    private static class DelaySlider extends AbstractSliderButton {
        private final java.util.function.Consumer<Integer> c;
        public DelaySlider(int x, int y, int w, int h, int t, java.util.function.Consumer<Integer> c) { super(x, y, w, h, Component.empty(), t / 100.0); this.c = c; updateMessage(); }
        @Override protected void updateMessage() {
            float s = (float)Math.round(this.value * 100.0) / 20.0f;
            Component val = Component.literal(String.format(Locale.US, "%.2fs", s)).withStyle(ChatFormatting.AQUA);
            setMessage(Component.translatable("deathsound.screen.settings.delay", val));
        }
        @Override protected void applyValue() { c.accept((int)(Math.round(this.value * 20.0) * 5)); }
    }

    private static class DistanceSlider extends AbstractSliderButton {
        private final java.util.function.Consumer<Integer> c;

        public DistanceSlider(int x, int y, int w, int h, int d, java.util.function.Consumer<Integer> c) {
            super(x, y, w, h, Component.empty(), (Mth.clamp(d, 1, 32) - 1) / 31.0);
            this.c = c;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int d = 1 + (int)Math.round(this.value * 31.0);

            if (d >= 32) {
                setMessage(Component.translatable("deathsound.screen.settings.distance_unlimited"));
            } else {
                Component val = Component.literal(String.valueOf(d)).withStyle(ChatFormatting.AQUA);
                setMessage(Component.translatable("deathsound.screen.settings.distance", val));
            }
        }

        @Override
        protected void applyValue() {
            c.accept(1 + (int)Math.round(this.value * 31.0));
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) { super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick); }
}