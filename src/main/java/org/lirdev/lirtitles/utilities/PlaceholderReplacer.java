package org.lirdev.lirtitles.utilities;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class PlaceholderReplacer {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)}");

    private PlaceholderReplacer() {
    }

    public static Component replace(Component template, Map<String, ? extends ComponentLike> values) {
        if (values.isEmpty()) {
            return template;
        }
        return template.replaceText(TextReplacementConfig.builder()
                .match(PLACEHOLDER)
                .replacement((match, builder) -> {
                    ComponentLike value = values.get(match.group(1));
                    return value == null ? builder : value;
                })
                .build());
    }

    public static Optional<String> wholeLinePlaceholder(Component line) {
        String text = PlainTextComponentSerializer.plainText().serialize(line).trim();
        Matcher matcher = PLACEHOLDER.matcher(text);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
