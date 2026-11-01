package org.lirdev.lirtitles.menu.action;

import java.util.List;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;

public final class MenuClickActions {
    private MenuClickActions() {
    }

    public static MenuClickAction sequence(MenuClickAction... actions) {
        List<MenuClickAction> sequence = List.of(actions);
        return context -> {
            try {
                for (MenuClickAction action : sequence) {
                    action.execute(context);
                }
            } catch (SequenceHalted halted) {
                return;
            }
        };
    }

    public static MenuClickAction closeMenu() {
        return context -> context.player().closeInventory();
    }

    public static MenuClickAction openMenu(String name) {
        String menuName = requireNonBlank(name, "menu name");
        return context -> context.menuOpener().open(context.player(), menuName);
    }

    public static MenuClickAction runPlayerCommand(String command) {
        String normalizedCommand = normalizeCommand(command);
        return context -> context.player().performCommand(normalizedCommand);
    }

    public static MenuClickAction runConsoleCommand(Plugin plugin, String command) {
        Plugin owner = Objects.requireNonNull(plugin, "plugin");
        String normalizedCommand = normalizeCommand(command);
        return context -> {
            String resolvedCommand = normalizedCommand.replace("{player}", context.player().getName());
            Bukkit.getGlobalRegionScheduler().execute(owner, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolvedCommand));
        };
    }

    public static MenuClickAction sendMessage(Component template) {
        Component messageTemplate = Objects.requireNonNull(template, "message");
        return context -> context.player().sendMessage(context.messageRenderer().render(messageTemplate));
    }

    public static MenuClickAction broadcastMessage(Plugin plugin, Component template) {
        Plugin owner = Objects.requireNonNull(plugin, "plugin");
        Component messageTemplate = Objects.requireNonNull(template, "message");
        return context -> Bukkit.getOnlinePlayers().forEach(player -> player.getScheduler()
                .run(owner, scheduledTask -> player.sendMessage(context.messageRenderer().render(messageTemplate)), null));
    }

    public static MenuClickAction playSound(Sound sound, float volume, float pitch) {
        Sound playerSound = Objects.requireNonNull(sound, "sound");
        if (volume < 0 || pitch < 0) {
            throw new IllegalArgumentException("Sound volume and pitch must not be negative");
        }
        return context -> context.player().playSound(context.player().getLocation(), playerSound, volume, pitch);
    }

    public static MenuClickAction requirePermission(String permission) {
        String node = requireNonBlank(permission, "permission");
        return context -> {
            if (!context.player().hasPermission(node)) {
                throw SequenceHalted.INSTANCE;
            }
        };
    }

    public static MenuClickAction delayed(Plugin plugin, long ticks, MenuClickAction action) {
        if (ticks < 1) {
            throw new IllegalArgumentException("Delay must be positive");
        }
        Plugin targetPlugin = Objects.requireNonNull(plugin, "plugin");
        MenuClickAction targetAction = Objects.requireNonNull(action, "action");
        return context -> context.player().getScheduler().runDelayed(targetPlugin, scheduledTask -> targetAction.execute(context), null, ticks);
    }

    private static String normalizeCommand(String command) {
        String normalizedCommand = Objects.requireNonNull(command, "command").trim();
        if (normalizedCommand.isEmpty()) {
            throw new IllegalArgumentException("Command must not be empty");
        }
        return normalizedCommand.startsWith("/") ? normalizedCommand.substring(1) : normalizedCommand;
    }

    private static String requireNonBlank(String value, String label) {
        String trimmed = Objects.requireNonNull(value, label).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
        return trimmed;
    }

    private static final class SequenceHalted extends RuntimeException {
        private static final SequenceHalted INSTANCE = new SequenceHalted();

        private SequenceHalted() {
            super(null, null, false, false);
        }
    }
}
