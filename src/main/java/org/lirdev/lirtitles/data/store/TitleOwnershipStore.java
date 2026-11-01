package org.lirdev.lirtitles.data.store;

import java.util.Set;
import java.util.UUID;

public interface TitleOwnershipStore {
    boolean owns(UUID playerId, String titleId);

    Set<String> owned(UUID playerId);

    boolean give(UUID playerId, String titleId);
}
