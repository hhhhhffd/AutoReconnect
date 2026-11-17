package com.autoreconnect.mixin;

import com.autoreconnect.Reconnect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "disconnectFromWorld", at = @At("HEAD"))
    private void autoreconnect$intentionalDisconnect(Component reason, CallbackInfo ci) {
        Reconnect.clear();
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void autoreconnect$returnedToMenu(Screen screen, CallbackInfo ci) {
        Minecraft client = (Minecraft)(Object)this;
        if (client.screen instanceof TitleScreen || client.screen instanceof JoinMultiplayerScreen) {
            Reconnect.clear();
        }
    }
}
