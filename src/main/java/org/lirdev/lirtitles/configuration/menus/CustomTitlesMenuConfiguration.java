package org.lirdev.lirtitles.configuration.menus;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;
import org.lirdev.lirtitles.menu.menus.customtitles.TitleDisplayState;

public record CustomTitlesMenuConfiguration(
        String id,
        Component title,
        int rows,
        Layout layout,
        Layout createLayout,
        int firstSlot,
        TitleStatusItems titleItems,
        MenuItem createItem,
        MenuItem fontItem,
        MenuItem backItem,
        MenuItem nextPageItem,
        MenuItem previousPageItem,
        List<MenuItem> staticItems) {
    private static final String RESOURCE_PATH = "menus/custom-titles/list.yml";

    public static CustomTitlesMenuConfiguration load(MenuItemLoader loader) {
        ConfigurationSection yaml = loader.loadFile(RESOURCE_PATH);
        return new CustomTitlesMenuConfiguration(
                loader.menuId(yaml, RESOURCE_PATH),
                loader.text(yaml, "menu_title", "Custom titles"),
                MenuItemLoader.rows(yaml),
                Layout.from(yaml.getString("layout", "dynamic")),
                Layout.from(yaml.getString("items.create.layout", "static")),
                yaml.getInt("first_slot", 0),
                TitleStatusItems.load(loader, yaml),
                loader.item(yaml, "items.create"),
                loader.optionalItem(yaml, "items.font"),
                loader.item(yaml, "items.back"),
                loader.item(yaml, "items.next-page"),
                loader.item(yaml, "items.previous-page"),
                loader.staticItems(yaml, Set.of("title", "create", "font", "back", "next-page", "previous-page")));
    }

    public enum Layout {
        STATIC,
        DYNAMIC;

        static Layout from(String value) {
            try {
                return Layout.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Unknown custom title layout '" + value + "'");
            }
        }
    }

    public record TitleStatusItems(MenuItem pending, MenuItem active, MenuItem normal, MenuItem rejected) {
        static TitleStatusItems load(MenuItemLoader loader, ConfigurationSection yaml) {
            return new TitleStatusItems(
                    loader.item(yaml, "items.title.pending"),
                    loader.item(yaml, "items.title.active"),
                    loader.item(yaml, "items.title.normal"),
                    loader.item(yaml, "items.title.rejected"));
        }

        public MenuItem forState(TitleDisplayState state) {
            return switch (state) {
                case PENDING -> pending;
                case ACTIVE -> active;
                case NORMAL -> normal;
                case REJECTED -> rejected;
            };
        }
    }
}
