package org.lirdev.lirtitles.menu.menus.colors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.lirdev.lirtitles.configuration.menus.ColorShadeMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.utilities.HexColor;

final class GeneratedShadeLayout {
    private static final int SHADE_CACHE_LIMIT = 64;

    private final ColorShadeMenuConfiguration appearance;
    private final List<Integer> slots;
    private final int shadeCount;
    private final Map<HexColor, List<HexColor>> shadesByBase = new LinkedHashMap<>(16, 0.75f, true);

    GeneratedShadeLayout(ColorShadeMenuConfiguration appearance) {
        this.appearance = Objects.requireNonNull(appearance, "appearance");
        slots = freeSlots(appearance);
        shadeCount = Math.min(appearance.generatedShadeCount(), slots.size());
    }

    boolean describes(ColorShadeMenuConfiguration candidate) {
        return appearance == candidate;
    }

    int slotAt(int index) {
        return slots.get(index);
    }

    synchronized List<HexColor> shadesOf(HexColor base) {
        if (shadeCount == 0) {
            return List.of();
        }
        List<HexColor> shades = shadesByBase.get(base);
        if (shades != null) {
            return shades;
        }
        shades = base.shades(shadeCount);
        if (shadesByBase.size() == SHADE_CACHE_LIMIT) {
            shadesByBase.remove(shadesByBase.keySet().iterator().next());
        }
        shadesByBase.put(base, shades);
        return shades;
    }

    private static List<Integer> freeSlots(ColorShadeMenuConfiguration appearance) {
        Set<Integer> occupied = new HashSet<>();
        appearance.staticItems().forEach(item -> occupied.addAll(item.slots()));
        Stream.of(appearance.backItem(), appearance.customItem())
                .filter(Objects::nonNull)
                .map(MenuItem::slot)
                .filter(slot -> slot >= 0)
                .forEach(occupied::add);
        List<Integer> free = new ArrayList<>();
        for (int slot = 0; slot < appearance.rows() * ChestMenu.ROW_WIDTH; slot++) {
            if (!occupied.contains(slot)) {
                free.add(slot);
            }
        }
        return List.copyOf(free);
    }
}
