package org.lirdev.lirtitles.menu.action;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import org.lirdev.lirtitles.menu.MenuClickKind;
import org.lirdev.lirtitles.utilities.Colorizer;

public final class ConfiguredMenuActions {
    private static final Map<String, MenuClickKind> CLICK_ACTION_PATHS = Map.of(
            "left_click_commands", MenuClickKind.LEFT,
            "shift_left_click_commands", MenuClickKind.SHIFT_LEFT,
            "right_click_commands", MenuClickKind.RIGHT,
            "shift_right_click_commands", MenuClickKind.SHIFT_RIGHT,
            "middle_click_commands", MenuClickKind.MIDDLE,
            "double_click_commands", MenuClickKind.DOUBLE
    );

    private final Plugin plugin;
    private final Colorizer colorizer;
    private final Map<List<String>, List<MenuClickAction>> parsedDefinitions = new ConcurrentHashMap<>();

    public ConfiguredMenuActions(Plugin plugin, Colorizer colorizer) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.colorizer = Objects.requireNonNull(colorizer, "colorizer");
    }

    public Map<MenuClickKind, List<MenuClickAction>> readAll(ConfigurationSection item) {
        Map<MenuClickKind, List<MenuClickAction>> actions = new EnumMap<>(MenuClickKind.class);
        CLICK_ACTION_PATHS.forEach((path, kind) -> {
            List<MenuClickAction> parsed = read(item, path);
            if (!parsed.isEmpty()) {
                actions.put(kind, parsed);
            }
        });
        return Map.copyOf(actions);
    }

    public List<MenuClickAction> read(ConfigurationSection item, String path) {
        List<String> definitions = item.getStringList(path);
        if (definitions.isEmpty()) {
            return List.of();
        }
        List<String> key = List.copyOf(definitions);
        List<MenuClickAction> cached = parsedDefinitions.get(key);
        if (cached != null) {
            return cached;
        }
        List<MenuClickAction> parsed = List.of(build(definitions, 0, path));
        parsedDefinitions.putIfAbsent(key, parsed);
        return parsed;
    }

    private MenuClickAction build(List<String> definitions, int fromIndex, String path) {
        List<MenuClickAction> immediate = new ArrayList<>();
        for (int index = fromIndex; index < definitions.size(); index++) {
            Directive directive = parse(definitions.get(index), path);
            if ("delay".equals(directive.type())) {
                long ticks = delayTicks(directive.value(), path);
                MenuClickAction remainder = build(definitions, index + 1, path);
                immediate.add(MenuClickActions.delayed(plugin, ticks, remainder));
                return MenuClickActions.sequence(immediate.toArray(MenuClickAction[]::new));
            }
            immediate.add(read(directive, path));
        }
        return MenuClickActions.sequence(immediate.toArray(MenuClickAction[]::new));
    }

    private MenuClickAction read(Directive directive, String path) {
        return switch (directive.type()) {
            case "close" -> close(directive.value());
            case "player" -> MenuClickActions.runPlayerCommand(requiredValue(directive.value(), directive.type()));
            case "console" ->
                    MenuClickActions.runConsoleCommand(plugin, requiredValue(directive.value(), directive.type()));
            case "message" ->
                    MenuClickActions.sendMessage(colorizer.colorize(requiredValue(directive.value(), directive.type())));
            case "broadcast" ->
                    MenuClickActions.broadcastMessage(plugin, colorizer.colorize(requiredValue(directive.value(), directive.type())));
            case "sound" -> sound(directive.value());
            case "open_menu" -> MenuClickActions.openMenu(requiredValue(directive.value(), directive.type()));
            case "permission" -> MenuClickActions.requirePermission(requiredValue(directive.value(), directive.type()));
            default ->
                    throw new IllegalStateException("Unknown menu action type '" + directive.type() + "' in '" + path + "'");
        };
    }

    private static Directive parse(String definition, String path) {
        int closingBracket = definition.indexOf(']');
        if (!definition.startsWith("[") || closingBracket < 2) {
            throw new IllegalStateException("Menu action in '" + path + "' must start with [type]");
        }
        String type = definition.substring(1, closingBracket).toLowerCase(Locale.ROOT);
        String value = definition.substring(closingBracket + 1).trim();
        return new Directive(type, value);
    }

    private static MenuClickAction close(String value) {
        if (!value.isEmpty()) {
            throw new IllegalStateException("Menu action [close] does not accept a value");
        }
        return MenuClickActions.closeMenu();
    }

    private static MenuClickAction sound(String value) {
        String[] parts = requiredValue(value, "sound").split("\\s+");
        if (parts.length > 3) {
            throw new IllegalStateException("Menu action [sound] accepts sound, volume and pitch");
        }
        return MenuClickActions.playSound(parsedSound(parts[0]), number(parts, 1, 1F), number(parts, 2, 1F));
    }

    private static long delayTicks(String value, String path) {
        String ticksValue = requiredValue(value, "delay");
        try {
            long ticks = Long.parseLong(ticksValue);
            if (ticks < 1) {
                throw new IllegalStateException("Menu action [delay] in '" + path + "' must be a positive number of ticks");
            }
            return ticks;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Menu action [delay] in '" + path + "' must be a whole number of ticks", exception);
        }
    }

    private static String requiredValue(String value, String type) {
        if (value.isBlank()) {
            throw new IllegalStateException("Menu action [" + type + "] requires a value");
        }
        return value;
    }

    private static float number(String[] values, int index, float fallback) {
        if (index >= values.length) {
            return fallback;
        }
        try {
            return Float.parseFloat(values[index]);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Menu sound volume and pitch must be numbers", exception);
        }
    }

    private static Sound parsedSound(String value) {
        NamespacedKey key = NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
        Sound sound = key == null ? null : Registry.SOUNDS.get(key);
        if (sound == null) {
            throw new IllegalStateException("Unknown sound '" + value + "'");
        }
        return sound;
    }

    private record Directive(String type, String value) {
    }
}
