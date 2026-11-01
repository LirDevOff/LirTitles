package org.lirdev.lirtitles.menu.menus.fonts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.fonts.TitleFont;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;

public final class FontSelectionCache {
    private static final int MAX_CACHED_ITEMS = 128;

    private final LinkedHashMap<FontSelectionKey, ItemStack> displayItems = new LinkedHashMap<>(16, 0.75F, true);

    public MenuButton button(LirTitles plugin, MenuItem appearance, Player viewer, MenuItemFactory itemFactory,
                             String selectedFontId, BiConsumer<Player, String> onFontChanged) {
        if (appearance == null) {
            return null;
        }
        List<TitleFont> fonts = plugin.configuration().fonts().fontList();
        TitleFont selectedFont = fonts.isEmpty() ? null : plugin.configuration().fonts().findOrDefault(selectedFontId).orElseThrow();
        String resolvedFontId = selectedFont == null ? "" : selectedFont.id();
        FontSelectionKey key = new FontSelectionKey(appearance, resolvedFontId);
        ItemStack displayItem = displayItem(key, plugin, appearance, itemFactory, fonts, selectedFont);
        return FontSelectionMenuItem.create(appearance, viewer, fonts, selectedFont, displayItem, onFontChanged);
    }

    private synchronized ItemStack displayItem(FontSelectionKey key, LirTitles plugin, MenuItem appearance,
                                               MenuItemFactory itemFactory, List<TitleFont> fonts, TitleFont selectedFont) {
        ItemStack displayItem = displayItems.get(key);
        if (displayItem == null) {
            displayItem = FontSelectionMenuItem.displayItem(plugin, appearance, itemFactory, fonts, selectedFont);
            displayItems.put(key, displayItem);
            if (displayItems.size() > MAX_CACHED_ITEMS) {
                displayItems.remove(displayItems.keySet().iterator().next());
            }
        }
        return displayItem;
    }

    public synchronized void clearCaches() {
        displayItems.clear();
    }

    private static final class FontSelectionKey {
        private final MenuItem appearance;
        private final String fontId;

        private FontSelectionKey(MenuItem appearance, String fontId) {
            this.appearance = appearance;
            this.fontId = fontId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof FontSelectionKey key && appearance == key.appearance && fontId.equals(key.fontId);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(appearance) + fontId.hashCode();
        }
    }
}
