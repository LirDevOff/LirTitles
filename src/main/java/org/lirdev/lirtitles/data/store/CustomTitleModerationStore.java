package org.lirdev.lirtitles.data.store;

import java.util.List;
import java.util.UUID;

import org.lirdev.lirtitles.data.model.CustomTitleStatus;
import org.lirdev.lirtitles.data.model.PendingModerationEntry;

public interface CustomTitleModerationStore {
    List<PendingModerationEntry> pendingModeration();

    boolean decideCustomTitle(UUID playerId, UUID titleId, CustomTitleStatus status, String reason, long decidedAt);
}
