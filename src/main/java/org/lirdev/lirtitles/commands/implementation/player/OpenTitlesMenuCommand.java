package org.lirdev.lirtitles.commands.implementation.player;

import java.util.Objects;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import org.lirdev.lirtitles.configuration.Configuration;
import org.lirdev.lirtitles.menu.menus.TitleMenuController;

public final class OpenTitlesMenuCommand {
    private final Configuration configuration;
    private final TitleMenuController menus;

    public OpenTitlesMenuCommand(Configuration configuration, TitleMenuController menus) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.menus = Objects.requireNonNull(menus, "menus");
    }

    public int execute(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(configuration.message(configuration.messages().playerOnly()));
            return 0;
        }
        menus.openCategories(player);
        return 1;
    }
}