package org.lirdev.lirtitles.menu.action;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface MenuRequirement {
    boolean test(Player player);
}
