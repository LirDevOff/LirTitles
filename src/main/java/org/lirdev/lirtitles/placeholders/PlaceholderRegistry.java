package org.lirdev.lirtitles.placeholders;

import java.util.Objects;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.data.model.PlayerTitleSnapshot;

public final class PlaceholderRegistry {
    public static final String PLUGIN_NAME = "PlaceholderAPI";

    private final LirTitles plugin;
    private TitlePlaceholderExpansion expansion;

    public PlaceholderRegistry(LirTitles plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean register(PlayerTitleSnapshot snapshot) {
        if (expansion != null || !plugin.getServer().getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
            return false;
        }
        TitlePlaceholderExpansion candidate = new TitlePlaceholderExpansion(plugin, snapshot);
        try {
            if (candidate.register()) {
                expansion = candidate;
                return true;
            }
            plugin.getLogger().warning("Could not register the LirTitles PlaceholderAPI expansion.");
        } catch (RuntimeException exception) {
            plugin.getLogger()
                    .warning("Could not register the LirTitles PlaceholderAPI expansion: " + exception.getMessage());
        }
        return false;
    }

    public void unregister() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
    }
}
