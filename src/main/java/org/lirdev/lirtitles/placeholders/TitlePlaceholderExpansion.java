package org.lirdev.lirtitles.placeholders;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.titles.Titles;
import org.lirdev.lirtitles.fonts.FontRegistry;
import org.lirdev.lirtitles.data.model.CustomTitle;
import org.lirdev.lirtitles.data.model.PlayerTitleSnapshot;
import org.lirdev.lirtitles.metadata.PluginInfo;

final class TitlePlaceholderExpansion extends PlaceholderExpansion {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final int MAX_RENDERED_ENTRIES = 4096;

    private final LirTitles plugin;
    private final PlayerTitleSnapshot snapshot;
    private final ConcurrentHashMap<UUID, RenderedTitle> renderedTitles = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<UUID> insertionOrder = new ConcurrentLinkedQueue<>();

    TitlePlaceholderExpansion(LirTitles plugin, PlayerTitleSnapshot snapshot) {
        this.plugin = plugin;
        this.snapshot = snapshot;
    }

    @Override
    public String getIdentifier() {
        return PluginInfo.ID;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String parameter) {
        if (player == null || parameter == null) {
            return null;
        }

        return "active".equalsIgnoreCase(parameter) ? renderedActive(player) : null;
    }

    private String renderedActive(OfflinePlayer player) {
        UUID playerId = player.getUniqueId();
        String titleKey = snapshot.cachedSelectedTitle(playerId);
        Titles.Title ordinary = plugin.configuration().titles().find(titleKey);
        CustomTitle custom = null;
        Component active;
        Object source;
        if (ordinary != null) {
            active = ordinary.displayName();
            source = ordinary;
        } else if (CustomTitle.isCustomKey(titleKey)) {
            try {
                custom = snapshot.cachedCustomTitle(playerId, CustomTitle.idFromKey(titleKey));
            } catch (IllegalArgumentException exception) {
                return "";
            }
            if (custom == null) {
                return "";
            }
            active = plugin.colorizer().colorize(custom.text());
            source = custom;
        } else {
            return "";
        }
        if (active == null) {
            return "";
        }

        String fontId = snapshot.cachedSelectedFont(playerId);
        FontRegistry fonts = plugin.configuration().fonts();
        RenderedTitle cached = renderedTitles.get(playerId);
        if (cached != null && cached.matches(titleKey, fontId, source, fonts)) {
            return cached.value();
        }

        String rendered = MINI_MESSAGE.serialize(fonts.render(fontId, active));
        RenderedTitle previous = renderedTitles.put(playerId,
                new RenderedTitle(titleKey, fontId, source, fonts, rendered));
        if (previous == null) {
            insertionOrder.offer(playerId);
        }
        trimCache();
        return rendered;
    }

    private void trimCache() {
        while (renderedTitles.size() > MAX_RENDERED_ENTRIES) {
            UUID oldest = insertionOrder.poll();
            if (oldest == null) {
                return;
            }
            renderedTitles.remove(oldest);
        }
    }

    private record RenderedTitle(String titleKey, String fontId, Object source, FontRegistry fonts, String value) {
        private boolean matches(String titleKey, String fontId, Object source, FontRegistry fonts) {
            return Objects.equals(this.titleKey, titleKey)
                    && Objects.equals(this.fontId, fontId)
                    && this.source == source
                    && this.fonts == fonts;
        }
    }
}
