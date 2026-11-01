package org.lirdev.lirtitles.menu.menus.customtitles;

import java.util.List;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.CustomTitleEditorMenuConfiguration;

public final class CustomTitleMarkup {
    private final LirTitles plugin;

    public CustomTitleMarkup(LirTitles plugin) {
        this.plugin = plugin;
    }

    public String render(CustomTitleDraft draft) {
        StringBuilder opening = new StringBuilder();
        StringBuilder closing = new StringBuilder();
        for (String tag : formatOrder()) {
            if (draft.formats().contains(tag)) {
                opening.append(tag);
                closing.insert(0, closingTag(tag));
            }
        }
        return opening + withColors(draft) + closing;
    }

    private String withColors(CustomTitleDraft draft) {
        List<String> filled = draft.colors().stream().filter(color -> !color.isBlank()).toList();
        return TitleColorComposer.applyColors(plugin.colorizer().escape(draft.text()), filled);
    }

    private List<String> formatOrder() {
        return plugin.configuration().customTitleEditorMenu().formats().stream()
                .map(CustomTitleEditorMenuConfiguration.FormatButton::tag)
                .toList();
    }

    private static String closingTag(String tag) {
        return "</" + tag.substring(1);
    }
}
