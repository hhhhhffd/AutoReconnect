package com.autoreconnect;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

public final class ReconnectConfig {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("autoreconnect.properties");
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("Auto Reconnect");

    private boolean enabled = true;
    private int delay = RetryPolicy.DEFAULT_DELAY_SECONDS;
    private boolean dirty;

    ReconnectConfig() {
        if (!Files.isRegularFile(PATH)) return;
        try (var reader = Files.newBufferedReader(PATH)) {
            Properties props = new Properties();
            props.load(reader);
            enabled = !"false".equalsIgnoreCase(props.getProperty("autoReconnect", "true").trim());
            String value = props.getProperty("delaySeconds", "").trim();
            if (!value.isEmpty()) {
                try {
                    delay = RetryPolicy.clampDelay(Long.parseLong(value));
                } catch (NumberFormatException ignored) {
                    LOGGER.warn("Invalid reconnect delay; using {}s", delay);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.warn("Could not read reconnect settings", e);
        }
    }

    public boolean enabled() { return enabled; }
    public int delay() { return delay; }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        dirty = true;
        save();
    }

    public void setDelay(int delay) {
        int clamped = RetryPolicy.clampDelay(delay);
        if (this.delay == clamped) return;
        this.delay = clamped;
        dirty = true;
    }

    public void save() {
        if (!dirty) return;
        Path temp = null;
        try {
            Files.createDirectories(PATH.getParent());
            temp = Files.createTempFile(PATH.getParent(), "autoreconnect-", ".tmp");
            Files.writeString(temp, "# Auto Reconnect\nautoReconnect=" + enabled + "\ndelaySeconds=" + delay + "\n");
            try {
                Files.move(temp, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            LOGGER.warn("Could not save reconnect settings", e);
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }
}
