package org.lirdev.lirtitles.configuration.menus;

import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;

public record CustomTitleDeleteMenuConfiguration(String id, Component title, int rows, MenuItem titleItem,
                                                 MenuItem confirmItem, MenuItem cancelItem,
                                                 List<MenuItem> staticItems) {
    private static final String RESOURCE_PATH = "menus/custom-titles/delete-confirmation.yml";

    public static CustomTitleDeleteMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        return new CustomTitleDeleteMenuConfiguration(loader.menuId(yaml, RESOURCE_PATH), loader.text(yaml, "menu_title", "Delete {title}"), MenuItemLoader.rows(yaml),
                loader.item(yaml, "items.title"), loader.item(yaml, "items.confirm"),
                loader.item(yaml, "items.cancel"), loader.staticItems(yaml, Set.of("title", "confirm", "cancel")));
    }
}
