package org.lirdev.lirtitles.data.model;

import java.util.List;
import java.util.Set;

public record PlayerTitleData(
        Set<String> ownedTitles,
        String selectedTitle,
        String selectedFont,
        List<CustomTitle> customTitles,
        int customTitleTokens) {
    public PlayerTitleData {
        ownedTitles = Set.copyOf(ownedTitles);
        customTitles = List.copyOf(customTitles);
    }
}
