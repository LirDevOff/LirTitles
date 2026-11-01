package org.lirdev.lirtitles.commands.implementation.moderation;

import java.util.Objects;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import org.lirdev.lirtitles.commands.api.Subcommand;
import org.lirdev.lirtitles.configuration.Configuration;
import org.lirdev.lirtitles.menu.menus.TitleMenuController;
import org.lirdev.lirtitles.permissions.Permissions;

public final class OpenModerationMenuCommand implements Subcommand {
    private static final String PERMISSION = Permissions.MODERATE;
    private final Configuration configuration;
    private final TitleMenuController menus;

    public OpenModerationMenuCommand(Configuration configuration, TitleMenuController menus) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.menus = Objects.requireNonNull(menus, "menus");
    }

    @Override
    public boolean isAvailableTo(CommandSourceStack source) {
        return source.getSender().hasPermission(PERMISSION);
    }

    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("moderation").requires(this::isAvailableTo).executes(context -> {
            if (!(context.getSource().getSender() instanceof Player player)) {
                context.getSource().getSender().sendMessage(configuration.message(configuration.messages().playerOnly()));
                return 0;
            }
            menus.openModerationQueue(player);
            return 1;
        }));
    }
}
