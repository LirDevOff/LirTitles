package org.lirdev.lirtitles.menu.menus.shared;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.kyori.adventure.text.Component;

public record MenuItemTokens(Map<String, Component> inline, Map<String, List<Component>> blocks) {
    public static final MenuItemTokens NONE = new MenuItemTokens(Map.of(), Map.of());
    public static final MenuItemTokens BLANK = titled(Component.empty(), Component.empty());

    public MenuItemTokens {
        inline = Map.copyOf(inline);
        blocks = Map.copyOf(blocks);
    }

    public static MenuItemTokens of(Map<String, Component> inline) {
        return new MenuItemTokens(inline, Map.of());
    }

    public static MenuItemTokens titled(Component title, Component category) {
        return of(Map.of("title", title, "category", category));
    }

    public static MenuItemTokens forCategory(Component category) {
        return titled(Component.empty(), category);
    }

    public MenuItemTokens withBlock(String name, List<Component> lines) {
        Map<String, List<Component>> combined = new LinkedHashMap<>(blocks);
        combined.put(name, lines.isEmpty() ? List.of(Component.empty()) : List.copyOf(lines));
        return new MenuItemTokens(inline, combined);
    }
}
