package org.lirdev.lirtitles.data.model;

import java.util.UUID;

public interface PlayerTitleSnapshot {
    String cachedSelectedTitle(UUID playerId);

    String cachedSelectedFont(UUID playerId);

    CustomTitle cachedCustomTitle(UUID playerId, UUID titleId);
}
