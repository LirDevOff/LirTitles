package org.lirdev.lirtitles.configuration.menus;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;

public record CustomTitleInputMenuConfiguration(Component title, Component emptyPreview, MenuItem cancelItem,
                                                MenuItem previewItem,
                                                MenuItem confirmItem) {
    private static final String RESOURCE_PATH = "menus/custom-titles/input.yml";

    public static CustomTitleInputMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        return new CustomTitleInputMenuConfiguration(
                loader.text(yaml, "menu_title", "Custom title"),
                loader.text(yaml, "empty_preview", "Type the title text"),
                loader.item(yaml, "items.cancel"),
                loader.item(yaml, "items.preview"),
                loader.item(yaml, "items.confirm"));
    }
}
