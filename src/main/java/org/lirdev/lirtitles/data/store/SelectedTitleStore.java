package org.lirdev.lirtitles.data.store;

import java.util.UUID;

public interface SelectedTitleStore {
    String selected(UUID playerId);

    boolean select(UUID playerId, String titleId);

    boolean select(UUID playerId, String titleId, String fontId);
}
