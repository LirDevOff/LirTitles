package org.lirdev.lirtitles.menu.bukkit;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import org.lirdev.lirtitles.menu.MenuCloser;
import org.lirdev.lirtitles.menu.input.bukkit.BukkitAnvilTextSessions;

public final class BukkitMenuCloser implements MenuCloser {
    private final Plugin plugin;
    private final BukkitAnvilTextSessions anvilSessions;

    public BukkitMenuCloser(Plugin plugin, BukkitAnvilTextSessions anvilSessions) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.anvilSessions = Objects.requireNonNull(anvilSessions, "anvilSessions");
    }

    @Override
    public void closeOpenMenus() {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> anvilSessions.has(player) || player.getOpenInventory().getTopInventory().getHolder() instanceof MenuInventoryHolder)
                .forEach(player -> player.getScheduler().run(plugin, scheduledTask -> player.closeInventory(), null));
    }
}
