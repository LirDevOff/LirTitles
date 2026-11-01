package org.lirdev.lirtitles.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;


public final class YamlConfigurationCache {
    private static final Map<String, String> LEGACY_MENU_PATHS = Map.ofEntries(
            Map.entry("menus/titles/categories.yml", "menus/categories.yml"),
            Map.entry("menus/titles/selection.yml", "menus/title-selection.yml"),
            Map.entry("menus/titles/confirmation.yml", "menus/title-apply.yml"),
            Map.entry("menus/custom-titles/list.yml", "menus/custom-titles.yml"),
            Map.entry("menus/custom-titles/editor.yml", "menus/custom-title-editor.yml"),
            Map.entry("menus/custom-titles/input.yml", "menus/custom-title-input.yml"),
            Map.entry("menus/custom-titles/delete-confirmation.yml", "menus/custom-title-delete.yml"),
            Map.entry("menus/colors/palette.yml", "menus/color-selection.yml"),
            Map.entry("menus/colors/shades.yml", "menus/color-shades.yml"),
            Map.entry("menus/moderation/queue.yml", "menus/moderation.yml"));
    private final JavaPlugin plugin;
    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    public YamlConfigurationCache(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public FileConfiguration load(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        copyLegacyMenuFile(resourcePath, file);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }

        Entry cached = entries.get(resourcePath);
        if (cached != null) {
            return parse(resourcePath, cached);
        }
        try {
            String source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return parse(resourcePath, new Entry(source, null));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load configuration file '" + resourcePath + "'", exception);
        }
    }

    private FileConfiguration parse(String resourcePath, Entry entry) {
        if (entry.configuration() != null) {
            return entry.configuration();
        }
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(entry.source());
            entries.put(resourcePath, new Entry(entry.source(), configuration));
            return configuration;
        } catch (org.bukkit.configuration.InvalidConfigurationException exception) {
            throw new IllegalStateException("Could not load configuration file '" + resourcePath + "'", exception);
        }
    }

    private void copyLegacyMenuFile(String resourcePath, File target) {
        String legacyPath = LEGACY_MENU_PATHS.get(resourcePath);
        if (target.exists() || legacyPath == null) {
            return;
        }
        File legacy = new File(plugin.getDataFolder(), legacyPath);
        if (!legacy.isFile()) {
            return;
        }
        try {
            Files.createDirectories(target.toPath().getParent());
            Files.copy(legacy.toPath(), target.toPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not migrate menu configuration from '" + legacy + "' to '" + target + "'", exception);
        }
    }

    public Set<String> findChanges() {
        Set<String> changed = new LinkedHashSet<>();
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            File file = new File(plugin.getDataFolder(), entry.getKey());
            try {
                String source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                if (!source.equals(entry.getValue().source())) {
                    entries.put(entry.getKey(), new Entry(source, null));
                    changed.add(entry.getKey());
                }
            } catch (IOException exception) {
                entries.remove(entry.getKey());
                changed.add(entry.getKey());
            }
        }
        return Set.copyOf(changed);
    }

    public void clear() {
        entries.clear();
    }

    private record Entry(String source, FileConfiguration configuration) {
    }
}
