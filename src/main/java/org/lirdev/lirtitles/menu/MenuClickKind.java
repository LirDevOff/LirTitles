package org.lirdev.lirtitles.menu;

import org.bukkit.event.inventory.ClickType;

public enum MenuClickKind {
    LEFT,
    SHIFT_LEFT,
    RIGHT,
    SHIFT_RIGHT,
    MIDDLE,
    DOUBLE,
    DROP;

    public static MenuClickKind classify(ClickType clickType) {
        return switch (clickType) {
            case LEFT -> LEFT;
            case SHIFT_LEFT -> SHIFT_LEFT;
            case RIGHT -> RIGHT;
            case SHIFT_RIGHT -> SHIFT_RIGHT;
            case MIDDLE -> MIDDLE;
            case DOUBLE_CLICK -> DOUBLE;
            case DROP -> DROP;
            default -> null;
        };
    }
}
