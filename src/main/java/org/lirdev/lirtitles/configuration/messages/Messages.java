package org.lirdev.lirtitles.configuration.messages;

import java.util.List;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;

import org.lirdev.lirtitles.utilities.Branding;
import org.lirdev.lirtitles.utilities.Colorizer;

public record Messages(
        List<Component> help,
        Component reloadSuccess,
        Component titleGiven,
        Component tokensGiven,
        Component noTargetsFound,
        Component titleSelected,
        Component titleAlreadySelected,
        Component noAvailableTitles,
        Component customTitleCreated,
        Component customTitleDeleted,
        Component customTitleInputPrompt,
        Component noCustomTitleTokens,
        Component customTitleInvalidLength,
        Component customTitleInvalidFormat,
        Component customTitleMissingText,
        Component customTitleNotApproved,
        Component customColorInputPrompt,
        Component customColorInvalidFormat,
        Component moderationReasonPrompt,
        Component moderationApproved,
        Component moderationRejected,
        Component moderationNoReasonFallback,
        Component moderationRejectionNotice,
        Component playerOnly,
        Component titleNotFound
) {
    public Messages {
        help = List.copyOf(requireNonEmpty(help, "help"));
        Objects.requireNonNull(reloadSuccess, "reloadSuccess");
        Objects.requireNonNull(titleGiven, "titleGiven");
        Objects.requireNonNull(tokensGiven, "tokensGiven");
        Objects.requireNonNull(noTargetsFound, "noTargetsFound");
        Objects.requireNonNull(titleSelected, "titleSelected");
        Objects.requireNonNull(titleAlreadySelected, "titleAlreadySelected");
        Objects.requireNonNull(noAvailableTitles, "noAvailableTitles");
        Objects.requireNonNull(customTitleCreated, "customTitleCreated");
        Objects.requireNonNull(customTitleDeleted, "customTitleDeleted");
        Objects.requireNonNull(customTitleInputPrompt, "customTitleInputPrompt");
        Objects.requireNonNull(noCustomTitleTokens, "noCustomTitleTokens");
        Objects.requireNonNull(customTitleInvalidLength, "customTitleInvalidLength");
        Objects.requireNonNull(customTitleInvalidFormat, "customTitleInvalidFormat");
        Objects.requireNonNull(customTitleMissingText, "customTitleMissingText");
        Objects.requireNonNull(customTitleNotApproved, "customTitleNotApproved");
        Objects.requireNonNull(customColorInputPrompt, "customColorInputPrompt");
        Objects.requireNonNull(customColorInvalidFormat, "customColorInvalidFormat");
        Objects.requireNonNull(moderationReasonPrompt, "moderationReasonPrompt");
        Objects.requireNonNull(moderationApproved, "moderationApproved");
        Objects.requireNonNull(moderationRejected, "moderationRejected");
        Objects.requireNonNull(moderationNoReasonFallback, "moderationNoReasonFallback");
        Objects.requireNonNull(moderationRejectionNotice, "moderationRejectionNotice");
        Objects.requireNonNull(playerOnly, "playerOnly");
        Objects.requireNonNull(titleNotFound, "titleNotFound");
    }

    public static Messages read(FileConfiguration config, Colorizer colorizer) {
        return new Messages(
                requiredComponents(config, colorizer, "messages.commands.help"),
                required(config, colorizer, "messages.commands.admin.reload-success"),
                required(config, colorizer, "messages.commands.admin.title-given"),
                required(config, colorizer, "messages.commands.admin.tokens-given"),
                required(config, colorizer, "messages.commands.admin.no-targets-found"),
                required(config, colorizer, "messages.titles.selected"),
                required(config, colorizer, "messages.titles.already-selected"),
                required(config, colorizer, "messages.titles.no-available-in-category"),
                required(config, colorizer, "messages.custom-titles.created", "{prefix} <" + primary() + ">Custom title <"
                        + secondary() + ">'{title}<" + secondary() + ">' <" + primary() + ">has been created."),
                required(config, colorizer, "messages.custom-titles.deleted", "{prefix} <" + primary() + ">Custom title <"
                        + secondary() + ">'{title}<" + secondary() + ">' <" + primary() + ">has been deleted."),
                required(config, colorizer, "messages.custom-titles.input-prompt"),
                required(config, colorizer, "messages.custom-titles.errors.no-tokens"),
                required(config, colorizer, "messages.custom-titles.errors.invalid-length"),
                required(config, colorizer, "messages.custom-titles.errors.invalid-format"),
                required(config, colorizer, "messages.custom-titles.errors.missing-text"),
                required(config, colorizer, "messages.custom-titles.errors.not-approved"),
                required(config, colorizer, "messages.custom-colors.input-prompt"),
                required(config, colorizer, "messages.custom-colors.errors.invalid-format"),
                required(config, colorizer, "messages.moderation.reason-prompt"),
                required(config, colorizer, "messages.moderation.approved"),
                required(config, colorizer, "messages.moderation.rejected"),
                required(config, colorizer, "messages.moderation.no-reason-fallback"),
                required(config, colorizer, "messages.moderation.rejection-notice"),
                required(config, colorizer, "messages.errors.player-only"),
                required(config, colorizer, "messages.errors.title-not-found")
        );
    }

    private static String primary() {
        return Branding.PRIMARY.asHexString();
    }

    private static String secondary() {
        return Branding.SECONDARY.asHexString();
    }

    private static Component required(FileConfiguration configuration, Colorizer colorizer, String path) {
        String value = configuration.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing non-blank string configuration value '" + path + "'");
        }
        return colorizer.colorize(value);
    }

    private static Component required(FileConfiguration configuration, Colorizer colorizer, String path, String fallback) {
        String value = configuration.getString(path, fallback);
        return colorizer.colorize(value == null || value.isBlank() ? fallback : value);
    }

    private static List<Component> requiredComponents(FileConfiguration configuration, Colorizer colorizer, String path) {
        return requireNonEmpty(configuration.getStringList(path), path).stream().map(colorizer::colorize).toList();
    }

    private static <T> List<T> requireNonEmpty(List<T> values, String path) {
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("Missing non-empty list configuration value '" + path + "'");
        }
        return values;
    }
}
