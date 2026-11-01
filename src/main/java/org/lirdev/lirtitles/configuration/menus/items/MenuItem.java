package org.lirdev.lirtitles.configuration.menus.items;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import org.lirdev.lirtitles.menu.MenuClickKind;
import org.lirdev.lirtitles.menu.action.MenuClickAction;
import org.lirdev.lirtitles.menu.action.MenuRequirement;

public record MenuItem(
        Material material,
        String customMaterial,
        int amount,
        int slot,
        List<Integer> slots,
        Component name,
        List<Component> lore,
        Integer customModelData,
        boolean glow,
        String baseHead,
        List<MenuClickAction> clickCommands,
        Map<MenuClickKind, List<MenuClickAction>> clickActions,
        List<MenuRequirement> viewRequirements,
        List<MenuRequirement> clickRequirements,
        List<MenuClickAction> denyActions) {
    public static final int NO_SLOT = -1;

    public MenuItem {
        Objects.requireNonNull(material);
        customMaterial = customMaterial == null ? "" : customMaterial;
        amount = Math.clamp(amount, 1, material.getMaxStackSize());
        slots = List.copyOf(slots);
        lore = List.copyOf(lore);
        baseHead = baseHead == null ? "" : baseHead;
        clickCommands = List.copyOf(clickCommands);
        clickActions = Map.copyOf(clickActions);
        viewRequirements = List.copyOf(viewRequirements);
        clickRequirements = List.copyOf(clickRequirements);
        denyActions = List.copyOf(denyActions);
    }

    public List<MenuClickAction> leftClickActions() {
        return clickActions.getOrDefault(MenuClickKind.LEFT, List.of());
    }
}
