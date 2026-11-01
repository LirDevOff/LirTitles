package org.lirdev.lirtitles.configuration.menus;

import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;

public record ColorShadeMenuConfiguration(
        String id,
        Component title,
        int rows,
        MenuItem backItem,
        MenuItem customItem,
        MenuItem shadeItem,
        int generatedShadeCount,
        List<MenuItem> staticItems) {
    private static final int DEFAULT_GENERATED_SHADE_COUNT = 27;
    private static final String RESOURCE_PATH = "menus/colors/shades.yml";

    public static ColorShadeMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        return new ColorShadeMenuConfiguration(
                loader.menuId(yaml, RESOURCE_PATH),
                loader.text(yaml, "menu_title", "Choose a shade"),
                MenuItemLoader.rows(yaml),
                loader.item(yaml, "items.back"),
                loader.item(yaml, "items.custom"),
                loader.item(yaml, "items.shade"),
                positiveOrDefault(yaml.getInt("generated_shade_count", DEFAULT_GENERATED_SHADE_COUNT)),
                loader.staticItems(yaml, Set.of("back", "custom", "shade")));
    }

    private static int positiveOrDefault(int value) {
        return value > 0 ? value : DEFAULT_GENERATED_SHADE_COUNT;
    }
}
