package org.lirdev.lirtitles.utilities;

import java.util.concurrent.TimeUnit;

public final class DurationFormatter {
    private DurationFormatter() {
    }

    public static String elapsedSince(long timestampMillis) {
        long elapsed = Math.max(0, System.currentTimeMillis() - timestampMillis);
        long days = TimeUnit.MILLISECONDS.toDays(elapsed);
        if (days >= 1) {
            return days + (days == 1 ? " day" : " days");
        }
        long hours = TimeUnit.MILLISECONDS.toHours(elapsed);
        if (hours >= 1) {
            return hours + (hours == 1 ? " hour" : " hours");
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed);
        if (minutes >= 1) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return "less than a minute";
    }
}
