package org.lirdev.lirtitles.menu.menus.customtitles;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

public final class TitleColorParser {
    private TitleColorParser() {
    }

    public static List<String> extractColors(Component root, int maxColors) {
        List<String> colors = new ArrayList<>();
        collect(root, null, colors, maxColors);
        return List.copyOf(colors);
    }

    private static void collect(Component component, TextColor inherited, List<String> colors, int maxColors) {
        if (colors.size() >= maxColors) {
            return;
        }
        TextColor effective = component.color() != null ? component.color() : inherited;
        if (effective != null && component instanceof TextComponent text && !text.content().isEmpty()) {
            String hex = effective.asHexString();
            if (!colors.contains(hex)) {
                colors.add(hex);
            }
        }
        for (Component child : component.children()) {
            if (colors.size() >= maxColors) {
                return;
            }
            collect(child, effective, colors, maxColors);
        }
    }
}
