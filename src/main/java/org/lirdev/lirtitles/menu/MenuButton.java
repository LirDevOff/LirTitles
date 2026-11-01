package org.lirdev.lirtitles.menu;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.menu.action.MenuClickAction;

public final class MenuButton {
    private final ItemStack displayItem;
    private final List<MenuClickAction> anyClickActions;
    private final Map<MenuClickKind, List<MenuClickAction>> clickActions;

    private MenuButton(ItemStack displayItem, List<MenuClickAction> anyClickActions, Map<MenuClickKind, List<MenuClickAction>> clickActions) {
        this.displayItem = Objects.requireNonNull(displayItem, "displayItem");
        this.anyClickActions = List.copyOf(anyClickActions);
        this.clickActions = combinedActions(this.anyClickActions, clickActions);
    }

    public static MenuButton of(ItemStack displayItem, List<MenuClickAction> anyClickActions, Map<MenuClickKind, List<MenuClickAction>> clickActions) {
        return new MenuButton(Objects.requireNonNull(displayItem, "displayItem"), anyClickActions, clickActions);
    }

    public ItemStack displayItem() {
        return displayItem;
    }

    public List<MenuClickAction> clickActions(ClickType clickType) {
        MenuClickKind kind = MenuClickKind.classify(clickType);
        return kind == null ? anyClickActions : clickActions.getOrDefault(kind, anyClickActions);
    }

    private static Map<MenuClickKind, List<MenuClickAction>> combinedActions(List<MenuClickAction> anyClickActions,
                                                                             Map<MenuClickKind, List<MenuClickAction>> clickActions) {
        if (clickActions.isEmpty()) {
            return Map.of();
        }
        Map<MenuClickKind, List<MenuClickAction>> combined = new EnumMap<>(MenuClickKind.class);
        clickActions.forEach((kind, actions) -> {
            if (anyClickActions.isEmpty()) {
                combined.put(kind, List.copyOf(actions));
                return;
            }
            java.util.ArrayList<MenuClickAction> merged = new java.util.ArrayList<>(anyClickActions.size() + actions.size());
            merged.addAll(anyClickActions);
            merged.addAll(actions);
            combined.put(kind, List.copyOf(merged));
        });
        return Map.copyOf(combined);
    }
}
