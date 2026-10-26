package net.wellwellwell.sound;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;

import java.util.Locale;

public class DeathSoundScreen extends Screen {

    private final Screen parent;

    private boolean tempEnabled;
    private boolean tempSpamProtect;
    private String tempSelectedSound;
    private int tempDelayTicks;
    private float tempVolume;
    private float tempPitch;
    private String tempFilterMode;
    private int tempMaxDistanceChunks;
    private boolean tempNotifyTracked;
    private boolean tempDeathEffects;
    private boolean tempUseFeet;

    public DeathSoundScreen(Screen parent) {
        super(Component.literal("DeathSound Settings"));
        this.parent = parent;

        this.tempEnabled = ModConfig.enabled;
        this.tempSpamProtect = ModConfig.spamProtect;
        this.tempSelectedSound = (ModConfig.selectedSound == null) ? "wither" : ModConfig.selectedSound;
        this.tempDelayTicks = ModConfig.delayTicks;
        this.tempVolume = ModConfig.volume;
        this.tempPitch = ModConfig.pitch;
        this.tempFilterMode = (ModConfig.filterMode == null) ? "everyone" : ModConfig.filterMode;
        this.tempMaxDistanceChunks = ModConfig.maxDistanceChunks;
        this.tempNotifyTracked = ModConfig.notifyTrackedDeath;
        this.tempDeathEffects = ModConfig.deathEffects;
        this.tempUseFeet = ModConfig.useFeet;
    }


    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 110;

        this.addRenderableWidget(Button.builder(Component.literal("§l Death Sound Settings §r"), b -> {})
                .bounds(x, y - 47, 200, 20)
                .build()).active = false;

