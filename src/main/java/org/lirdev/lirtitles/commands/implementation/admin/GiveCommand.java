package org.lirdev.lirtitles.commands.implementation.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.commands.api.Subcommand;
import org.lirdev.lirtitles.configuration.Configuration;
import org.lirdev.lirtitles.configuration.titles.Titles;
import org.lirdev.lirtitles.permissions.Permissions;

public final class GiveCommand implements Subcommand {
    private static final String PERMISSION = Permissions.ADMIN;
    private final LirTitles plugin;
    private final Configuration configuration;

    public GiveCommand(LirTitles plugin, Configuration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
    }

    @Override
    public boolean isAvailableTo(CommandSourceStack source) {
        return source.getSender().hasPermission(PERMISSION);
    }

    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("give").requires(this::isAvailableTo)
                .then(Commands.argument("targets", ArgumentTypes.entities())
                        .then(Commands.literal("title")
                                .then(Commands.argument("title", StringArgumentType.word())
                                        .suggests(this::suggestMissingTitles)
                                        .executes(context -> giveTitle(context.getSource(),
                                                context.getArgument("targets", EntitySelectorArgumentResolver.class),
                                                StringArgumentType.getString(context, "title")))))
                        .then(Commands.literal("token")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> giveTokens(context.getSource(),
                                                context.getArgument("targets", EntitySelectorArgumentResolver.class),
                                                IntegerArgumentType.getInteger(context, "amount")))))));
    }

    private CompletableFuture<Suggestions> suggestMissingTitles(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<UUID> targetIds = resolveTargets(context).stream().map(Player::getUniqueId).toList();
        CompletableFuture<?>[] preloads = targetIds.stream()
                .map(plugin.titleStorage()::preloadAsync)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(preloads).thenCompose(ignored -> {
            Set<String> commonlyOwned = commonlyOwnedTitles(targetIds);
            for (String key : configuration.titles().titleKeys()) {
                if (!commonlyOwned.contains(key)) {
                    builder.suggest(key);
                }
            }
            return builder.buildFuture();
        });
    }

    private Set<String> commonlyOwnedTitles(List<UUID> targetIds) {
        if (targetIds.isEmpty()) {
            return Set.of();
        }
        Set<String> commonlyOwned = new HashSet<>(plugin.titleStorage().owned(targetIds.get(0)));
        for (UUID targetId : targetIds.subList(1, targetIds.size())) {
            commonlyOwned.retainAll(plugin.titleStorage().owned(targetId));
        }
        return commonlyOwned;
    }

    private List<Player> resolveTargets(CommandContext<CommandSourceStack> context) {
        try {
            EntitySelectorArgumentResolver targets = context.getArgument("targets", EntitySelectorArgumentResolver.class);
            return resolvePlayers(context.getSource(), targets);
        } catch (CommandSyntaxException | IllegalArgumentException exception) {
            return List.of();
        }
    }

    private int giveTitle(CommandSourceStack source, EntitySelectorArgumentResolver targets, String titleKey) throws CommandSyntaxException {
        Titles.Title title = configuration.titles().find(titleKey);
        if (title == null) {
            source.getSender().sendMessage(configuration.message(configuration.messages().titleNotFound(), Map.of("id", Component.text(titleKey))));
            return 0;
        }
        List<Player> players = requireTargets(source, targets);
        for (Player player : players) {
            plugin.titleStorage().give(player.getUniqueId(), title.key());
            source.getSender().sendMessage(configuration.message(configuration.messages().titleGiven(),
                    Map.of("title", title.displayName(), "player", Component.text(player.getName()))));
        }
        return players.size();
    }

    private int giveTokens(CommandSourceStack source, EntitySelectorArgumentResolver targets, int amount) throws CommandSyntaxException {
        List<Player> players = requireTargets(source, targets);
        for (Player player : players) {
            plugin.titleStorage().addCustomTitleTokens(player.getUniqueId(), amount);
            source.getSender().sendMessage(configuration.message(configuration.messages().tokensGiven(),
                    Map.of("amount", Component.text(amount), "player", Component.text(player.getName()))));
        }
        return players.size();
    }

    private List<Player> requireTargets(CommandSourceStack source, EntitySelectorArgumentResolver targets) throws CommandSyntaxException {
        List<Player> players = resolvePlayers(source, targets);
        if (players.isEmpty()) {
            throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
                    .serialize(configuration.message(configuration.messages().noTargetsFound()))).create();
        }
        return players;
    }

    private static List<Player> resolvePlayers(CommandSourceStack source, EntitySelectorArgumentResolver targets) throws CommandSyntaxException {
        return targets.resolve(source).stream().filter(Player.class::isInstance).map(Player.class::cast).toList();
    }
}
