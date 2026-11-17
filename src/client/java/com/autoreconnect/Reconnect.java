package com.autoreconnect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** All state changes occur on the client thread; the server address is kept only in memory. */
public final class Reconnect {
    public static final RetryPolicy POLICY = new RetryPolicy();
    private static ServerAddress address;
    private static ServerData server;
    private static boolean automaticInvocation;
    private static ReconnectConfig config;

    public static ReconnectConfig config() {
        if (config == null) config = new ReconnectConfig();
        return config;
    }

    public static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    public static void clear() {
        POLICY.clear();
        address = null;
        server = null;
    }

    public static void onVanillaConnect(ServerAddress target, ServerData info, boolean transfer) {
        if (automaticInvocation) return;
        clear();
        if (!transfer && target != null && info != null && !info.isRealm()) {
            address = target;
            server = info;
            POLICY.begin();
        }
    }

    public static boolean hasServer() {
        return server != null && address != null;
    }

    public static void connect(Minecraft minecraft, Screen returnScreen, boolean automatic) {
        if (!hasServer() || !minecraft.allowsMultiplayer()) return;
        automaticInvocation = automatic;
        try {
            ConnectScreen.startConnecting(returnScreen, minecraft, address, server, false, null);
        } finally {
            automaticInvocation = false;
        }
    }

    public static RetryPolicy.Cause classify(Component reason, boolean report) {
        String key = "";
        StringBuilder args = new StringBuilder();
        if (reason.getContents() instanceof TranslatableContents translate) {
            key = translate.getKey();
            for (Object arg : translate.getArgs()) {
                args.append(arg instanceof Component c ? c.getString(2048) : String.valueOf(arg)).append(' ');
            }
        }
        return RetryPolicy.classify(key, reason.getString(4096), args.toString(), report);
    }
}
