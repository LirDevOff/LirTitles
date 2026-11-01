package org.lirdev.lirtitles.menu.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.configuration.ConfigurationSection;

public final class ConfiguredMenuRequirements {
    private ConfiguredMenuRequirements() {
    }

    public static List<MenuRequirement> read(ConfigurationSection item, String path) {
        List<MenuRequirement> requirements = new ArrayList<>();
        for (String definition : item.getStringList(path)) {
            requirements.add(read(definition, path));
        }
        return List.copyOf(requirements);
    }

    private static MenuRequirement read(String definition, String path) {
        int closingBracket = definition.indexOf(']');
        if (!definition.startsWith("[") || closingBracket < 2) {
            throw new IllegalStateException("Menu requirement in '" + path + "' must start with [type]");
        }
        String type = definition.substring(1, closingBracket).toLowerCase(Locale.ROOT);
        String value = definition.substring(closingBracket + 1).trim();
        return switch (type) {
            case "permission" -> permission(value, path);
            default -> throw new IllegalStateException("Unknown menu requirement type '" + type + "'");
        };
    }

    private static MenuRequirement permission(String value, String path) {
        boolean negated = value.startsWith("!");
        String node = (negated ? value.substring(1) : value).trim();
        if (node.isEmpty()) {
            throw new IllegalStateException("Menu requirement [permission] in '" + path + "' requires a value");
        }
        return negated ? player -> !player.hasPermission(node) : player -> player.hasPermission(node);
    }
}
