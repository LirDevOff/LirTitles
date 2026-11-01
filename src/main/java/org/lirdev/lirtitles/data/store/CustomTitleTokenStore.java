package org.lirdev.lirtitles.data.store;

import java.util.UUID;

public interface CustomTitleTokenStore {
    int customTitleTokens(UUID playerId);

    void addCustomTitleTokens(UUID playerId, int amount);

    boolean consumeCustomTitleToken(UUID playerId);
}
