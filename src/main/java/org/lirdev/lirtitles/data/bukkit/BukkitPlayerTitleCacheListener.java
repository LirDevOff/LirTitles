package org.lirdev.lirtitles.data.bukkit;

import java.util.Objects;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.lirdev.lirtitles.data.PlayerTitleStorage;

public final class BukkitPlayerTitleCacheListener implements Listener {
    private final Plugin plugin;
    private final PlayerTitleStorage storage;

    public BukkitPlayerTitleCacheListener(Plugin plugin, PlayerTitleStorage storage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        storage.preloadAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        storage.invalidate(event.getPlayer().getUniqueId());
    }
}
