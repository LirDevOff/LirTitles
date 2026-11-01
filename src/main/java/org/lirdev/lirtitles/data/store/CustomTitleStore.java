package org.lirdev.lirtitles.data.store;

import java.util.List;
import java.util.UUID;

import org.lirdev.lirtitles.data.model.CustomTitle;
import org.lirdev.lirtitles.data.model.CustomTitleStatus;

public interface CustomTitleStore {
    List<CustomTitle> customTitles(UUID playerId);

    int maxNumericId();

    CustomTitle createCustomTitle(UUID playerId, String text, int slot, CustomTitleStatus status);

    default CustomTitle createCustomTitle(UUID playerId, CustomTitle title) {
        return createCustomTitle(playerId, title.text(), title.slot(), title.status());
    }

    default CustomTitle createCustomTitle(UUID playerId, CustomTitle title, String playerName) {
        return createCustomTitle(playerId, title);
    }

    boolean updateCustomTitle(UUID playerId, UUID titleId, String text, CustomTitleStatus status);

    boolean deleteCustomTitle(UUID playerId, UUID titleId);

    CustomTitle customTitle(UUID playerId, UUID titleId);

    boolean selectCustomTitle(UUID playerId, UUID titleId);
}
