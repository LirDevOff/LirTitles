package org.lirdev.lirtitles.fonts;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record TitleFont(String id, String displayName, Map<Character, String> characters) {
    public TitleFont {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        characters = withUppercaseCharacters(Objects.requireNonNull(characters, "characters"));
    }

    private static Map<Character, String> withUppercaseCharacters(Map<Character, String> characters) {
        Map<Character, String> resolvedCharacters = new LinkedHashMap<>(characters);
        characters.forEach((character, replacement) -> {
            if (Character.isLowerCase(character)) {
                resolvedCharacters.putIfAbsent(Character.toUpperCase(character), replacement);
            }
        });
        return Map.copyOf(resolvedCharacters);
    }
}
