package org.lirdev.lirtitles.menu.menus.shared;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.action.MenuClickAction;

public final class StaticMenuItems {
    private final MenuItemFactory itemFactory;
    private final Map<MenuItem, ItemStack> cachedItemStacks = new IdentityHashMap<>();
    private final Map<MenuItem, MenuButton> cachedButtons = new IdentityHashMap<>();

    public StaticMenuItems(MenuItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    public synchronized void addTo(ChestMenu.Builder menu, Player viewer, List<MenuItem> items) {
        for (MenuItem item : items) {
            MenuButton button = createButton(viewer, item);
            if (button == null) {
                continue;
            }
            for (int slot : item.slots()) {
                menu.button(slot, button);
            }
        }
    }

    public synchronized void clearCaches() {
        cachedItemStacks.clear();
        cachedButtons.clear();
        itemFactory.clearCaches();
    }

    private MenuButton createButton(Player viewer, MenuItem item) {
        if (item.viewRequirements().isEmpty() && item.clickRequirements().isEmpty()) {
            return cachedButtons.computeIfAbsent(item, this::createUnconditionalButton);
        }
        ItemStack displayItem = cachedItemStacks.computeIfAbsent(item, key -> itemFactory.create(key, MenuItemTokens.BLANK));
        return ConfiguredMenuButtons.create(viewer, item, displayItem, (MenuClickAction) null);
    }

    private MenuButton createUnconditionalButton(MenuItem item) {
        ItemStack displayItem = cachedItemStacks.computeIfAbsent(item, key -> itemFactory.create(key, MenuItemTokens.BLANK));
        return ConfiguredMenuButtons.create(null, item, displayItem, (MenuClickAction) null);
    }
}
