package org.lirdev.lirtitles.configuration.menus.colors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;
import org.lirdev.lirtitles.utilities.HexColor;

public record PaletteColor(String id, HexColor hex, MenuItem item, List<PaletteColor> shades) {
    public PaletteColor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(hex, "hex");
        Objects.requireNonNull(item, "item");
        shades = List.copyOf(shades);
    }

    public static List<PaletteColor> loadList(MenuItemLoader loader, List<?> rawEntries, String path) {
        List<PaletteColor> colors = new ArrayList<>();
        for (Object entry : rawEntries) {
            if (!(entry instanceof Map<?, ?> data)) {
                throw new IllegalStateException("Entries in '" + path + "' must be mappings");
            }
            colors.add(load(loader, data, path));
        }
        return List.copyOf(colors);
    }

    private static PaletteColor load(MenuItemLoader loader, Map<?, ?> data, String path) {
        String id = requiredString(data, "id", path);
        HexColor hex = HexColor.parse(requiredString(data, "hex", path))
                .orElseThrow(() -> new IllegalStateException("Invalid 'hex' value for '" + id + "' in '" + path + "'"));
        Map<String, Object> normalized = new LinkedHashMap<>();
        data.forEach((key, value) -> normalized.put(String.valueOf(key), value));
        normalized.putIfAbsent("material", "LEATHER_CHESTPLATE");
        normalized.computeIfAbsent("display_name", key -> "<" + hex.hex() + ">" + id + " {selected} {hex}");
        MenuItem item = loader.itemFromMap(normalized);
        return new PaletteColor(id, hex, item, loadShades(loader, normalized, path, id));
    }

    private static List<PaletteColor> loadShades(MenuItemLoader loader, Map<String, Object> data, String path, String id) {
        if (!(data.get("shades") instanceof List<?> rawShades) || rawShades.isEmpty()) {
            return List.of();
        }
        return loadList(loader, rawShades, path + "." + id + ".shades");
    }

    private static String requiredString(Map<?, ?> data, String key, String path) {
        Object value = data.get(key);
        if (!(value instanceof String string) || string.isBlank()) {
            throw new IllegalStateException("Missing '" + key + "' for an entry in '" + path + "'");
        }
        return string;
    }
}
