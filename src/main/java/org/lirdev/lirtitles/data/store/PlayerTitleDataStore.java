package org.lirdev.lirtitles.data.store;

import java.util.UUID;

import org.lirdev.lirtitles.data.model.PlayerTitleData;

public interface PlayerTitleDataStore {
    PlayerTitleData loadPlayerData(UUID playerId);
}
