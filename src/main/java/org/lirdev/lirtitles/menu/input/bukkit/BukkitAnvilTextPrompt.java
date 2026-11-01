package org.lirdev.lirtitles.menu.input.bukkit;

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.view.AnvilView;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.lirdev.lirtitles.menu.input.AnvilTextPrompt;
import org.lirdev.lirtitles.menu.input.AnvilTextRequest;

public final class BukkitAnvilTextPrompt implements AnvilTextPrompt {
    private final BukkitAnvilTextSessions sessions;

    public BukkitAnvilTextPrompt(BukkitAnvilTextSessions sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    @Override
    public void open(Player player, AnvilTextRequest request) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(request, "request");
        InventoryView openedView = player.openAnvil(null, true);
        if (!(openedView instanceof AnvilView view)) {
            throw new IllegalStateException("The opened inventory is not an anvil view");
        }
        view.setTitle(PlainTextComponentSerializer.plainText().serialize(request.title()));
        BukkitAnvilTextSession session = new BukkitAnvilTextSession(view, request);
        BukkitAnvilTextSession previous = sessions.register(player, session);
        if (previous != null) {
            previous.dispose();
        }
        session.render();
    }
}
