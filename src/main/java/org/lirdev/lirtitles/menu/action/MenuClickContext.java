package org.lirdev.lirtitles.menu.action;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuMessageRenderer;
import org.lirdev.lirtitles.menu.MenuNavigator;
import org.lirdev.lirtitles.menu.MenuOpener;

public record MenuClickContext(Player player, ChestMenu menu, MenuNavigator menuNavigator,
                               MenuMessageRenderer messageRenderer,
                               MenuOpener menuOpener, int slot, ClickType clickType) {
}
