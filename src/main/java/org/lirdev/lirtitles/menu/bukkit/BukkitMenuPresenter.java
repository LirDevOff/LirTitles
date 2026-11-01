package org.lirdev.lirtitles.menu.bukkit;

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.MenuNavigator;

public final class BukkitMenuPresenter implements MenuNavigator {
    private final BukkitMenuInventoryFactory inventoryFactory;

    public BukkitMenuPresenter(BukkitMenuInventoryFactory inventoryFactory) {
        this.inventoryFactory = Objects.requireNonNull(inventoryFactory, "inventoryFactory");
    }

    @Override
    public void open(Player player, ChestMenu menu) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(menu, "menu");
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory.getHolder(false) instanceof MenuInventoryHolder holder
                && topInventory.getSize() == menu.size()
                && Objects.equals(holder.menu().title(), menu.title())) {
            ChestMenu previousMenu = holder.menu();
            holder.updateMenu(menu);
            for (int slot = 0; slot < menu.size(); slot++) {
                ItemStack previousItem = displayItem(previousMenu.buttonAt(slot));
                ItemStack nextItem = displayItem(menu.buttonAt(slot));
                if (!Objects.equals(previousItem, nextItem)) {
                    topInventory.setItem(slot, nextItem);
                }
            }
            return;
        }
        player.openInventory(inventoryFactory.create(menu));
    }

    private static ItemStack displayItem(MenuButton button) {
        return button == null ? null : button.displayItem();
    }
}
