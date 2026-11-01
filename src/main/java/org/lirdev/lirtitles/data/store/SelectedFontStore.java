package org.lirdev.lirtitles.data.store;

import java.util.UUID;

public interface SelectedFontStore {
    String selectedFont(UUID playerId);

    void selectFont(UUID playerId, String fontId);
}
