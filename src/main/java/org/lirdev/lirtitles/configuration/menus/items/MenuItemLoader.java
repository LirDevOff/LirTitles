package org.lirdev.lirtitles.configuration.menus.items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import org.lirdev.lirtitles.configuration.YamlConfigurationCache;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.action.ConfiguredMenuActions;
import org.lirdev.lirtitles.menu.action.ConfiguredMenuRequirements;
import org.lirdev.lirtitles.utilities.Colorizer;

public final class MenuItemLoader {
    private static final int DEFAULT_ROWS = 3;
    private static final int MAX_SLOT = ChestMenu.MAX_ROWS * ChestMenu.ROW_WIDTH - 1;

    private final JavaPlugin plugin;
    private final Colorizer colorizer;
    private final ConfiguredMenuActions actions;
    private final YamlConfigurationCache yamlCache;

    public MenuItemLoader(JavaPlugin plugin, Colorizer colorizer, YamlConfigurationCache yamlCache) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.colorizer = Objects.requireNonNull(colorizer, "colorizer");
        this.yamlCache = Objects.requireNonNull(yamlCache, "yamlCache");
        this.actions = new ConfiguredMenuActions(plugin, colorizer);
    }

    public ConfigurationSection loadFile(String resourcePath) {
        return yamlCache.load(resourcePath);
    }

    public static int rows(ConfigurationSection section) {
        int size = section.getInt("size", DEFAULT_ROWS * ChestMenu.ROW_WIDTH);
        if (size < ChestMenu.ROW_WIDTH || size > ChestMenu.MAX_ROWS * ChestMenu.ROW_WIDTH || size % ChestMenu.ROW_WIDTH != 0) {
            throw new IllegalStateException("Menu size must be a multiple of " + ChestMenu.ROW_WIDTH + " between "
                    + ChestMenu.ROW_WIDTH + " and " + ChestMenu.MAX_ROWS * ChestMenu.ROW_WIDTH);
        }
        return size / ChestMenu.ROW_WIDTH;
    }

    public Component text(ConfigurationSection section, String path, String fallback) {
        return colorizer.colorize(section.getString(path, fallback));
    }

    public String menuId(ConfigurationSection yaml, String resourcePath) {
        String id = yaml.getString("id", "");
        if (id.isBlank()) {
            throw new IllegalStateException("Menu file '" + resourcePath + "' is missing a required 'id'");
        }
        return id;
    }

    public MenuItem item(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) throw new IllegalStateException("Missing menu item '" + path + "'");
        return item(section);
    }

    public MenuItem optionalItem(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        return section == null ? null : item(section);
    }

    private MenuItem item(ConfigurationSection section) {
        String configuredMaterial = section.getString("material", "BARRIER");
        String customMaterial = isCustomMaterial(configuredMaterial) ? configuredMaterial : "";
        Material material = customMaterial.isEmpty() ? Material.matchMaterial(configuredMaterial) : Material.BARRIER;
        Integer customModelData = customModelData(section);
        String baseHead = section.getString("base-head", section.getString("basehead", ""));
        return new MenuItem(material == null ? Material.BARRIER : material, customMaterial, section.getInt("amount", 1), section.getInt("slot", MenuItem.NO_SLOT), slots(section),
                text(section, "display_name", section.getString("display-name", "")), lore(section),
                customModelData, section.getBoolean("glow", false), baseHead,
                actions.read(section, "click_commands"),
                actions.readAll(section),
                ConfiguredMenuRequirements.read(section, "view_requirement"),
                ConfiguredMenuRequirements.read(section, "click_requirement"),
                actions.read(section, "deny_commands"));
    }

    private static boolean isCustomMaterial(String material) {
        String normalized = material.toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("nexo-") || normalized.startsWith("oraxen-") || normalized.startsWith("itemsadder-")
                || normalized.startsWith("nexo:") || normalized.startsWith("oraxen:") || normalized.startsWith("itemsadder:");
    }

    public MenuItem itemFromMap(Map<?, ?> data) {
        MemoryConfiguration section = new MemoryConfiguration();
        data.forEach((key, value) -> section.set(String.valueOf(key), value));
        return item(section);
    }

    public List<MenuItem> itemList(ConfigurationSection parent, String path) {
        List<MenuItem> items = new ArrayList<>();
        for (Object entry : parent.getList(path, List.of())) {
            if (!(entry instanceof Map<?, ?> data)) {
                throw new IllegalStateException("Entries in '" + path + "' must be mappings");
            }
            items.add(itemFromMap(data));
        }
        return List.copyOf(items);
    }

    public List<MenuItem> staticItems(ConfigurationSection yaml, Set<String> reservedKeys) {
        ConfigurationSection items = section(yaml, "items");
        List<MenuItem> result = new ArrayList<>();
        for (String key : items.getKeys(false)) {
            if (reservedKeys.contains(key)) {
                continue;
            }
            MenuItem item = item(yaml, "items." + key);
            if (item.slots().isEmpty()) {
                throw new IllegalStateException("Static menu item '" + key + "' requires 'slot' or 'slots'");
            }
            result.add(item);
        }
        return List.copyOf(result);
    }

    private List<Component> lore(ConfigurationSection section) {
        return section.getStringList("lore").stream().map(colorizer::colorize).toList();
    }

    private static Integer customModelData(ConfigurationSection section) {
        if (section.contains("custom_model_data")) {
            return section.getInt("custom_model_data");
        }
        if (section.contains("custom-model-data")) {
            return section.getInt("custom-model-data");
        }
        return null;
    }

    private static List<Integer> slots(ConfigurationSection section) {
        Set<Integer> slots = new LinkedHashSet<>();
        if (section.contains("slot")) {
            addSlot(slots, section.getInt("slot"));
        }
        Object configuredSlots = section.get("slots");
        if (configuredSlots instanceof String slotRanges) {
            addSlotRanges(slots, slotRanges);
        } else if (configuredSlots instanceof List<?> slotList) {
            for (Object value : slotList) {
                if (!(value instanceof Number number) || number.doubleValue() != number.intValue()) {
                    throw new IllegalStateException("Menu item 'slots' must contain only whole numbers or slot ranges");
                }
                addSlot(slots, number.intValue());
            }
        } else if (configuredSlots != null) {
            throw new IllegalStateException("Menu item 'slots' must be a list of numbers or a comma-separated string");
        }
        return List.copyOf(slots);
    }

    private static void addSlotRanges(Set<Integer> slots, String configuredSlots) {
        for (String configuredRange : configuredSlots.split(",", -1)) {
            String range = configuredRange.trim();
            if (range.isEmpty()) {
                throw new IllegalStateException("Menu item 'slots' contains an empty slot or range");
            }
            int separator = range.indexOf('-');
            if (separator < 0) {
                addSlot(slots, parseSlot(range));
                continue;
            }
            if (separator == 0 || separator != range.lastIndexOf('-') || separator == range.length() - 1) {
                throw new IllegalStateException("Invalid menu item slot range '" + range + "'");
            }
            int first = parseSlot(range.substring(0, separator).trim());
            int last = parseSlot(range.substring(separator + 1).trim());
            if (first > last) {
                throw new IllegalStateException("Menu item slot range must be ascending: '" + range + "'");
            }
            for (int slot = first; slot <= last; slot++) {
                addSlot(slots, slot);
            }
        }
    }

    private static int parseSlot(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid menu item slot '" + value + "'", exception);
        }
    }

    private static void addSlot(Set<Integer> slots, int slot) {
        if (slot < 0 || slot > MAX_SLOT) {
            throw new IllegalStateException("Menu item slot must be between 0 and " + MAX_SLOT);
        }
        slots.add(slot);
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) throw new IllegalStateException("Missing configuration section '" + path + "'");
        return section;
    }
}
