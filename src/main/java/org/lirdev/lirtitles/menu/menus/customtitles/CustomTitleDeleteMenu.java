package org.lirdev.lirtitles.menu.menus.customtitles;

import java.util.Map;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.CustomTitleDeleteMenuConfiguration;
import org.lirdev.lirtitles.data.model.CustomTitle;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.MenuClickKind;
import org.lirdev.lirtitles.menu.action.MenuClickAction;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.menu.menus.shared.StaticMenuItems;
import org.lirdev.lirtitles.utilities.PlaceholderReplacer;

public final class CustomTitleDeleteMenu {
    private final LirTitles plugin;
    private final MenuItemFactory itemFactory;
    private final StaticMenuItems staticItems;

    public CustomTitleDeleteMenu(LirTitles plugin) {
        this.plugin = plugin;
        itemFactory = new MenuItemFactory();
        staticItems = new StaticMenuItems(itemFactory);
    }

    public void clearCaches() {
        staticItems.clearCaches();
    }

    public ChestMenu create(Player player, CustomTitle title, Consumer<Player> onConfirm, Consumer<Player> onCancel) {
        CustomTitleDeleteMenuConfiguration appearance = plugin.configuration().customTitleDeleteMenu();
        Component text = plugin.colorizer().colorize(title.text());
        MenuItemTokens tokens = MenuItemTokens.of(Map.of("title", text, "preview", text));
        ChestMenu.Builder menu = ChestMenu.builder(PlaceholderReplacer.replace(appearance.title(), Map.of("title", text)), appearance.rows());
        staticItems.addTo(menu, player, appearance.staticItems());
        add(menu, appearance.titleItem().slot(), ConfiguredMenuButtons.create(player, appearance.titleItem(),
                itemFactory.create(appearance.titleItem(), tokens), Map.<MenuClickKind, MenuClickAction>of()));
        add(menu, appearance.confirmItem().slot(), ConfiguredMenuButtons.create(player, appearance.confirmItem(),
                itemFactory.create(appearance.confirmItem(), tokens), context -> onConfirm.accept(context.player())));
        add(menu, appearance.cancelItem().slot(), ConfiguredMenuButtons.create(player, appearance.cancelItem(),
                itemFactory.create(appearance.cancelItem(), tokens), context -> onCancel.accept(context.player())));
        return menu.build();
    }

    private static void add(ChestMenu.Builder menu, int slot, MenuButton button) {
        if (button != null && slot >= 0) {
            menu.button(slot, button);
        }
    }
}
