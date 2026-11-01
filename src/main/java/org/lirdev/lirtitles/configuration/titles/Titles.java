package org.lirdev.lirtitles.configuration.titles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.lirdev.lirtitles.configuration.YamlConfigurationCache;
import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.utilities.Colorizer;

public final class Titles {
    private final Map<String, Category> categories;
    private final List<String> titleKeys;
    private final SlotMode slotMode;

    private Titles(SlotMode slotMode, Map<String, Category> categories) {
        this.slotMode = slotMode;
        this.titleKeys = categories.values().stream()
                .flatMap(category -> category.orderedTitles().stream())
                .map(Title::key)
                .toList();
        this.categories = Collections.unmodifiableMap(new LinkedHashMap<>(categories));
    }

    public static Titles load(Colorizer colorizer, YamlConfigurationCache yamlCache) {
        ConfigurationSection yaml = yamlCache.load("titles.yml");
        SlotMode slotMode = slotMode(yaml.getString("settings.slot-mode", "manual"));
        ConfigurationSection section = requiredSection(yaml, "categories");
        Map<String, Category> categories = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection category = section.getConfigurationSection(id);
            if (category == null) continue;
            Map<String, Title> entries = new LinkedHashMap<>();
            ConfigurationSection titlesSection = category.getConfigurationSection("titles");
            if (titlesSection != null) for (String titleId : titlesSection.getKeys(false)) {
                Object raw = titlesSection.get(titleId);
                if (raw instanceof String value)
                    entries.put(titleId, new Title(id, titleId, colorizer.colorize(value), MenuItem.NO_SLOT));
                else {
                    ConfigurationSection title = titlesSection.getConfigurationSection(titleId);
                    if (title != null) entries.put(titleId, new Title(id, titleId,
                            colorizer.colorize(title.getString("display_name", titleId)), title.getInt("slot", MenuItem.NO_SLOT)));
                }
            }
            for (String titleId : category.getKeys(false)) {
                if (List.of("name", "display_name", "material", "slot", "titles").contains(titleId)) continue;
                String value = category.getString(titleId);
                if (value != null)
                    entries.put(titleId, new Title(id, titleId, colorizer.colorize(value), MenuItem.NO_SLOT));
            }
            Map<String, Title> immutableEntries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
            categories.put(id, new Category(id, colorizer.colorize(category.getString("display_name", category.getString("name", id))),
                    category.getString("material", "BOOK"), category.getInt("slot", MenuItem.NO_SLOT), immutableEntries, List.copyOf(entries.values())));
        }
        return new Titles(slotMode, categories);
    }

    private static SlotMode slotMode(String configuredMode) {
        try {
            return SlotMode.valueOf(configuredMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("titles.yml setting 'settings.slot-mode' must be 'manual' or 'range'", exception);
        }
    }

    public Map<String, Category> categories() {
        return categories;
    }

    public SlotMode slotMode() {
        return slotMode;
    }

    public Title find(String id) {
        if (id == null) {
            return null;
        }
        int separator = id.indexOf('.');
        if (separator <= 0 || separator == id.length() - 1) {
            return null;
        }
        Category category = categories.get(id.substring(0, separator));
        return category == null ? null : category.titles().get(id.substring(separator + 1));
    }

    public List<String> titleKeys() {
        return titleKeys;
    }

    private static ConfigurationSection requiredSection(ConfigurationSection source, String path) {
        ConfigurationSection result = source.getConfigurationSection(path);
        if (result == null)
            throw new IllegalStateException("Missing configuration section '" + path + "' in titles.yml");
        return result;
    }

    public enum SlotMode {
        MANUAL,
        RANGE
    }

    public record Category(String id, Component name, String material, int slot, Map<String, Title> titles,
                           List<Title> orderedTitles) {
    }

    public record Title(String categoryId, String id, Component displayName, int slot) {
        public String key() {
            return categoryId + "." + id;
        }
    }
}