        this.addRenderableWidget(Button.builder(Component.literal("Mod: " + (tempEnabled ? "§aON" : "§cOFF")), b -> {
            tempEnabled = !tempEnabled;
            b.setMessage(Component.literal("Mod: " + (tempEnabled ? "§aON" : "§cOFF")));
        }).bounds(x, y - 22, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Spam Protection: " + (tempSpamProtect ? "§aON" : "§cOFF")), b -> {
                    tempSpamProtect = !tempSpamProtect;
                    b.setMessage(Component.literal("Spam Protection: " + (tempSpamProtect ? "§aON" : "§cOFF")));
                }).bounds(x, y, 200, 20)
                .tooltip(Tooltip.create(Component.literal("Enable a cooldown between sounds"))) // Tooltip.of -> Tooltip.create
                .build());



        this.addRenderableWidget(Button.builder(Component.literal("Notify List Death: " + (tempNotifyTracked ? "§aON" : "§cOFF")), b -> {
                    tempNotifyTracked = !tempNotifyTracked;
                    b.setMessage(Component.literal("Notify List Death: " + (tempNotifyTracked ? "§aON" : "§cOFF")));
                }).bounds(x, y + 22, 200, 20)
                .tooltip(Tooltip.create(Component.literal("Get notified when someone on your list dies near you")))
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Death Effects: " + (tempDeathEffects ? "§aON" : "§cOFF")), b -> {
                    tempDeathEffects = !tempDeathEffects;
                    b.setMessage(Component.literal("Death Effect: " + (tempDeathEffects ? "§aON" : "§cOFF")));
                }).bounds(x, y + 44, 200, 20)
                .tooltip(Tooltip.create(Component.literal("Toggle a Thunder Effect on death.")))
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Filter: §a" + tempFilterMode.toUpperCase()), b -> {
            tempFilterMode = switch (tempFilterMode.toLowerCase()) {
                case "everyone" -> "others";
                case "others" -> "list";
                default -> "everyone";
            };
            b.setMessage(Component.literal("Filter: §a" + tempFilterMode.toUpperCase()));
        }).bounds(x, y + 66, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Unit: " + (tempUseFeet ? "§aFeet" : "§aMeters")), b -> {
            tempUseFeet = !tempUseFeet;
            b.setMessage(Component.literal("Unit: " + (tempUseFeet ? "§aFeet" : "§aMeters")));
        }).bounds(x + 102, y + 66, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Sound: §a" + tempSelectedSound.toUpperCase()), b -> {
            tempSelectedSound = switch (tempSelectedSound.toLowerCase()) {
                case "wither" -> "thunder";
                case "thunder" -> "explosion";
                case "explosion" -> "shrieker";
                default -> "wither";
            };
            b.setMessage(Component.literal("Sound: §a" + tempSelectedSound.toUpperCase()));
        }).bounds(x, y + 88, 98, 20).build());

        this.addRenderableWidget(new DelaySlider(x, y + 110, 200, 20, tempDelayTicks, v -> tempDelayTicks = v));
        this.addRenderableWidget(new VolumeSlider(x, y + 132, 200, 20, tempVolume, v -> tempVolume = v));
        this.addRenderableWidget(new PitchSlider(x, y + 154, 200, 20, tempPitch, v -> tempPitch = v));
        this.addRenderableWidget(new DistanceSlider(x, y + 176, 200, 20, tempMaxDistanceChunks, v -> tempMaxDistanceChunks = v));

        this.addRenderableWidget(Button.builder(Component.literal("§cReset"), b -> {
            ModConfig.resetToDefaults();
            if (this.minecraft != null) this.minecraft.setScreen(new DeathSoundScreen(this.parent));
        }).bounds(x, y + 198, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§cCancel"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }).bounds(x + 102, y + 198, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§aManage List ..."), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ManageListScreen(this));
                    }
                })
                .bounds(x, y + 222, 98, 20)
                .tooltip(Tooltip.create(Component.literal("Edit the tracked players list")))
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("§aSave & Exit"), b -> {
                    applyAndSave();
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parent);
                    }
                })
                .bounds(x + 102, y + 222, 98, 20).build());
    }

    private void applyAndSave() {
        ModConfig.enabled = tempEnabled;
        ModConfig.spamProtect = tempSpamProtect;
        ModConfig.selectedSound = tempSelectedSound;
        ModConfig.delayTicks = tempDelayTicks;
        ModConfig.volume = tempVolume;
        ModConfig.pitch = tempPitch;
        ModConfig.filterMode = tempFilterMode;
        ModConfig.maxDistanceChunks = tempMaxDistanceChunks;
        ModConfig.notifyTrackedDeath = tempNotifyTracked;
        ModConfig.deathEffects = tempDeathEffects;
        ModConfig.useFeet = tempUseFeet;
        ModConfig.save();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);

        super.render(context, mouseX, mouseY, delta);
    }

    private static class VolumeSlider extends AbstractSliderButton {
        private final java.util.function.Consumer<Float> callback;

        public VolumeSlider(int x, int y, int w, int h, float v, java.util.function.Consumer<Float> callback) {
            super(x, y, w, h, Component.literal("Volume: §b" + (int)(v * 100) + "%"), (double)v);
            this.callback = callback;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Volume: §b" + (int)(this.value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            callback.accept((float)this.value);
        }
    }

    private static class PitchSlider extends AbstractSliderButton {
        private final java.util.function.Consumer<Float> callback;

        public PitchSlider(int x, int y, int w, int h, float v, java.util.function.Consumer<Float> callback) {
            super(x, y, w, h, Component.literal("Pitch: §b" + String.format(Locale.US, "%.1f", v)), (double)(v / 2.0f));
            this.callback = callback;
        }

        @Override
        protected void updateMessage() {
            float p = Math.round((float)this.value * 20.0f) / 10.0f;
            setMessage(Component.literal("Pitch: §b" + String.format(Locale.US, "%.1f", p)));
        }

        @Override
        protected void applyValue() {
            callback.accept(Math.round((float)this.value * 20.0f) / 10.0f);
        }
    }

    private static class DelaySlider extends AbstractSliderButton {
        private final java.util.function.Consumer<Integer> callback;

        public DelaySlider(int x, int y, int w, int h, int t, java.util.function.Consumer<Integer> callback) {
            super(x, y, w, h, Component.literal("Delay: §b" + String.format(Locale.US, "%.2fs", t / 20.0f)), t / 100.0);
            this.callback = callback;
        }

        @Override
        protected void updateMessage() {
            int ticks = (int)(this.value * 100);
            setMessage(Component.literal("Delay: §b" + String.format(Locale.US, "%.2fs", ticks / 20.0f)));
        }

        @Override
        protected void applyValue() {
            callback.accept((int)(this.value * 100));
        }
    }

    private static class DistanceSlider extends AbstractSliderButton {
        private final java.util.function.Consumer<Integer> callback;

        public DistanceSlider(int x, int y, int w, int h, int d, java.util.function.Consumer<Integer> callback) {
            super(x, y, w, h, Component.literal("Max Distance: §b" + (d > 20 ? "Unlimited" : d) + " §rChunks"), (d - 1) / 20.0);
            this.callback = callback;
        }

        @Override
        protected void updateMessage() {
            int d = 1 + (int)(this.value * 20.0);
            setMessage(Component.literal("Max Distance: §b" + (d > 20 ? "Unlimited" : d) + " §rChunks"));
        }

        @Override
        protected void applyValue() {
            callback.accept(1 + (int)Math.round(this.value * 20.0));
        }
    }
}