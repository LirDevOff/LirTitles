package org.lirdev.lirtitles.menu.menus.customtitles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CustomTitleDraft(UUID id, String text, Set<String> formats, List<String> colors) {
    public CustomTitleDraft {
        colors = List.copyOf(colors);
    }

    public static CustomTitleDraft blank(int colorSlotCount) {
        return new CustomTitleDraft(null, "", Set.of(), emptyColors(colorSlotCount));
    }

    public CustomTitleDraft withText(String value) {
        return new CustomTitleDraft(id, value, formats, colors);
    }

    public CustomTitleDraft withFormats(Set<String> value) {
        return new CustomTitleDraft(id, text, value, colors);
    }

    public CustomTitleDraft withColor(int slot, String hex) {
        List<String> updated = new ArrayList<>(colors);
        updated.set(slot, hex);
        return new CustomTitleDraft(id, text, formats, List.copyOf(updated));
    }

    private static List<String> emptyColors(int count) {
        List<String> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            values.add("");
        }
        return List.copyOf(values);
    }
}
