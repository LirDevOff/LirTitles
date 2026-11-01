package org.lirdev.lirtitles.bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import org.lirdev.lirtitles.permissions.Permissions;
import org.lirdev.lirtitles.utilities.Logger;

public final class BukkitDevelopmentNoticeListener implements Listener {
    private static final long DEVELOPMENT_NOTICE_DELAY_TICKS = 100L;
    private static final String ISSUE_URL = "https://github.com/LirDevOff/LirTitles/issues";
    private static final Component NOTICE = Component.text("This plugin is under development. If you encounter a bug, please report it at ", NamedTextColor.WHITE)
            .append(Component.text(ISSUE_URL, NamedTextColor.WHITE).clickEvent(ClickEvent.openUrl(ISSUE_URL)));
    private final JavaPlugin plugin;
    private final Logger logger;

    public BukkitDevelopmentNoticeListener(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission(Permissions.ADMIN)) {
            logger.player(event.getPlayer(), NOTICE);
        }
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin,
                task -> logger.info("This plugin is under development. If you encounter a bug, please report it at " + ISSUE_URL),
                DEVELOPMENT_NOTICE_DELAY_TICKS);
    }
}
