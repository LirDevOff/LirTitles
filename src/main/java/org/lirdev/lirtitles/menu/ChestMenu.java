package org.lirdev.lirtitles.menu;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import net.kyori.adventure.text.Component;

public final class ChestMenu {
    public static final int ROW_WIDTH = 9;
    public static final int MAX_ROWS = 6;
    private final Component title;
    private final int size;
    private final Map<Integer, MenuButton> buttons;

    private ChestMenu(Component title, int size, Map<Integer, MenuButton> buttons) {
        this.title = title;
        this.size = size;
        this.buttons = Map.copyOf(buttons);
    }

    public static Builder builder(Component title, int rows) {
        return new Builder(title, rows);
    }

    public Component title() {
        return title;
    }

    public int size() {
        return size;
    }

    public MenuButton buttonAt(int slot) {
        return buttons.get(slot);
    }

    public Map<Integer, MenuButton> buttons() {
        return buttons;
    }

    public static final class Builder {
        private final Component title;
        private final int size;
        private final Map<Integer, MenuButton> buttons = new LinkedHashMap<>();

        private Builder(Component title, int rows) {
            this.title = Objects.requireNonNull(title, "title");
            if (rows < 1 || rows > MAX_ROWS) {
                throw new IllegalArgumentException("Menu rows must be between 1 and 6");
            }
            size = rows * ROW_WIDTH;
        }

        public Builder button(int slot, MenuButton button) {
            if (slot < 0 || slot >= size) {
                throw new IllegalArgumentException("Slot must be inside the menu");
            }
            buttons.put(slot, Objects.requireNonNull(button, "button"));
            return this;
        }

        public ChestMenu build() {
            return new ChestMenu(title, size, buttons);
        }
    }
}
