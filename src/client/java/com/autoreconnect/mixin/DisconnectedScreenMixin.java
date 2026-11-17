package com.autoreconnect.mixin;

import com.autoreconnect.Reconnect;
import com.autoreconnect.RetryPolicy;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
abstract class DisconnectedScreenMixin extends Screen {
    @Shadow @Final private Screen parent;
    @Shadow @Final private DisconnectionDetails details;
    @Shadow @Final private LinearLayout layout;

    @Unique private Button autoreconnect$autoButton;
    @Unique private Button autoreconnect$applyDelay;
    @Unique private EditBox autoreconnect$delayField;
    @Unique private RetryPolicy.Cause autoreconnect$cause = RetryPolicy.Cause.MANUAL_ONLY;
    @Unique private long autoreconnect$due = Long.MIN_VALUE;

    protected DisconnectedScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;allowsMultiplayer()Z"))
    private void autoreconnect$addControls(CallbackInfo ci) {
        if (!Reconnect.hasServer() || minecraft == null || !minecraft.allowsMultiplayer()) return;

        Reconnect.POLICY.disconnected(Reconnect.now());
        autoreconnect$cause = Reconnect.classify(details.reason(), details.report().isPresent());

        LinearLayout row = LinearLayout.horizontal().spacing(4);
        row.addChild(Button.builder(Component.literal("Reconnect"), button -> {
            autoreconnect$due = Long.MIN_VALUE;
            Reconnect.connect(minecraft, parent, false);
        }).width(98).tooltip(Tooltip.create(Component.literal("Reconnect to the same server now."))).build());

        autoreconnect$autoButton = row.addChild(Button.builder(Component.empty(), button -> {
            Reconnect.config().setEnabled(!Reconnect.config().enabled());
            autoreconnect$schedule();
        }).width(98).tooltip(Tooltip.create(Component.literal("Toggle automatic reconnect. Turning it off cancels the timer."))).build());

        layout.addChild(row);
        LinearLayout delayRow = LinearLayout.horizontal().spacing(4);
        autoreconnect$delayField = new EditBox(font, 0, 0, 84, 20, null, Component.literal("Reconnect delay in seconds"));
        autoreconnect$delayField.setMaxLength(10);
        autoreconnect$delayField.setFilter(value -> value.chars().allMatch(ch -> ch >= '0' && ch <= '9'));
        autoreconnect$delayField.setHint(Component.literal("Seconds"));
        autoreconnect$delayField.setValue(Integer.toString(Reconnect.config().delay()));
        delayRow.addChild(autoreconnect$delayField);
        autoreconnect$applyDelay = delayRow.addChild(Button.builder(Component.literal("Set delay (s)"),
                button -> autoreconnect$applyDelay()).width(112)
                .tooltip(Tooltip.create(Component.literal("First retry delay: 3 to 2147483647 seconds."))).build());
        autoreconnect$delayField.setResponder(value -> autoreconnect$applyDelay.active = !value.isEmpty());
        layout.addChild(delayRow);
        autoreconnect$schedule();
    }

    @Unique private void autoreconnect$applyDelay() {
        String value = autoreconnect$delayField.getValue();
        if (value.isEmpty()) return;
        int seconds = RetryPolicy.clampDelay(Long.parseLong(value));
        autoreconnect$delayField.setValue(Integer.toString(seconds));
        boolean changed = Reconnect.config().delay() != seconds;
        Reconnect.config().setDelay(seconds);
        Reconnect.config().save();
        if (changed && autoreconnect$due != Long.MIN_VALUE) autoreconnect$schedule();
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (autoreconnect$delayField != null && autoreconnect$delayField.isFocused() && (key == 257 || key == 335)) {
            autoreconnect$applyDelay();
            return true;
        }
        return super.keyPressed(event);
    }

    @Unique private void autoreconnect$schedule() {
        autoreconnect$due = Reconnect.config().enabled() && Reconnect.POLICY.canSchedule(autoreconnect$cause)
                ? Reconnect.now() + Reconnect.POLICY.nextDelaySeconds(Reconnect.config().delay()) * 1000L
                : Long.MIN_VALUE;
        autoreconnect$updateLabel(Reconnect.now());
    }

    @Unique private void autoreconnect$updateLabel(long now) {
        if (autoreconnect$autoButton == null) return;
        String label;
        if (!Reconnect.config().enabled()) {
            label = "Auto: OFF";
        } else if (autoreconnect$due == Long.MIN_VALUE) {
            label = "Auto: ON";
        } else {
            long seconds = Math.max(0L, (autoreconnect$due - now + 999L) / 1000L);
            label = seconds < 10_000 ? "Auto: ON (" + seconds + "s)" : "Auto: " + seconds + "s";
        }
        if (!autoreconnect$autoButton.getMessage().getString().equals(label)) {
            autoreconnect$autoButton.setMessage(Component.literal(label));
        }
    }

    @Override public void tick() {
        super.tick();
        if (autoreconnect$autoButton == null || minecraft == null || minecraft.screen != (Object)this) return;
        long now = Reconnect.now();
        if (autoreconnect$due != Long.MIN_VALUE && now >= autoreconnect$due) {
            autoreconnect$due = Long.MIN_VALUE;
            if (Reconnect.config().enabled() && Reconnect.hasServer() && minecraft.allowsMultiplayer()
                    && Reconnect.POLICY.startAutomatic(autoreconnect$cause)) {
                Reconnect.connect(minecraft, parent, true);
                return;
            }
        }
        autoreconnect$updateLabel(now);
    }

    @Override public void removed() {
        super.removed();
        autoreconnect$due = Long.MIN_VALUE;
        if (autoreconnect$autoButton != null) Reconnect.config().save();
    }
}
