package org.lirdev.lirtitles.menu.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.lirdev.lirtitles.menu.ChestMenu;

public final class BukkitMenuInventoryFactory {
    public Inventory create(ChestMenu menu) {
        MenuInventoryHolder holder = new MenuInventoryHolder(menu);
        Inventory inventory = Bukkit.createInventory(holder, menu.size(), menu.title());
        holder.bind(inventory);
        menu.buttons().forEach((slot, button) -> inventory.setItem(slot, button.displayItem()));
        return inventory;
    }
}
