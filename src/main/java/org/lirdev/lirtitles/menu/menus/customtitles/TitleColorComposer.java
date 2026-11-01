package org.lirdev.lirtitles.menu.menus.customtitles;

import java.util.List;

public final class TitleColorComposer {
    private TitleColorComposer() {
    }

    public static String applyColors(String plainText, List<String> filledColors) {
        if (filledColors.isEmpty() || plainText.isBlank()) {
            return plainText;
        }
        if (filledColors.size() == 1) {
            return "<" + filledColors.get(0) + ">" + plainText;
        }
        return "<gradient:" + String.join(":", filledColors) + ">" + plainText + "</gradient>";
    }
}
