package org.lirdev.lirtitles.configuration.menus;

import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;

public record ModerationMenuConfiguration(
        String id,
        Component title,
        int rows,
        int firstSlot,
        MenuItem entryItem,
        MenuItem backItem,
        MenuItem nextPageItem,
        MenuItem previousPageItem,
        List<MenuItem> staticItems) {
    private static final String RESOURCE_PATH = "menus/moderation/queue.yml";

    public static ModerationMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        return new ModerationMenuConfiguration(
                loader.menuId(yaml, RESOURCE_PATH),
                loader.text(yaml, "menu_title", "Title moderation"),
                MenuItemLoader.rows(yaml),
                yaml.getInt("first_slot", 0),
                loader.item(yaml, "items.entry"),
                loader.item(yaml, "items.back"),
                loader.item(yaml, "items.next-page"),
                loader.item(yaml, "items.previous-page"),
                loader.staticItems(yaml, Set.of("entry", "back", "next-page", "previous-page")));
    }
}
