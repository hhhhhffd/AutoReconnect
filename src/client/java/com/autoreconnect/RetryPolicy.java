package com.autoreconnect;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Client-only retry policy with no networking or UI dependencies. */
public final class RetryPolicy {
    public enum Cause { NETWORK, ORDINARY_KICK, MANUAL_ONLY }

    public static final int MIN_DELAY_SECONDS = 3;
    public static final int MAX_DELAY_SECONDS = Integer.MAX_VALUE;
    public static final int DEFAULT_DELAY_SECONDS = 5;
    public static final int MAX_AUTOMATIC_TRIES = 3;
    private static final long STABLE_JOIN_MS = 30_000L;

    private static final Set<String> NETWORK_KEYS = Set.of(
            "disconnect.endofstream", "disconnect.timeout", "disconnect.closed",
            "connect.unknownhost", "disconnect.lost", "disconnect.server_shutdown",
            "multiplayer.disconnect.server_shutdown");

    private static final Pattern ADMIN_REASON = Pattern.compile(
            "(?iu)(?:\\b(?:ban|banned|banning|blacklisted|blacklist|whitelist(?:ed)?|allowlist(?:ed)?"
            + "|suspended|suspension|blocked|denied|authentication|authenticate|unauthenticated|unauthorized|"
            + "unverified|incompatible|forbidden|unauthorised|invalid session|not logged in|"
            + "not authenticated|not permitted|no permission|secure profile|required resource pack|"
            + "missing profile public key|duplicate login|protocol mismatch|version mismatch)\\b"
            + "|забан|белый список|авторизац|аутентификац|доступ запрещ|время бана|аккаунт заблокирован)");

    private static final Pattern NET_ERROR = Pattern.compile(
            "(?iu)(?:connection (?:reset|refused|timed? ?out|closed|aborted)|connect timed? ?out|"
            + "broken pipe|network is unreachable|no route to host|unknown host|unresolved address|"
            + "read timed? ?out|end of stream|timed out|timeout|socketexception|connectexception|"
            + "readtimeoutexception|unknownhostexception|connection lost)");

    private boolean remembered;
    private boolean joinedBefore;
    private boolean thisAttemptJoined;
    private long joinedAt;
    private int attempts;

    public void clear() {
        remembered = false;
        joinedBefore = false;
        thisAttemptJoined = false;
        attempts = 0;
    }

    public void begin() {
        clear();
        remembered = true;
    }

    public void joined(long now) {
        if (remembered) {
            joinedBefore = true;
            thisAttemptJoined = true;
            joinedAt = now;
        }
    }

    public void disconnected(long now) {
        if (thisAttemptJoined && now - joinedAt >= STABLE_JOIN_MS) {
            attempts = 0;
        }
        thisAttemptJoined = false;
    }

    public boolean canSchedule(Cause cause) {
        return remembered && joinedBefore && attempts < MAX_AUTOMATIC_TRIES
                && (cause == Cause.NETWORK || (cause == Cause.ORDINARY_KICK && attempts == 0));
    }

    public int nextDelaySeconds(int requested) {
        return clampDelay((long)clampDelay(requested) * (1L << Math.min(attempts, MAX_AUTOMATIC_TRIES - 1)));
    }

    public boolean startAutomatic(Cause cause) {
        if (!canSchedule(cause)) return false;
        ++attempts;
        return true;
    }

    public static int clampDelay(long delay) {
        return Math.clamp(delay, MIN_DELAY_SECONDS, MAX_DELAY_SECONDS);
    }

    public static Cause classify(String translationKey, String englishText, String argText, boolean report) {
        String key = translationKey.toLowerCase(Locale.ROOT);
        String text = englishText + " " + argText;
        if (ADMIN_REASON.matcher(text).find()
                || key.contains("bann") || key.contains("whitelist") || key.contains("blacklist")
                || key.contains("loginfailed") || key.contains("auth") || key.contains("invalid")
                || key.contains("incompatible") || key.contains("duplicate") || key.contains("resource_pack")
                || key.contains("secure_profile") || key.contains("unverified") || key.contains("outdated")
                || key.contains("chat_validation") || key.contains("profile")) {
            return Cause.MANUAL_ONLY;
        }
        if (NETWORK_KEYS.contains(key) || NET_ERROR.matcher(text).find()
                || (key.equals("disconnect.genericreason") && text.contains("EndOfStream"))) {
            return Cause.NETWORK;
        }
        if (report || key.equals("disconnect.genericreason")) return Cause.MANUAL_ONLY;
        return Cause.ORDINARY_KICK;
    }
}
