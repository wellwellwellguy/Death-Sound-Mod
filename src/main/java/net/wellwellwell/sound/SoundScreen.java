package net.wellwellwell.sound;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.util.ArrayList;
import java.util.List;

public class SoundScreen extends Screen {
    private final Screen parent;
    private double scrollAmount = 0;

    private final List<String> installedSounds = List.of(
            "classic_hurt", "wither", "thunder", "explosion",
            "shrieker", "warden_heartbeat", "totem", "anvil", "ghast",
            "enderdragon", "elder_guardian", "amethyst", "villager_no", "bell",
            "creeper", "level_up", "goat_scream", "plinth", "enderman"
    );

    private final List<ButtonWidget> soundButtons = new ArrayList<>();
    private final String selectedSoundKey;
    private String tempSelectedSound;

    private final int entryHeight = 22;
    private final int itemWidth = 280;
    private boolean isDraggingScrollbar = false;

    public SoundScreen(Screen parent) {
        super(Text.translatable("deathsound.screen.sounds.title"));
        this.parent = parent;

        this.selectedSoundKey = ModConfig.selectedSound;
        this.tempSelectedSound = this.selectedSoundKey;
    }

    @Override
    protected void init() {
        this.clearChildren();
        this.soundButtons.clear();

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.manage_list.cancel"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 155, this.height - 28, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("deathsound.screen.manage_list.save"), button -> {
            ModConfig.selectedSound = SoundScreen.this.tempSelectedSound;
            ModConfig.save();
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 + 5, this.height - 28, 150, 20).build());

        this.buildSoundList();
        this.clampScroll();
    }

    private void buildSoundList() {
        for (ButtonWidget btn : soundButtons) {
            this.remove(btn);
        }
        soundButtons.clear();

        for (String soundId : installedSounds) {
            String displayName = soundId.replace("_", " ").toUpperCase();

            ButtonWidget button = ButtonWidget.builder(
                            Text.literal(displayName),
                            b -> this.tempSelectedSound = soundId
                    )
                    .dimensions(this.width / 2 - itemWidth / 2, 0, itemWidth, 20)
                    .build();

            this.soundButtons.add(button);
            this.addSelectableChild(button);
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

        if (mouseX >= scrollbarLeft && mouseX <= scrollbarRight && mouseY >= listTop && mouseY <= listBottom) {
            this.isDraggingScrollbar = true;
            this.updateScrollFromMouse(mouseY);
            return true;
        }

        if (mouseY >= listTop && mouseY <= listBottom) {
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
        if (button == 0 && this.isDraggingScrollbar) {
            this.updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void updateScrollFromMouse(double mouseY) {
        int listTop = 60;
        int listBottom = this.height - 40;
        int viewHeight = listBottom - listTop;
        int contentHeight = installedSounds.size() * entryHeight;

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
            String soundId = installedSounds.get(i);
            int renderY = (int) (startY + (i * entryHeight) - scrollAmount);

            btn.setY(renderY);

            if (renderY + 20 > listTop && renderY < listBottom) {
                boolean isSelected = soundId.equals(this.tempSelectedSound);

                if (isSelected) {
                    btn.setAlpha(1f);
                } else if (btn.isHovered()) {
                    btn.setAlpha(0.80f);
                } else {
                    btn.setAlpha(0.75f);
                }

                btn.render(context, mouseX, mouseY, delta);

                if (isSelected) {
                    int border = 0xFFFFFFFF;
                    context.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + 1, border);
                    context.fill(btn.getX(), btn.getY() + btn.getHeight() - 1, btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), border);
                    context.fill(btn.getX(), btn.getY(), btn.getX() + 1, btn.getY() + btn.getHeight(), border);
                    context.fill(btn.getX() + btn.getWidth() - 1, btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), border);
                }
            }
        }

        context.disableScissor();

        context.fillGradient(0, listTop, this.width, listTop + 4, 0x50000000, 0x00000000);
        context.fillGradient(0, listBottom - 4, this.width, listBottom, 0x00000000, 0x50000000);

        context.fill(0, listTop - 2, this.width, listTop - 1, 0xFF000000);
        context.fill(0, listTop - 1, this.width, listTop,     0xFF555555);
        context.fill(0, listBottom,     this.width, listBottom + 1, 0xFF000000);
        context.fill(0, listBottom + 1, this.width, listBottom + 2, 0xFF555555);

        this.drawScrollbar(context, listTop, listBottom);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("deathsound.screen.sounds.select_below"), this.width / 2, 25, 0xFFAAAAAA);
    }

    private void drawScrollbar(DrawContext context, int top, int bottom) {
        int contentHeight = installedSounds.size() * entryHeight;
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
        int contentHeight = installedSounds.size() * entryHeight;
        int viewHeight = this.height - 100;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        this.scrollAmount = MathHelper.clamp(this.scrollAmount - verticalAmount * 12, 0, maxScroll);
        return true;
    }

    private void clampScroll() {
        int contentHeight = installedSounds.size() * entryHeight;
        int viewHeight = this.height - 100;
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        if (this.scrollAmount > maxScroll) {
            this.scrollAmount = maxScroll;
        }
    }
}