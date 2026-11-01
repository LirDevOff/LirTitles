package org.lirdev.lirtitles.menu.bukkit;

import java.util.List;
import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.MenuMessageRenderer;
import org.lirdev.lirtitles.menu.MenuNavigator;
import org.lirdev.lirtitles.menu.MenuOpener;
import org.lirdev.lirtitles.menu.action.MenuClickAction;
import org.lirdev.lirtitles.menu.action.MenuClickContext;

public final class BukkitMenuClickListener implements Listener {
    private final Plugin plugin;
    private final MenuNavigator menuNavigator;
    private final MenuMessageRenderer messageRenderer;
    private final MenuOpener menuOpener;

    public BukkitMenuClickListener(Plugin plugin, MenuNavigator menuNavigator, MenuMessageRenderer messageRenderer, MenuOpener menuOpener) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.menuNavigator = Objects.requireNonNull(menuNavigator, "menuNavigator");
        this.messageRenderer = Objects.requireNonNull(messageRenderer, "messageRenderer");
        this.menuOpener = Objects.requireNonNull(menuOpener, "menuOpener");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof MenuInventoryHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= topInventory.getSize()) {
            return;
        }
        MenuButton button = holder.menu().buttonAt(event.getRawSlot());
        if (button == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        List<MenuClickAction> actions = button.clickActions(event.getClick());
        if (actions.isEmpty()) {
            return;
        }
        MenuClickContext context = new MenuClickContext(player, holder.menu(), menuNavigator, messageRenderer, menuOpener, event.getRawSlot(), event.getClick());
        player.getScheduler().run(plugin, scheduledTask -> {
            for (MenuClickAction action : actions) {
                action.execute(context);
            }
        }, null);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof MenuInventoryHolder)) {
            return;
        }
        int topInventorySize = topInventory.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topInventorySize) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
