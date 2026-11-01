package org.lirdev.lirtitles.fonts;

import java.util.Map;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public final class FontTextRenderer {
    private FontTextRenderer() {
    }

    public static Component render(TitleFont font, Component text) {
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(text, "text");
        return font.characters().isEmpty() ? text : convert(font.characters(), text);
    }

    private static Component convert(Map<Character, String> characters, Component component) {
        Component converted = component;
        if (component instanceof TextComponent textComponent) {
            String content = replaceCharacters(characters, textComponent.content());
            if (content != null) {
                converted = textComponent.content(content);
            }
        }

        var children = converted.children();
        if (children.isEmpty()) {
            return converted;
        }

        java.util.ArrayList<Component> convertedChildren = null;
        for (int index = 0; index < children.size(); index++) {
            Component child = children.get(index);
            Component convertedChild = convert(characters, child);
            if (convertedChildren != null) {
                convertedChildren.add(convertedChild);
            } else if (convertedChild != child) {
                convertedChildren = new java.util.ArrayList<>(children.size());
                convertedChildren.addAll(children.subList(0, index));
                convertedChildren.add(convertedChild);
            }
        }
        return convertedChildren == null ? converted : converted.children(convertedChildren);
    }

    private static String replaceCharacters(Map<Character, String> characters, String text) {
        StringBuilder result = null;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            String replacement = characters.get(character);
            if (replacement != null) {
                if (result == null) {
                    result = new StringBuilder(text.length());
                    result.append(text, 0, index);
                }
                result.append(replacement);
            } else if (result != null) {
                result.append(character);
            }
        }
        return result == null ? null : result.toString();
    }
}
