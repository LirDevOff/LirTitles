package org.lirdev.lirtitles.commands;

import java.util.List;

import io.papermc.paper.command.brigadier.Commands;

import org.lirdev.lirtitles.commands.implementation.player.OpenTitlesMenuCommand;

public final class TitlesCommandRegistrar {
    private static final List<String> ALIASES = List.of("tags");

    private TitlesCommandRegistrar() {
    }

    public static void register(Commands commands, OpenTitlesMenuCommand openTitlesMenu) {
        commands.register(Commands.literal("titles")
                        .executes(context -> openTitlesMenu.execute(context.getSource()))
                        .build(),
                ALIASES);
    }
}
