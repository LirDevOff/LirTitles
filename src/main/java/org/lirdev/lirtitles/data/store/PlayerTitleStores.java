package org.lirdev.lirtitles.data.store;

public record PlayerTitleStores(
        TitleOwnershipStore ownership,
        SelectedTitleStore selection,
        SelectedFontStore fonts,
        CustomTitleStore customTitles,
        CustomTitleModerationStore customTitleModeration,
        CustomTitleTokenStore customTitleTokens,
        PlayerTitleDataStore playerData,
        int maxCustomTitleId,
        AutoCloseable closer
) {
}
