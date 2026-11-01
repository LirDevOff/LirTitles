package org.lirdev.lirtitles.menu.input.bukkit;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

public final class BukkitAnvilTextSessions {
    private final Map<UUID, BukkitAnvilTextSession> sessions = new ConcurrentHashMap<>();

    BukkitAnvilTextSession register(Player player, BukkitAnvilTextSession session) {
        return sessions.put(player.getUniqueId(), session);
    }

    BukkitAnvilTextSession find(Player player) {
        return sessions.get(player.getUniqueId());
    }

    void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean has(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void disposeAll() {
        List<BukkitAnvilTextSession> active = List.copyOf(sessions.values());
        sessions.clear();
        active.forEach(BukkitAnvilTextSession::dispose);
    }
}
