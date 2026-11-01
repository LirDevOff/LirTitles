package org.lirdev.lirtitles.menu.menus.fonts;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.fonts.TitleFont;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.MenuClickKind;
import org.lirdev.lirtitles.menu.action.MenuClickAction;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.utilities.Branding;
import org.lirdev.lirtitles.utilities.Colorizer;

final class FontSelectionMenuItem {
    private FontSelectionMenuItem() {
    }

    static ItemStack displayItem(LirTitles plugin, MenuItem appearance, MenuItemFactory itemFactory,
                                 List<TitleFont> fonts, TitleFont selectedFont) {
        if (selectedFont == null) {
            return itemFactory.create(appearance, MenuItemTokens.NONE);
        }
        Colorizer colorizer = plugin.colorizer();
        MenuItemTokens tokens = MenuItemTokens.of(Map.of("font", colorizer.colorize(selectedFont.displayName())))
                .withBlock("font_options", fontOptions(colorizer, fonts, selectedFont));
        return itemFactory.create(appearance, tokens);
    }

    static MenuButton create(MenuItem appearance, Player player, List<TitleFont> fonts,
                             TitleFont selectedFont, ItemStack displayItem, BiConsumer<Player, String> onFontChanged) {
        if (selectedFont == null) {
            return ConfiguredMenuButtons.create(player, appearance, displayItem, (MenuClickAction) null);
        }
        Map<MenuClickKind, MenuClickAction> actions = Map.of(
                MenuClickKind.LEFT, changeFont(fonts, selectedFont, 1, onFontChanged),
                MenuClickKind.SHIFT_LEFT, changeFont(fonts, selectedFont, 1, onFontChanged),
                MenuClickKind.RIGHT, changeFont(fonts, selectedFont, -1, onFontChanged),
                MenuClickKind.SHIFT_RIGHT, changeFont(fonts, selectedFont, -1, onFontChanged)
        );
        return ConfiguredMenuButtons.create(player, appearance, displayItem, actions);
    }

    private static MenuClickAction changeFont(List<TitleFont> fonts, TitleFont selectedFont, int direction, BiConsumer<Player, String> onFontChanged) {
        String fontId = cycle(fonts, selectedFont, direction);
        return context -> onFontChanged.accept(context.player(), fontId);
    }

    private static List<Component> fontOptions(Colorizer colorizer, List<TitleFont> fonts, TitleFont selectedFont) {
        return fonts.stream().map(font -> fontOption(colorizer, font, font.equals(selectedFont))).toList();
    }

    private static Component fontOption(Colorizer colorizer, TitleFont font, boolean selected) {
        Component marker = selected ? Component.text(" → ", Branding.PRIMARY) : Component.text(" ", Branding.SECONDARY);
        return marker.append(colorizer.colorize(font.displayName()));
    }

    private static String cycle(List<TitleFont> fonts, TitleFont current, int direction) {
        int index = Math.max(fonts.indexOf(current), 0);
        int nextIndex = Math.floorMod(index + direction, fonts.size());
        return fonts.get(nextIndex).id();
    }
}
