package org.lirdev.lirtitles.commands.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;

public interface Subcommand {
    default boolean isAvailableTo(CommandSourceStack source) {
        return true;
    }

    void register(LiteralArgumentBuilder<CommandSourceStack> root);
}