package net.wellwellwell.sound;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
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
        super(Text.translatable("deathsound.screen.settings.title"));
        this.parent = parent;

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
        int y = this.height / 2 - 85;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.settings.header"), b -> {})
                .dimensions(x, y - 47, fullWidth, 20)
                .build()).active = false;

        this.addDrawableChild(ButtonWidget.builder(getModEnabledText(), b -> {
            tempEnabled = !tempEnabled;
            b.setMessage(getModEnabledText());
        }).dimensions(x, y - 22, fullWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(getSpamProtectText(), b -> {
                    tempSpamProtect = !tempSpamProtect;
                    b.setMessage(getSpamProtectText());
                }).dimensions(x, y, fullWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.spam_protect.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(getNotifyTrackedText(), b -> {
                    tempNotifyTracked = !tempNotifyTracked;
                    b.setMessage(getNotifyTrackedText());
                }).dimensions(x, y + 22, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.notify_list.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(getMonitorPopsText(), b -> {
                    tempMonitorPops = !tempMonitorPops;
                    b.setMessage(getMonitorPopsText());
                }).dimensions(x, y + 44, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.monitorPops.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(getMonitorDeathsText(), b -> {
                    tempMonitorDeaths = !tempMonitorDeaths;
                    b.setMessage(getMonitorDeathsText());
                }).dimensions(x + halfWidth + gap, y + 44, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.monitorDeaths.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(getUnitSystemText(), b -> {
                    tempUseFeet = !tempUseFeet;
                    b.setMessage(getUnitSystemText());
                }).dimensions(x + halfWidth + gap, y + 22, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.unit.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(getDeathEffectsText(), b -> {
                    if (this.client != null) {
                        this.client.setScreen(new DeathEffectsScreen(this));
                    }
                }).dimensions(x + halfWidth + gap, y + 66, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.effects.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(getFilterText(), b -> {
                    tempFilterMode = switch (tempFilterMode.toLowerCase()) {
                        case "everyone" -> "others";
                        case "others" -> "list";
                        default -> "everyone";
                    };
                    b.setMessage(getFilterText());
                }).dimensions(x, y + 66, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.filter.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.settings.open_sound_menu"), b -> {
                    if (this.client != null) {
                        this.client.setScreen(new SoundScreen(this));
                    }
                })
                .dimensions(x, y + 88, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.sound.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.settings.custom_sounds"), b -> {
                    CustomSoundPack.syncSounds();
                    assert this.client != null;
                    this.client.setScreen(new CustomSoundScreen(this));
                }).dimensions(x + halfWidth + gap, y + 88, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.custom_sounds.tooltip")))
                .build());

        this.addDrawableChild(new VolumeSlider(x, y + 110, halfWidth, 20, tempVolume, v -> tempVolume = v));
        this.addDrawableChild(new DelaySlider(x + halfWidth + gap, y + 110, halfWidth, 20, tempDelayTicks, v -> tempDelayTicks = v));

        this.addDrawableChild(new DistanceSlider(x, y + 132, halfWidth, 20, tempMaxDistanceChunks, v -> tempMaxDistanceChunks = v));
        this.addDrawableChild(new PitchSlider(x + halfWidth + gap, y + 132, halfWidth, 20, tempPitch, v -> tempPitch = v));

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.settings.reset"), b -> {
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
                    this.init();
                    assert this.client != null;
                    this.client.setScreen(new DeathSoundScreen(this.parent));
                }).dimensions(x, y + 156, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.reset.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.settings.cancel"), b -> {
                    assert this.client != null;
                    this.client.setScreen(this.parent);
                })
                .dimensions(x + halfWidth + gap, y + 156, halfWidth, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.settings.manage_list"), b -> {
                    assert this.client != null;
                    this.client.setScreen(new ManageListScreen(this));
                }).dimensions(x, y + 180, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathsound.screen.settings.manage_list.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.settings.save"), b -> {
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
            assert this.client != null;
            this.client.setScreen(this.parent);
        }).dimensions(x + halfWidth + gap, y + 180, halfWidth, 20).build());

        int buttonWidth = 120;
        int buttonHeight = 20;
        int xPos = this.width - buttonWidth - 10;
        int yPos = this.height - buttonHeight - 10;

        this.addDrawableChild(ButtonWidget.builder(getMenuButtonText(), b -> {
                    tempShowMenuButton = !tempShowMenuButton;
                    b.setMessage(getMenuButtonText());
                })
                .dimensions(xPos, yPos, buttonWidth, buttonHeight)
                .build());
    }

    private Text getModEnabledText() {
        return Text.translatable("deathsound.screen.settings.mod_status",
                tempEnabled ? Text.translatable("deathsound.status.on")
                        : Text.translatable("deathsound.status.off"));
    }

    private Text getSpamProtectText() {
        return Text.translatable("deathsound.screen.settings.spam_protect",
                tempSpamProtect ? Text.translatable("deathsound.status.on")
                        : Text.translatable("deathsound.status.off"));
    }

    private Text getNotifyTrackedText() {
        return Text.translatable("deathsound.screen.settings.notify_list",
                tempNotifyTracked ? Text.translatable("deathsound.status.on")
                        : Text.translatable("deathsound.status.off"));
    }

    private Text getDeathEffectsText() {
        return Text.translatable("deathsound.screen.settings.effects");
    }

    private Text getFilterText() {
        Text modeText = Text.translatable("deathsound.filter.mode." + tempFilterMode.toLowerCase());
        return Text.translatable("deathsound.screen.settings.filter", modeText);
    }

    private Text getUnitSystemText() {
        return Text.translatable("deathsound.screen.settings.unit",
                tempUseFeet ? Text.translatable("deathsound.unit.screen.feets")
                        : Text.translatable("deathsound.unit.screen.meters"));
    }

    private Text getMonitorPopsText() {
        return Text.translatable("deathsound.screen.settings.monitorPops",
                tempMonitorPops ? Text.translatable("deathsound.status.on")
                        : Text.translatable("deathsound.status.off"));
    }

    private Text getMenuButtonText() {
        return Text.translatable("deathsound.screen.menu_button.title",
                tempShowMenuButton
                        ? Text.translatable("deathsound.screen.menu_button.shown")
                        : Text.translatable("deathsound.screen.menu_button.hidden"));
    }

    private Text getMonitorDeathsText() {
        return Text.translatable("deathsound.screen.settings.monitorDeaths",
                tempMonitorDeaths ? Text.translatable("deathsound.status.on")
                        : Text.translatable("deathsound.status.off"));
    }

    private static class VolumeSlider extends SliderWidget {
        private final java.util.function.Consumer<Float> c;
        public VolumeSlider(int x, int y, int w, int h, float v, java.util.function.Consumer<Float> c) {
            super(x, y, w, h, Text.empty(), (double)v);
            this.c = c;
            updateMessage();
        }
        protected void updateMessage() {
            Text amountWithPercent = Text.literal((int)(this.value * 100) + "%");
            setMessage(Text.translatable("deathsound.screen.settings.volume", amountWithPercent));
        }
        protected void applyValue() { c.accept((float)this.value); }
    }

    private static class PitchSlider extends SliderWidget {
        private final java.util.function.Consumer<Float> c;
        public PitchSlider(int x, int y, int w, int h, float v, java.util.function.Consumer<Float> c) {
            super(x, y, w, h, Text.empty(), (double)(v / 2.0f));
            this.c = c;
            updateMessage();
        }
        protected void updateMessage() {
            float p = (float)this.value * 2.0f;
            p = Math.round(p * 10.0f) / 10.0f;
            if (p <= 0.0f) {
                setMessage(Text.translatable("deathsound.screen.settings.pitch_off"));
            } else {
                Text amount = Text.literal(String.format(Locale.US, "%.1f", p));
                setMessage(Text.translatable("deathsound.screen.settings.pitch", amount));
            }
        }
        protected void applyValue() {
            float p = (float)this.value * 2.0f;
            p = Math.round(p * 10.0f) / 10.0f;
            c.accept(p);
        }
    }

    private static class DelaySlider extends SliderWidget {
        private final java.util.function.Consumer<Integer> c;
        public DelaySlider(int x, int y, int w, int h, int t, java.util.function.Consumer<Integer> c) {
            super(x, y, w, h, Text.empty(), t / 100.0);
            this.c = c;
            updateMessage();
        }
        protected void updateMessage() {
            float s = (float)Math.round(this.value * 100.0) / 20.0f;
            Text amount = Text.literal(String.format(Locale.US, "%.2fs", s));
            setMessage(Text.translatable("deathsound.screen.settings.delay", amount));
        }
        protected void applyValue() { c.accept((int)(Math.round(this.value * 20.0) * 5)); }
    }

    private static class DistanceSlider extends SliderWidget {
        private final java.util.function.Consumer<Integer> c;

        public DistanceSlider(int x, int y, int w, int h, int d, java.util.function.Consumer<Integer> c) {
            super(x, y, w, h, Text.empty(), (MathHelper.clamp(d, 1, 32) - 1) / 31.0);
            this.c = c;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int d = 1 + (int) Math.round(this.value * 31.0);

            if (d >= 32) {
                Text amount = Text.translatable("deathsound.screen.settings.distance.unlimited_value");
                setMessage(Text.translatable("deathsound.screen.settings.distance_unlimited", amount));
            } else {
                Text amount = Text.literal(String.valueOf(d));
                setMessage(Text.translatable("deathsound.screen.settings.distance", amount));
            }
        }

        @Override
        protected void applyValue() {
            c.accept(1 + (int) Math.round(this.value * 31.0));
        }
    }
}