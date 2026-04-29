package id.naturalsmp.naturalinteraction.webpanel;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token session store — LuckPerms-style auth.
 * No MySQL needed. Tokens are generated from /ni connect and expire after 30 minutes.
 *
 * Token format: 6 characters [A-Za-z] — e.g. "AbCdEf"
 */
public class TokenSession {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int TOKEN_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    // In-memory store
    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public record SessionData(
            String token,
            String playerName,
            UUID playerUUID,
            long createdAt,
            long expiresAt
    ) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * Generate a new 6-char token for the given player.
     * Expires in 30 minutes by default.
     */
    public static SessionData createSession(String playerName, UUID playerUUID, long durationMs) {
        // Remove any existing session for this player
        sessions.values().removeIf(s -> s.playerUUID().equals(playerUUID));

        String token = generateToken();
        // Ensure uniqueness
        while (sessions.containsKey(token)) {
            token = generateToken();
        }

        long now = System.currentTimeMillis();
        SessionData session = new SessionData(token, playerName, playerUUID, now, now + durationMs);
        sessions.put(token, session);
        return session;
    }

    /**
     * Verify a token. Returns null if invalid or expired.
     */
    public static SessionData verify(String token) {
        if (token == null || token.isBlank()) return null;
        SessionData session = sessions.get(token);
        if (session == null) return null;
        if (session.isExpired()) {
            sessions.remove(token);
            return null;
        }
        return session;
    }

    /**
     * Cleanup all expired tokens.
     */
    public static int cleanExpired() {
        int before = sessions.size();
        sessions.values().removeIf(SessionData::isExpired);
        return before - sessions.size();
    }

    /**
     * Revoke a specific token.
     */
    public static void revoke(String token) {
        sessions.remove(token);
    }

    /**
     * Get active session count.
     */
    public static int activeCount() {
        return (int) sessions.values().stream().filter(s -> !s.isExpired()).count();
    }

    private static String generateToken() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
