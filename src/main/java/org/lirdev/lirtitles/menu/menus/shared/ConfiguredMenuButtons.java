package org.lirdev.lirtitles.menu.menus.shared;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.MenuClickKind;
import org.lirdev.lirtitles.menu.action.MenuClickAction;
import org.lirdev.lirtitles.menu.action.MenuRequirement;

public final class ConfiguredMenuButtons {
    private ConfiguredMenuButtons() {
    }

    public static MenuButton create(Player viewer, MenuItem item, ItemStack displayItem, MenuClickAction onLeftClick) {
        return create(viewer, item, displayItem, onLeftClick == null ? Map.of() : Map.of(MenuClickKind.LEFT, onLeftClick));
    }

    public static MenuButton create(Player viewer, MenuItem item, ItemStack displayItem, Map<MenuClickKind, MenuClickAction> onSelectByKind) {
        if (!passes(viewer, item.viewRequirements())) {
            return null;
        }
        if (!passes(viewer, item.clickRequirements())) {
            return MenuButton.of(displayItem, item.denyActions(), Map.of());
        }
        if (onSelectByKind.isEmpty()) {
            return MenuButton.of(displayItem, item.clickCommands(), item.clickActions());
        }
        if (item.clickActions().isEmpty() && onSelectByKind.size() == 1) {
            Map.Entry<MenuClickKind, MenuClickAction> selection = onSelectByKind.entrySet().iterator().next();
            return MenuButton.of(displayItem, item.clickCommands(), Map.of(selection.getKey(), List.of(selection.getValue())));
        }
        return MenuButton.of(displayItem, item.clickCommands(), withSelectActions(item.clickActions(), onSelectByKind));
    }

    private static boolean passes(Player viewer, List<MenuRequirement> requirements) {
        for (MenuRequirement requirement : requirements) {
            if (!requirement.test(viewer)) {
                return false;
            }
        }
        return true;
    }

    private static Map<MenuClickKind, List<MenuClickAction>> withSelectActions(Map<MenuClickKind, List<MenuClickAction>> actions, Map<MenuClickKind, MenuClickAction> onSelectByKind) {
        Map<MenuClickKind, List<MenuClickAction>> result = new EnumMap<>(MenuClickKind.class);
        result.putAll(actions);
        onSelectByKind.forEach((kind, action) -> {
            List<MenuClickAction> kindActions = new ArrayList<>(result.getOrDefault(kind, List.of()));
            kindActions.add(action);
            result.put(kind, List.copyOf(kindActions));
        });
        return Map.copyOf(result);
    }
}
