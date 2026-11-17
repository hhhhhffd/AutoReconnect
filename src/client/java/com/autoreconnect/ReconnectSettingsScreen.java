package com.autoreconnect;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ReconnectSettingsScreen extends Screen {
    private static final int ROW_WIDTH = 200;
    private final Screen parent;
    private boolean draftEnabled;
    private EditBox delayInput;
    private Button toggle;
    private Button done;
    private Button cancel;

    ReconnectSettingsScreen(Screen parent) {
        super(Component.literal("Auto Reconnect Settings"));
        this.parent = parent;
        draftEnabled = Reconnect.config().enabled();
    }

    @Override protected void init() {
        String delay = delayInput != null ? delayInput.getValue() : Integer.toString(Reconnect.config().delay());
        toggle = addRenderableWidget(Button.builder(toggleLabel(), button -> {
            draftEnabled = !draftEnabled;
            button.setMessage(toggleLabel());
        }).size(ROW_WIDTH, 20).build());
        toggle.setTooltip(Tooltip.create(Component.literal("Reconnect automatically after a server kick or network loss.")));

        delayInput = addRenderableWidget(new EditBox(font, 0, 0, ROW_WIDTH, 20, null,
                Component.literal("First retry delay in seconds")));
        delayInput.setMaxLength(10);
        delayInput.setFilter(value -> value.chars().allMatch(ch -> ch >= '0' && ch <= '9'));
        delayInput.setHint(Component.literal("Seconds"));

        done = addRenderableWidget(Button.builder(Component.literal("Done"), button -> save()).size(98, 20).build());
        cancel = addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose()).size(98, 20).build());
        delayInput.setResponder(value -> done.active = !value.isEmpty());
        delayInput.setValue(delay);
        done.active = !delay.isEmpty();
        repositionElements();
    }

    private Component toggleLabel() {
        return Component.literal(draftEnabled ? "Auto Reconnect: ON" : "Auto Reconnect: OFF");
    }

    private int contentTop() {
        return Math.max(44, height / 2 - 50);
    }

    @Override protected void repositionElements() {
        if (toggle == null) return;
        int x = (width - ROW_WIDTH) / 2;
        int y = contentTop();
        toggle.setPosition(x, y);
        delayInput.setPosition(x, y + 47);
        done.setPosition(x, y + 98);
        cancel.setPosition(x + 102, y + 98);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = (width - ROW_WIDTH) / 2;
        int y = contentTop();
        graphics.drawCenteredString(font, getTitle(), width / 2, y - 24, 0xFFFFFFFF);
        graphics.drawString(font, "First retry delay (seconds)", x, y + 34, 0xFFFFFFFF);
        graphics.drawString(font, "3–2147483647 seconds · Default: 5", x, y + 73, 0xFFAAAAAA);
    }

    private void save() {
        String value = delayInput.getValue();
        if (value.isEmpty()) return;
        ReconnectConfig config = Reconnect.config();
        config.setDelay(RetryPolicy.clampDelay(Long.parseLong(value)));
        config.setEnabled(draftEnabled);
        config.save();
        onClose();
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
