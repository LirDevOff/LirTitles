package org.lirdev.lirtitles.configuration.titles;

import org.bukkit.configuration.file.FileConfiguration;

public record CustomTitleSettings(boolean enabled, int minLength, int maxLength, boolean moderationEnabled) {

    public CustomTitleSettings {
        if (minLength < 1 || maxLength < minLength) {
            throw new IllegalStateException("Custom title length must satisfy 1 <= min <= max, but was min=" + minLength + ", max=" + maxLength);
        }
    }

    public static CustomTitleSettings load(FileConfiguration configuration) {
        return new CustomTitleSettings(
                configuration.getBoolean("settings.custom-titles.enabled", true),
                configuration.getInt("settings.custom-titles.length.min", 5),
                configuration.getInt("settings.custom-titles.length.max", 20),
                configuration.getBoolean("settings.custom-titles.moderation.enabled", true));
    }

    public boolean acceptsLength(int length) {
        return length >= minLength && length <= maxLength;
    }
}
