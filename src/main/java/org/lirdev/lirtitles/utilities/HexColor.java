package org.lirdev.lirtitles.utilities;

import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Color;

public record HexColor(Color color) {
    private static final String HEX_PREFIX = "#";
    private static final int HEX_DIGIT_COUNT = 6;
    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
    private static final int CHANNEL_MAX = 255;
    private static final float DARKEST_LIGHTNESS = 0.10f;
    private static final float LIGHTEST_LIGHTNESS = 0.90f;

    public HexColor {
        Objects.requireNonNull(color, "color");
    }

    public static Optional<HexColor> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String digits = withoutPrefix(value.trim());
        if (digits.length() != HEX_DIGIT_COUNT || !digits.chars().allMatch(HexFormat::isHexDigit)) {
            return Optional.empty();
        }
        return Optional.of(new HexColor(Color.fromRGB(HexFormat.fromHexDigits(digits))));
    }

    public String hex() {
        return HEX_PREFIX + HEX_FORMAT.toHexDigits(color.asRGB(), HEX_DIGIT_COUNT);
    }

    public Color asBukkitColor() {
        return color;
    }

    public List<HexColor> shades(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Shade count must be at least 1");
        }
        ShadeBase base = ShadeBase.of(color);
        HexColor[] shades = new HexColor[count];
        for (int index = 0; index < count; index++) {
            float ratio = count == 1 ? 0.5f : (float) index / (count - 1);
            shades[index] = base.withLightness(DARKEST_LIGHTNESS + (LIGHTEST_LIGHTNESS - DARKEST_LIGHTNESS) * ratio);
        }
        return List.of(shades);
    }

    private static String withoutPrefix(String value) {
        return value.startsWith(HEX_PREFIX) ? value.substring(HEX_PREFIX.length()) : value;
    }

    private record ShadeBase(float redHue, float greenHue, float blueHue, float saturation) {
        static ShadeBase of(Color color) {
            float red = (float) color.getRed() / CHANNEL_MAX;
            float green = (float) color.getGreen() / CHANNEL_MAX;
            float blue = (float) color.getBlue() / CHANNEL_MAX;
            float max = Math.max(red, Math.max(green, blue));
            float min = Math.min(red, Math.min(green, blue));
            if (max == min) {
                return new ShadeBase(0f, 0f, 0f, 0f);
            }
            float lightness = (max + min) / 2f;
            float delta = max - min;
            float saturation = lightness > 0.5f ? delta / (2f - max - min) : delta / (max + min);
            float hue = hue(red, green, blue, max, delta);
            return new ShadeBase(wrap(hue + 1f / 3f), wrap(hue), wrap(hue - 1f / 3f), saturation);
        }

        HexColor withLightness(float lightness) {
            if (saturation == 0f) {
                int gray = channel(lightness);
                return new HexColor(Color.fromRGB(gray, gray, gray));
            }
            float upper = lightness < 0.5f ? lightness * (1f + saturation) : lightness + saturation - lightness * saturation;
            float lower = 2f * lightness - upper;
            return new HexColor(Color.fromRGB(
                    channel(sector(lower, upper, redHue)),
                    channel(sector(lower, upper, greenHue)),
                    channel(sector(lower, upper, blueHue))));
        }

        private static float hue(float red, float green, float blue, float max, float delta) {
            if (max == red) {
                return ((green - blue) / delta + (green < blue ? 6f : 0f)) / 6f;
            }
            if (max == green) {
                return ((blue - red) / delta + 2f) / 6f;
            }
            return ((red - green) / delta + 4f) / 6f;
        }

        private static float wrap(float hue) {
            if (hue < 0f) {
                return hue + 1f;
            }
            return hue > 1f ? hue - 1f : hue;
        }

        private static float sector(float lower, float upper, float hue) {
            if (hue < 1f / 6f) {
                return lower + (upper - lower) * 6f * hue;
            }
            if (hue < 1f / 2f) {
                return upper;
            }
            if (hue < 2f / 3f) {
                return lower + (upper - lower) * (2f / 3f - hue) * 6f;
            }
            return lower;
        }

        private static int channel(float value) {
            return Math.clamp(Math.round(value * CHANNEL_MAX), 0, CHANNEL_MAX);
        }
    }
}