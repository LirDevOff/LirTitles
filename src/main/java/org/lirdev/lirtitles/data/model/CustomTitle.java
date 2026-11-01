package org.lirdev.lirtitles.data.model;

import java.util.Objects;
import java.util.UUID;

public record CustomTitle(UUID id, int numericId, String text, int slot, CustomTitleStatus status,
                          String rejectionReason, long decidedAt) {
    private static final String KEY_PREFIX = "custom:";

    public CustomTitle {
        Objects.requireNonNull(id, "id");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Custom title text must not be blank");
        }
        Objects.requireNonNull(status, "status");
        rejectionReason = rejectionReason == null ? "" : rejectionReason;
    }

    public CustomTitle withStatus(CustomTitleStatus newStatus, String reason, long timestamp) {
        return new CustomTitle(id, numericId, text, slot, newStatus, reason, timestamp);
    }

    public CustomTitle withText(String newText, CustomTitleStatus newStatus) {
        return new CustomTitle(id, numericId, newText, slot, newStatus, "", 0);
    }

    public String key() {
        return keyFor(id);
    }

    public static String keyFor(UUID id) {
        return KEY_PREFIX + Objects.requireNonNull(id, "id");
    }

    public static boolean isCustomKey(String key) {
        return key != null && key.startsWith(KEY_PREFIX);
    }

    public static UUID idFromKey(String key) {
        if (!isCustomKey(key)) {
            throw new IllegalArgumentException("Not a custom title key: " + key);
        }
        return UUID.fromString(key.substring(KEY_PREFIX.length()));
    }
}
