package org.lirdev.lirtitles.configuration.menus;

import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;
import org.lirdev.lirtitles.configuration.titles.Titles;

public record CategoriesMenuConfiguration(
        String id,
        Component title,
        int rows,
        MenuItem categoryItem,
        MenuItem customTitlesItem,
        MenuItem fontItem,
        List<MenuItem> staticItems) {
    private static final String RESOURCE_PATH = "menus/titles/categories.yml";

    public static CategoriesMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        return new CategoriesMenuConfiguration(
                loader.menuId(yaml, RESOURCE_PATH),
                loader.text(yaml, "menu_title", "Titles"),
                MenuItemLoader.rows(yaml),
                loader.item(yaml, "items.category"),
                loader.optionalItem(yaml, "items.custom-titles"),
                loader.optionalItem(yaml, "items.font"),
                loader.staticItems(yaml, Set.of("category", "custom-titles", "font")));
    }
}
