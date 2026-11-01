package org.lirdev.lirtitles.menu.bukkit;

import java.util.Objects;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.lirdev.lirtitles.menu.menus.TitleMenuController;

public final class BukkitPlayerMenuStateListener implements Listener {
    private final TitleMenuController menus;

    public BukkitPlayerMenuStateListener(TitleMenuController menus) {
        this.menus = Objects.requireNonNull(menus, "menus");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        menus.clearPlayerState(event.getPlayer().getUniqueId());
    }
}
