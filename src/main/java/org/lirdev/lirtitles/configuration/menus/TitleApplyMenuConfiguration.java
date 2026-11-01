package org.lirdev.lirtitles.configuration.menus;

import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;

public record TitleApplyMenuConfiguration(String id, Component title, int rows, MenuItem titleItem, MenuItem fontItem,
                                          Component selectedFontFormat, Component availableFontFormat,
                                          MenuItem applyItem, MenuItem denyItem,
                                          List<MenuItem> staticItems) {
    private static final String RESOURCE_PATH = "menus/titles/confirmation.yml";

    public static TitleApplyMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        return new TitleApplyMenuConfiguration(loader.menuId(yaml, RESOURCE_PATH), loader.text(yaml, "menu_title", "{title}"), MenuItemLoader.rows(yaml),
                loader.item(yaml, "items.title"), loader.item(yaml, "items.font"),
                loader.text(yaml, "items.font.selected_format", "<green>— {font}"),
                loader.text(yaml, "items.font.available_format", "<gray>{font}"),
                loader.item(yaml, "items.apply"), loader.item(yaml, "items.deny"),
                loader.staticItems(yaml, Set.of("title", "font", "apply", "deny")));
    }
}
