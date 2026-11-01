package org.lirdev.lirtitles.menu.input.bukkit;

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class BukkitAnvilTextListener implements Listener {
    private final Plugin plugin;
    private final BukkitAnvilTextSessions sessions;

    public BukkitAnvilTextListener(Plugin plugin, BukkitAnvilTextSessions sessions) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    @EventHandler
    public void onPrepare(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        BukkitAnvilTextSession session = ownedSession(player, event.getView());
        if (session == null) {
            return;
        }
        session.prepare(event);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        BukkitAnvilTextSession session = ownedSession(player, event.getView());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        switch (event.getRawSlot()) {
            case BukkitAnvilTextSession.CANCEL_SLOT -> schedule(player, session::cancel);
            case BukkitAnvilTextSession.CONFIRM_SLOT -> confirmIfAvailable(player, session, event.getCurrentItem());
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && ownedSession(player, event.getView()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        BukkitAnvilTextSession session = ownedSession(player, event.getView());
        if (session == null) {
            return;
        }
        sessions.remove(player);
        session.dispose();
        if (event.getReason() == InventoryCloseEvent.Reason.PLAYER) {
            schedule(player, session::cancel);
        }
    }

    private void confirmIfAvailable(Player player, BukkitAnvilTextSession session, ItemStack result) {
        if (result != null && !result.getType().isAir()) {
            schedule(player, session::confirm);
        }
    }

    private BukkitAnvilTextSession ownedSession(Player player, InventoryView view) {
        BukkitAnvilTextSession session = sessions.find(player);
        return session != null && session.owns(view) ? session : null;
    }

    private void schedule(Player player, Runnable action) {
        player.getScheduler().run(plugin, scheduledTask -> action.run(), null);
    }
}
