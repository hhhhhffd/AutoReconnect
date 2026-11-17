package com.autoreconnect.mixin;

import com.autoreconnect.Reconnect;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void autoreconnect$joined(ClientboundLoginPacket packet, CallbackInfo ci) {
        if (Reconnect.hasServer()) Reconnect.POLICY.joined(Reconnect.now());
    }
}
