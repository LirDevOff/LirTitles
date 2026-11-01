package org.lirdev.lirtitles.fonts;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

final class FontFileReader {
    private FontFileReader() {
    }

    static TitleFont read(File file, String source) {
        try {
            JsonObject json = JsonParser.parseString(source).getAsJsonObject();
            return new TitleFont(requiredString(json, "id", file), requiredString(json, "display_name", file), characters(json, file));
        } catch (JsonParseException | IllegalStateException exception) {
            throw new IllegalStateException("Could not read font file '" + file.getName() + "': " + exception.getMessage(), exception);
        }
    }

    private static Map<Character, String> characters(JsonObject json, File file) {
        JsonObject charactersJson = json.getAsJsonObject("characters");
        if (charactersJson == null) {
            return Map.of();
        }
        Map<Character, String> characters = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : charactersJson.entrySet()) {
            characters.put(requireSingleCharacter(entry.getKey(), file), entry.getValue().getAsString());
        }
        return characters;
    }

    private static char requireSingleCharacter(String key, File file) {
        if (key.length() != 1) {
            throw new IllegalStateException("Font file '" + file.getName() + "' has an invalid character key '" + key + "'");
        }
        return key.charAt(0);
    }

    private static String requiredString(JsonObject json, String field, File file) {
        JsonElement element = json.get(field);
        if (element == null || !element.isJsonPrimitive() || element.getAsString().isBlank()) {
            throw new IllegalStateException("Font file '" + file.getName() + "' is missing required field '" + field + "'");
        }
        return element.getAsString();
    }
}
