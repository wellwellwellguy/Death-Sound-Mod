package net.wellwellwell.sound;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

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
    private boolean tempCustomSoundEnabled;

    public DeathSoundScreen(Screen parent) {
        super(Text.literal("DeathSound Settings"));
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
        this.tempCustomSoundEnabled = ModConfig.customSoundEnabled;
        this.tempUseFeet = ModConfig.useFeet;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 110;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§l Death Sound Settings §r"), b -> {})
                .dimensions(x, y - 47, 200, 20)
                .build()).active = false;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Mod: " + (tempEnabled ? "§aON" : "§cOFF")), b -> {
            tempEnabled = !tempEnabled;
            b.setMessage(Text.literal("Mod: " + (tempEnabled ? "§aON" : "§cOFF")));
        }).dimensions(x, y - 22, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Spam Protection: " + (tempSpamProtect ? "§aON" : "§cOFF")), b -> {
                    tempSpamProtect = !tempSpamProtect;
                    b.setMessage(Text.literal("Spam Protection: " + (tempSpamProtect ? "§aON" : "§cOFF")));
                }).dimensions(x, y, 200, 20)
                .tooltip(Tooltip.of(Text.literal("Enable a cooldown between sounds")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Notify List Death: " + (tempNotifyTracked ? "§aON" : "§cOFF")), b -> {
                    tempNotifyTracked = !tempNotifyTracked;
                    b.setMessage(Text.literal("Notify List Death: " + (tempNotifyTracked ? "§aON" : "§cOFF")));
                }).dimensions(x, y + 22, 200, 20)
                .tooltip(Tooltip.of(Text.literal("Get notified when someone on your list dies near you")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Death Effects: " + (tempDeathEffects ? "§aON" : "§cOFF")), b -> {
                    tempDeathEffects = !tempDeathEffects;
                    b.setMessage(Text.literal("Death Effect: " + (tempDeathEffects ? "§aON" : "§cOFF")));
                }).dimensions(x, y + 44, 200, 20)
                .tooltip(Tooltip.of(Text.literal("Toggle a Thunder Effect on death.")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Filter: §a" + tempFilterMode.toUpperCase()), b -> {
            tempFilterMode = switch (tempFilterMode.toLowerCase()) {
                case "everyone" -> "others";
                case "others" -> "list";
                default -> "everyone";
            };
            b.setMessage(Text.literal("Filter: §a" + tempFilterMode.toUpperCase()));
        }).dimensions(x, y + 66, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Unit: " + (tempUseFeet ? "§aFeet" : "§aMeters")), b -> {
            tempUseFeet = !tempUseFeet;
            b.setMessage(Text.literal("Unit: " + (tempUseFeet ? "§aFeet" : "§aMeters")));
        }).dimensions(x + 102, y + 66, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Sound: §a" + tempSelectedSound.toUpperCase()), b -> {
            tempSelectedSound = switch (tempSelectedSound.toLowerCase()) {
                case "wither" -> "thunder";
                case "thunder" -> "explosion";
                case "explosion" -> "shrieker";
                default -> "wither";
            };
            b.setMessage(Text.literal("Sound: §a" + tempSelectedSound.toUpperCase()));
        }).dimensions(x, y + 88, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§aCustom Sounds..."), b -> {
            this.client.setScreen(new CustomSoundScreen(this));
        }).dimensions(x + 102, y + 88, 98, 20).build());

        this.addDrawableChild(new DelaySlider(x, y + 110, 200, 20, tempDelayTicks, v -> tempDelayTicks = v));
        this.addDrawableChild(new VolumeSlider(x, y + 132, 200, 20, tempVolume, v -> tempVolume = v));
        this.addDrawableChild(new PitchSlider(x, y + 154, 200, 20, tempPitch, v -> tempPitch = v));
        this.addDrawableChild(new DistanceSlider(x, y + 176, 200, 20, tempMaxDistanceChunks, v -> tempMaxDistanceChunks = v));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§cReset"), b -> {
            ModConfig.resetToDefaults();
            this.client.setScreen(new DeathSoundScreen(this.parent));
        }).dimensions(x, y + 198, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§cCancel"), b -> {
            this.client.setScreen(this.parent);
        }).dimensions(x + 102, y + 198, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§aManage List ..."), b -> {
                    if (this.client != null) {
                        this.client.setScreen(new ManageListScreen(this));
                    }
                })
                .dimensions(x, y + 222, 98, 20)
                .tooltip(Tooltip.of(Text.literal("Edit the tracked players list")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§aSave & Exit"), b -> {
                    applyAndSave();
                    if (this.client != null) {
                        this.client.setScreen(this.parent);
                    }
                })
                .dimensions(x + 102, y + 222, 98, 20).build());
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        super.render(context, mouseX, mouseY, delta);
    }

    private static class VolumeSlider extends SliderWidget {
        private final java.util.function.Consumer<Float> callback;
        public VolumeSlider(int x, int y, int w, int h, float v, java.util.function.Consumer<Float> callback) {
            super(x, y, w, h, Text.literal("Volume: §b" + (int)(v * 100) + "%"), (double)v);
            this.callback = callback;
        }
        protected void updateMessage() { setMessage(Text.literal("Volume: §b" + (int)(this.value * 100) + "%")); }
        protected void applyValue() { callback.accept((float)this.value); }
    }

    private static class PitchSlider extends SliderWidget {
        private final java.util.function.Consumer<Float> callback;
        public PitchSlider(int x, int y, int w, int h, float v, java.util.function.Consumer<Float> callback) {
            super(x, y, w, h, Text.literal("Pitch: §b" + String.format(Locale.US, "%.1f", v)), (double)(v / 2.0f));
            this.callback = callback;
        }
        protected void updateMessage() {
            float p = Math.round((float)this.value * 20.0f) / 10.0f;
            setMessage(Text.literal("Pitch: §b" + String.format(Locale.US, "%.1f", p)));
        }
        protected void applyValue() { callback.accept(Math.round((float)this.value * 20.0f) / 10.0f); }
    }

    private static class DelaySlider extends SliderWidget {
        private final java.util.function.Consumer<Integer> callback;
        public DelaySlider(int x, int y, int w, int h, int t, java.util.function.Consumer<Integer> callback) {
            super(x, y, w, h, Text.literal("Delay: §b" + String.format(Locale.US, "%.2fs", t / 20.0f)), t / 100.0);
            this.callback = callback;
        }
        protected void updateMessage() {
            int ticks = (int)(this.value * 100);
            setMessage(Text.literal("Delay: §b" + String.format(Locale.US, "%.2fs", ticks / 20.0f)));
        }
        protected void applyValue() { callback.accept((int)(this.value * 100)); }
    }

    private static class DistanceSlider extends SliderWidget {
        private final java.util.function.Consumer<Integer> callback;
        public DistanceSlider(int x, int y, int w, int h, int d, java.util.function.Consumer<Integer> callback) {
            super(x, y, w, h, Text.literal("Max Distance: §b" + (d > 20 ? "Unlimited" : d) + " §rChunks"), (d - 1) / 20.0);
            this.callback = callback;
        }
        protected void updateMessage() {
            int d = 1 + (int)(this.value * 20.0);
            setMessage(Text.literal("Max Distance: §b" + (d > 20 ? "Unlimited" : d) + " §rChunks"));
        }
        protected void applyValue() { callback.accept(1 + (int)Math.round(this.value * 20.0)); }
    }
}