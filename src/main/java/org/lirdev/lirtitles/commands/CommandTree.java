package org.lirdev.lirtitles.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.lirdev.lirtitles.commands.api.Subcommand;
import org.lirdev.lirtitles.configuration.Configuration;

import com.mojang.brigadier.arguments.StringArgumentType;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class CommandTree {
    private final String name;
    private final List<String> aliases;
    private final Configuration configuration;
    private final List<Subcommand> subcommands = new ArrayList<>();

    public CommandTree(String name, List<String> aliases, Configuration configuration) {
        this.name = requireLiteral(name, "name");
        this.aliases = List.copyOf(aliases);
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public void add(Subcommand... commands) {
        for (Subcommand command : commands) {
            subcommands.add(Objects.requireNonNull(command, "command"));
        }
    }

    public void registerWith(Commands commands) {
        Objects.requireNonNull(commands, "commands");
        var root = Commands.literal(name).executes(context -> sendHelp(context.getSource()));
        subcommands.forEach(subcommand -> subcommand.register(root));
        root.then(Commands.argument("input", StringArgumentType.greedyString()).executes(context -> sendHelp(context.getSource())));
        commands.register(root.build(), aliases);
    }

    public int sendHelp(CommandSourceStack source) {
        source.getSender().sendMessage(configuration.message(configuration.messages().help()));
        return 1;
    }

    private static String requireLiteral(String value, String name) {
        String literal = Objects.requireNonNull(value, name).trim();
        if (literal.isEmpty() || literal.contains(" ")) {
            throw new IllegalArgumentException(name + " must be a non-empty command literal");
        }
        return literal;
    }
}
