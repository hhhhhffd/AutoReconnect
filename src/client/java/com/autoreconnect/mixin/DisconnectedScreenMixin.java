package com.autoreconnect.mixin;

import com.autoreconnect.Reconnect;
import com.autoreconnect.RetryPolicy;
import net.minecraft.client.gui.components.Button;
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
    @Unique private RetryPolicy.Cause autoreconnect$cause = RetryPolicy.Cause.MANUAL_ONLY;
    @Unique private long autoreconnect$due = Long.MIN_VALUE;

    protected DisconnectedScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V"))
    private void autoreconnect$addControls(CallbackInfo ci) {
        if (!Reconnect.hasServer() || minecraft == null || !minecraft.allowsMultiplayer()) return;

        // At this point vanilla has placed Back after any optional report controls,
        // but has not performed layout or registered the widgets yet.
        Button[] back = {null};
        layout.visitWidgets(widget -> {
            if (widget instanceof Button button) back[0] = button;
        });
        if (back[0] == null) return;
        int buttonWidth = back[0].getWidth();
        int buttonHeight = back[0].getHeight();

        Reconnect.POLICY.disconnected(Reconnect.now());
        autoreconnect$cause = Reconnect.classify(details.reason(), details.report().isPresent());

        layout.addChild(Button.builder(Component.literal("Reconnect"), button -> {
            autoreconnect$due = Long.MIN_VALUE;
            Reconnect.connect(minecraft, parent, false);
        }).size(buttonWidth, buttonHeight)
          .tooltip(Tooltip.create(Component.literal("Reconnect to the same server now."))).build());

        autoreconnect$autoButton = layout.addChild(Button.builder(Component.empty(), button -> {
            Reconnect.config().setEnabled(!Reconnect.config().enabled());
            autoreconnect$schedule();
        }).size(buttonWidth, buttonHeight)
          .tooltip(Tooltip.create(Component.literal("Toggle automatic reconnect. Turning it off cancels the timer."))).build());

        autoreconnect$schedule();
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
            label = "Auto Reconnect: OFF";
        } else if (autoreconnect$due == Long.MIN_VALUE) {
            label = "Auto Reconnect: ON";
        } else {
            long seconds = Math.max(0L, (autoreconnect$due - now + 999L) / 1000L);
            label = "Auto Reconnect: ON (" + seconds + "s)";
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
    }
}
