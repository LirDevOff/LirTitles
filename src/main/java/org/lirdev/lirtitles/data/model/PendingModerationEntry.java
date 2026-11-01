package org.lirdev.lirtitles.data.model;

import java.util.Objects;
import java.util.UUID;

public record PendingModerationEntry(UUID playerId, String playerName, CustomTitle title) {
    public PendingModerationEntry {
        Objects.requireNonNull(playerId, "playerId");
        playerName = playerName == null || playerName.isBlank() ? playerId.toString().substring(0, 8) : playerName;
        Objects.requireNonNull(title, "title");
    }

    public PendingModerationEntry(UUID playerId, CustomTitle title) {
        this(playerId, null, title);
    }
}
