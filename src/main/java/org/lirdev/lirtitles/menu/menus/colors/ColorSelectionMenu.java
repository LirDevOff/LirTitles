package org.lirdev.lirtitles.menu.menus.colors;

import java.util.Map;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.ColorSelectionMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.colors.PaletteColor;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.menu.menus.shared.StaticMenuItems;
import org.lirdev.lirtitles.utilities.HexColor;
import org.lirdev.lirtitles.utilities.PlaceholderReplacer;

public final class ColorSelectionMenu {
    private final LirTitles plugin;
    private final MenuItemFactory itemFactory;
    private final StaticMenuItems staticItems;

    public ColorSelectionMenu(LirTitles plugin) {
        this.plugin = plugin;
        itemFactory = new MenuItemFactory();
        staticItems = new StaticMenuItems(itemFactory);
    }

    public void clearCaches() {
        staticItems.clearCaches();
        itemFactory.clearCaches();
    }

    public ChestMenu create(Player player, int colorSlot, HexColor selected, Consumer<PaletteColor> onSelect,
                            Consumer<Player> onCustomColor, Consumer<Player> onBack) {
        ColorSelectionMenuConfiguration appearance = plugin.configuration().colorSelectionMenu();
        Component title = PlaceholderReplacer.replace(appearance.title(), Map.of("slot", Component.text(colorSlot + 1)));
        ChestMenu.Builder menu = ChestMenu.builder(title, appearance.rows());
        staticItems.addTo(menu, player, appearance.staticItems());
        add(menu, appearance.backItem().slot(), ConfiguredMenuButtons.create(player, appearance.backItem(),
                itemFactory.create(appearance.backItem(), MenuItemTokens.NONE), context -> onBack.accept(context.player())));
        add(menu, appearance.customItem().slot(), ConfiguredMenuButtons.create(player, appearance.customItem(),
                itemFactory.create(appearance.customItem(), MenuItemTokens.NONE), context -> onCustomColor.accept(context.player())));
        for (PaletteColor option : appearance.colors()) {
            MenuItemTokens tokens = selectionTokens(option, selected);
            ItemStack display = itemFactory.createDyedCached(option.item(), tokens, option.hex());
            add(menu, option.item().slot(), ConfiguredMenuButtons.create(player, option.item(), display,
                    context -> onSelect.accept(option)));
        }
        return menu.build();
    }

    private static MenuItemTokens selectionTokens(PaletteColor option, HexColor selected) {
        boolean isSelected = selected != null && selected.equals(option.hex());
        return MenuItemTokens.of(Map.of(
                "selected", Component.text(isSelected ? "✔" : ""),
                "hex", Component.text("(" + option.hex().hex() + ")").color(NamedTextColor.GRAY)));
    }

    private static void add(ChestMenu.Builder menu, int slot, MenuButton button) {
        if (button != null && slot >= 0) {
            menu.button(slot, button);
        }
    }
}
