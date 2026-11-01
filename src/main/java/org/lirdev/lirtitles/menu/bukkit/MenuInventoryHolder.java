package org.lirdev.lirtitles.menu.bukkit;

import java.util.Objects;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.lirdev.lirtitles.menu.ChestMenu;

final class MenuInventoryHolder implements InventoryHolder {
    private ChestMenu menu;
    private Inventory inventory;

    MenuInventoryHolder(ChestMenu menu) {
        this.menu = Objects.requireNonNull(menu, "menu");
    }

    ChestMenu menu() {
        return menu;
    }

    void updateMenu(ChestMenu menu) {
        this.menu = Objects.requireNonNull(menu, "menu");
    }

    void bind(Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    public Inventory getInventory() {
        return Objects.requireNonNull(inventory, "inventory");
    }
}
