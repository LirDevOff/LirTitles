package org.lirdev.lirtitles.commands.implementation.admin;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import org.lirdev.lirtitles.commands.api.Subcommand;
import org.lirdev.lirtitles.configuration.Configuration;
import org.lirdev.lirtitles.menu.MenuCloser;
import org.lirdev.lirtitles.permissions.Permissions;

public final class ReloadCommand implements Subcommand {
    private static final String PERMISSION = Permissions.ADMIN;
    private final JavaPlugin plugin;
    private final Configuration configuration;
    private final MenuCloser menuCloser;
    private final Runnable clearConfigurationCaches;

    public ReloadCommand(JavaPlugin plugin, Configuration configuration, MenuCloser menuCloser, Runnable clearConfigurationCaches) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.menuCloser = Objects.requireNonNull(menuCloser, "menuCloser");
        this.clearConfigurationCaches = Objects.requireNonNull(clearConfigurationCaches, "clearConfigurationCaches");
    }

    @Override
    public boolean isAvailableTo(CommandSourceStack source) {
        return source.getSender().hasPermission(PERMISSION);
    }

    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("reload")
                .requires(this::isAvailableTo)
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    configuration.loadAsync().whenComplete((result, exception) -> complete(sender, result, exception));
                    return 1;
                }));
    }

    private void complete(CommandSender sender, Configuration.ReloadResult result, Throwable exception) {
        Component message;
        if (exception != null) {
            Throwable cause = exception instanceof CompletionException && exception.getCause() != null
                    ? exception.getCause()
                    : exception;
            message = Component.text("Could not reload the configuration, the previous configuration is still active: "
                    + cause.getMessage());
        } else {
            if (result.changed()) {
                clearConfigurationCaches.run();
            }
            message = configuration.message(configuration.messages().reloadSuccess(),
                    Map.of("time", Component.text(formatElapsed(result.durationNanos()))));
        }
        if (sender instanceof Player player) {
            player.getScheduler().run(plugin, task -> sender.sendMessage(message), null);
            if (exception == null && result.changed()) {
                plugin.getServer().getGlobalRegionScheduler().execute(plugin, menuCloser::closeOpenMenus);
            }
        } else {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
                if (exception == null && result.changed()) {
                    menuCloser.closeOpenMenus();
                }
                sender.sendMessage(message);
            });
        }
    }

    private static String formatElapsed(long elapsedNanos) {
        return String.format(Locale.ROOT, "%.2f", elapsedNanos / 1_000_000.0);
    }
}
