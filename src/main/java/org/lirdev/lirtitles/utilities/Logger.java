package org.lirdev.lirtitles.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Logger {
    private static final Component INFO_LABEL = label("INFO");
    private static final Component WARN_LABEL = label("WARN");
    private static final Component ERROR_LABEL = Component.text("ERROR → ", NamedTextColor.WHITE);

    private final Component prefix;

    public Logger(JavaPlugin plugin) {
        String pluginName = Objects.requireNonNull(plugin, "plugin").getPluginMeta().getName();
        this.prefix = Component.textOfChildren(
                Component.text(pluginName, Branding.PRIMARY, TextDecoration.BOLD),
                Component.space(),
                Component.text("→", NamedTextColor.GRAY),
                Component.space());
    }

    public void raw(Component message) {
        console().sendMessage(message);
    }

    public void raw(String message) {
        raw(Component.text(message));
    }

    public void player(Player player, Component message) {
        Objects.requireNonNull(player, "player").sendMessage(
                Component.textOfChildren(prefix, INFO_LABEL, Objects.requireNonNull(message, "message")));
    }

    public void player(Player player, String message) {
        player(player, Component.text(message));
    }

    public void info(Component message) {
        log(INFO_LABEL, message);
    }

    public void info(String message) {
        info(Component.text(message));
    }

    public void warn(Component message) {
        log(WARN_LABEL, message);
    }

    public void warn(String message) {
        warn(Component.text(message));
    }

    public void warn(String message, Throwable cause) {
        warn(message);
        logStackTrace(cause);
    }

    public void error(Component message) {
        log(ERROR_LABEL, message);
    }

    public void error(String message) {
        error(Component.text(message));
    }

    public void error(String message, Throwable cause) {
        error(message);
        logStackTrace(cause);
    }

    private static Component label(String name) {
        return Component.textOfChildren(
                Component.text(name, NamedTextColor.WHITE),
                Component.text(":", NamedTextColor.GRAY),
                Component.space());
    }

    private void log(Component label, Component message) {
        raw(Component.textOfChildren(prefix, label, message));
    }

    private void logStackTrace(Throwable cause) {
        StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));
        for (String line : writer.toString().split("\n")) {
            raw(Component.text(line));
        }
    }

    private static CommandSender console() {
        return Bukkit.getConsoleSender();
    }
}
