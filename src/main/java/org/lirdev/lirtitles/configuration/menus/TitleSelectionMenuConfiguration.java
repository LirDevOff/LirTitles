package org.lirdev.lirtitles.configuration.menus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;

public record TitleSelectionMenuConfiguration(
        String id,
        Component title,
        int rows,
        MenuItem lockedItem,
        MenuItem availableItem,
        MenuItem activeItem,
        MenuItem backItem,
        MenuItem fontItem,
        MenuItem nextPageItem,
        MenuItem previousPageItem,
        List<MenuItem> staticItems,
        List<Integer> titleSlots) {
    private static final String RESOURCE_PATH = "menus/titles/selection.yml";

    public static TitleSelectionMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        int rows = MenuItemLoader.rows(yaml);
        return new TitleSelectionMenuConfiguration(
                loader.menuId(yaml, RESOURCE_PATH),
                loader.text(yaml, "menu_title", "{category}"),
                rows,
                loader.item(yaml, "items.locked"),
                loader.item(yaml, "items.available"),
                loader.item(yaml, "items.active"),
                loader.item(yaml, "items.back"),
                loader.optionalItem(yaml, "items.font"),
                loader.optionalItem(yaml, "items.next-page"),
                loader.optionalItem(yaml, "items.previous-page"),
                loader.staticItems(yaml, Set.of("locked", "available", "active", "back", "font", "next-page", "previous-page")),
                titleSlots(yaml.get("titles-slots"), rows * 9));
    }

    private static List<Integer> titleSlots(Object configuredRanges, int menuSize) {
        if (configuredRanges == null) {
            return List.of();
        }
        List<?> ranges = configuredRanges instanceof String range ? List.of(range)
                : configuredRanges instanceof List<?> values ? values : null;
        if (ranges == null) {
            throw new IllegalStateException("Menu setting 'titles-slots' must be a range or a list of ranges");
        }
        Set<Integer> slots = new LinkedHashSet<>();
        for (Object configuredRange : ranges) {
            if (!(configuredRange instanceof String range)) {
                throw new IllegalStateException("Each value in 'titles-slots' must be a range like '10-19'");
            }
            String[] bounds = range.trim().split("-", -1);
            if (bounds.length != 2) {
                throw new IllegalStateException("Invalid title slot range '" + range + "'; expected 'start-end'");
            }
            int start = parseBound(bounds[0], range);
            int end = parseBound(bounds[1], range);
            if (start < 0 || end <= start || end > menuSize) {
                throw new IllegalStateException("Title slot range '" + range + "' must be within 0-" + menuSize + " and have a positive length");
            }
            for (int slot = start; slot < end; slot++) {
                if (!slots.add(slot)) {
                    throw new IllegalStateException("Title slot ranges overlap at slot " + slot);
                }
            }
        }
        return List.copyOf(slots);
    }

    private static int parseBound(String value, String range) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid title slot range '" + range + "'; bounds must be whole numbers", exception);
        }
    }
}
