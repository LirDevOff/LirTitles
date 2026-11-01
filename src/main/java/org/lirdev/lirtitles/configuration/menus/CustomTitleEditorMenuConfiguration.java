package org.lirdev.lirtitles.configuration.menus;

import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;

public record CustomTitleEditorMenuConfiguration(
        String id,
        Component title,
        int rows,
        MenuItem previewItem,
        MenuItem confirmItem,
        MenuItem cancelItem,
        List<MenuItem> colorSlots,
        List<FormatButton> formats,
        List<MenuItem> staticItems) {
    private static final String RESOURCE_PATH = "menus/custom-titles/editor.yml";

    public static CustomTitleEditorMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        List<FormatButton> formats = List.of(
                format(loader, yaml, "bold", "<bold>"),
                format(loader, yaml, "italic", "<italic>"),
                format(loader, yaml, "underlined", "<underlined>"),
                format(loader, yaml, "strikethrough", "<strikethrough>"),
                format(loader, yaml, "obfuscated", "<obfuscated>"));
        return new CustomTitleEditorMenuConfiguration(
                loader.menuId(yaml, RESOURCE_PATH),
                loader.text(yaml, "menu_title", "Custom title"),
                MenuItemLoader.rows(yaml),
                loader.item(yaml, "items.preview"),
                loader.item(yaml, "items.confirm"),
                loader.item(yaml, "items.cancel"),
                loader.itemList(yaml, "items.colors"),
                formats,
                loader.staticItems(yaml, Set.of("preview", "confirm", "cancel", "colors", "bold", "italic", "underlined", "strikethrough", "obfuscated")));
    }

    private static FormatButton format(MenuItemLoader loader, ConfigurationSection yaml, String key, String tag) {
        return new FormatButton(loader.item(yaml, "items." + key + ".enabled"),
                loader.item(yaml, "items." + key + ".disabled"), tag);
    }

    public record FormatButton(MenuItem enabledItem, MenuItem disabledItem, String tag) {
        public MenuItem item(boolean enabled) {
            return enabled ? enabledItem : disabledItem;
        }
    }
}
