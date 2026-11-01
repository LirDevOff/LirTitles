package org.lirdev.lirtitles.configuration.menus;

import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.colors.PaletteColor;
import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;

public record ColorSelectionMenuConfiguration(
        String id,
        Component title,
        int rows,
        MenuItem backItem,
        MenuItem customItem,
        List<PaletteColor> colors,
        List<MenuItem> staticItems) {
    private static final String RESOURCE_PATH = "menus/colors/palette.yml";

    public static ColorSelectionMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        List<PaletteColor> colors = PaletteColor.loadList(loader, yaml.getMapList("colors"), "colors");
        List<MenuItem> staticItems = loader.staticItems(yaml, Set.of("back", "custom"));
        return new ColorSelectionMenuConfiguration(
                loader.menuId(yaml, RESOURCE_PATH),
                loader.text(yaml, "menu_title", "Choose a color"),
                MenuItemLoader.rows(yaml),
                loader.item(yaml, "items.back"),
                loader.item(yaml, "items.custom"),
                colors,
                staticItems);
    }
}
