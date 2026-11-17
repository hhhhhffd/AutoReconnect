package com.autoreconnect.mixin;

import com.autoreconnect.Reconnect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
abstract class ConnectScreenMixin {
    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void autoreconnect$remember(Screen parent, Minecraft client, ServerAddress address, ServerData data,
                                               boolean quickPlay, TransferState transfer, CallbackInfo ci) {
        if (!(client.screen instanceof ConnectScreen)) {
            Reconnect.onVanillaConnect(address, data, transfer != null);
        }
    }
}
