package org.lirdev.lirtitles.menu.menus.titles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.TitleSelectionMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.titles.Titles;
import org.lirdev.lirtitles.data.PlayerTitleStorage;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.action.MenuClickAction;
import org.lirdev.lirtitles.menu.menus.fonts.FontSelectionCache;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.menu.menus.shared.StaticMenuItems;
import org.lirdev.lirtitles.permissions.Permissions;
import org.lirdev.lirtitles.utilities.PlaceholderReplacer;

public final class TitleSelectionMenu {
    private static final int MAX_CACHED_TITLE_ITEMS = 4_096;
    private final LirTitles plugin;
    private final PlayerTitleStorage storage;
    private final MenuItemFactory itemFactory;
    private final StaticMenuItems staticItems;
    private final FontSelectionCache fontSelection = new FontSelectionCache();
    private final LinkedHashMap<TitleDisplayKey, ItemStack> titleItems = new LinkedHashMap<>(64, 0.75F, true);
    private final LinkedHashMap<RenderedTitleKey, Component> renderedTitles = new LinkedHashMap<>(64, 0.75F, true);
    private TitleSelectionMenuConfiguration cachedAppearance;
    private Titles.SlotMode cachedSlotMode;
    private List<Integer> cachedAvailableSlots;

    public TitleSelectionMenu(LirTitles plugin, PlayerTitleStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        itemFactory = new MenuItemFactory();
        staticItems = new StaticMenuItems(itemFactory);
    }

    public synchronized void clearCaches() {
        staticItems.clearCaches();
        fontSelection.clearCaches();
        titleItems.clear();
        renderedTitles.clear();
        cachedAppearance = null;
        cachedSlotMode = null;
        cachedAvailableSlots = null;
    }

    public ChestMenu create(Player player, Titles.Category category, Consumer<Player> onBack, BiConsumer<Player, Titles.Title> onTitleChosen,
                            String selectedFontId, BiConsumer<Player, String> onFontChanged, int page, Consumer<Player> onNextPage, Consumer<Player> onPreviousPage) {
        TitleSelectionMenuConfiguration appearance = plugin.configuration().titleSelectionMenu();
        Titles titlesConfiguration = plugin.configuration().titles();
        List<Titles.Title> titles = category.orderedTitles();
        List<Integer> availableSlots = availableSlots(appearance, titlesConfiguration.slotMode());
        int capacity = availableSlots.size();
        if (capacity == 0) {
            throw new IllegalStateException("Title selection menu has no slots available for titles");
        }
        int maxPages = Math.max(1, (titles.size() - 1) / capacity + 1);
        int currentPage = Math.min(Math.max(page, 0), maxPages - 1) + 1;
        MenuItemTokens pageTokens = MenuItemTokens.of(Map.of(
                "category", category.name(),
                "current_page", Component.text(currentPage),
                "max_pages", Component.text(maxPages)));
        ChestMenu.Builder menu = ChestMenu.builder(PlaceholderReplacer.replace(appearance.title(), pageTokens.inline()), appearance.rows());
        staticItems.addTo(menu, player, appearance.staticItems());
        MenuButton fontButton = fontSelection.button(plugin, appearance.fontItem(), player, itemFactory, selectedFontId, onFontChanged);
        if (fontButton != null) {
            menu.button(appearance.fontItem().slot(), fontButton);
        }
        int start = Math.max(page, 0) * capacity;
        List<Titles.Title> visible = titles.subList(Math.min(start, titles.size()), Math.min(start + capacity, titles.size()));
        boolean unlockAll = player.hasPermission(Permissions.UNLOCK_ALL);
        Set<String> ownedTitles = unlockAll ? Set.of() : storage.owned(player.getUniqueId());
        String selectedTitle = storage.selected(player.getUniqueId());
        String appliedFont = storage.selectedFont(player.getUniqueId());
        for (int index = 0; index < visible.size(); index++) {
            Titles.Title title = visible.get(index);
            boolean useManualSlot = titlesConfiguration.slotMode() == Titles.SlotMode.MANUAL && titles.size() <= capacity && title.slot() >= 0;
            int slot = useManualSlot ? title.slot() : availableSlots.get(index);
            if (slot < 0 || slot >= appearance.rows() * 9) {
                continue;
            }
            boolean owned = unlockAll || ownedTitles.contains(title.key());
            boolean active = owned && selectedTitle.equals(title.key()) && selectedFontId.equals(appliedFont);
            MenuItem itemAppearance = active ? appearance.activeItem() : owned ? appearance.availableItem() : appearance.lockedItem();
            MenuClickAction select = owned ? context -> onTitleChosen.accept(context.player(), title) : null;
            MenuButton button = ConfiguredMenuButtons.create(player, itemAppearance,
                    titleDisplay(itemAppearance, title, selectedFontId, category.name()), select);
            if (button != null) {
                menu.button(slot, button);
            }
        }
        int backSlot = appearance.backItem().slot() >= 0 ? appearance.backItem().slot() : appearance.rows() * 9 - 1;
        MenuButton backButton = ConfiguredMenuButtons.create(player, appearance.backItem(),
                itemFactory.create(appearance.backItem(), pageTokens), context -> onBack.accept(context.player()));
        if (backButton != null) {
            menu.button(backSlot, backButton);
        }
        if (appearance.nextPageItem() != null && start + capacity < titles.size()) {
            MenuButton next = ConfiguredMenuButtons.create(player, appearance.nextPageItem(), itemFactory.create(appearance.nextPageItem(), pageTokens),
                    context -> onNextPage.accept(context.player()));
            if (next != null) {
                menu.button(appearance.nextPageItem().slot(), next);
            }
        }
        if (appearance.previousPageItem() != null && page > 0) {
            MenuButton previous = ConfiguredMenuButtons.create(player, appearance.previousPageItem(), itemFactory.create(appearance.previousPageItem(), pageTokens),
                    context -> onPreviousPage.accept(context.player()));
            if (previous != null) {
                menu.button(appearance.previousPageItem().slot(), previous);
            }
        }
        return menu.build();
    }

    private synchronized List<Integer> availableSlots(TitleSelectionMenuConfiguration appearance, Titles.SlotMode slotMode) {
        if (cachedAppearance == appearance && cachedSlotMode == slotMode && cachedAvailableSlots != null) {
            return cachedAvailableSlots;
        }
        if (slotMode == Titles.SlotMode.RANGE) {
            List<Integer> configuredSlots = appearance.titleSlots();
            if (configuredSlots.isEmpty()) {
                throw new IllegalStateException("Set 'titles-slots' in menus/titles/selection.yml when titles.yml uses slot-mode: range");
            }
            validateConfiguredSlots(appearance, configuredSlots);
            cachedAppearance = appearance;
            cachedSlotMode = slotMode;
            cachedAvailableSlots = configuredSlots;
            return configuredSlots;
        }
        Set<Integer> occupied = new HashSet<>();
        appearance.staticItems().forEach(item -> occupied.addAll(item.slots()));
        java.util.stream.Stream.of(appearance.backItem(), appearance.fontItem(), appearance.nextPageItem(), appearance.previousPageItem())
                .filter(java.util.Objects::nonNull).filter(item -> item.slot() >= 0).forEach(item -> occupied.add(item.slot()));
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < appearance.rows() * 9; slot++) {
            if (!occupied.contains(slot)) {
                slots.add(slot);
            }
        }
        cachedAppearance = appearance;
        cachedSlotMode = slotMode;
        cachedAvailableSlots = List.copyOf(slots);
        return cachedAvailableSlots;
    }

    private static void validateConfiguredSlots(TitleSelectionMenuConfiguration appearance, List<Integer> configuredSlots) {
        Set<Integer> reserved = new HashSet<>();
        appearance.staticItems().forEach(item -> reserved.addAll(item.slots()));
        java.util.stream.Stream.of(appearance.backItem(), appearance.fontItem(), appearance.nextPageItem(), appearance.previousPageItem())
                .filter(java.util.Objects::nonNull).filter(item -> item.slot() >= 0).forEach(item -> reserved.add(item.slot()));
        for (int slot : configuredSlots) {
            if (reserved.contains(slot)) {
                throw new IllegalStateException("Title slot range in menus/titles/selection.yml overlaps a menu control at slot " + slot);
            }
        }
    }

    private synchronized ItemStack titleDisplay(MenuItem template, Titles.Title title, String fontId, Component categoryName) {
        TitleDisplayKey key = new TitleDisplayKey(template, title.key(), fontId, categoryName);
        ItemStack item = titleItems.get(key);
        if (item != null) {
            return item;
        }
        item = itemFactory.create(template, MenuItemTokens.titled(rendered(title, fontId), categoryName));
        titleItems.put(key, item);
        if (titleItems.size() > MAX_CACHED_TITLE_ITEMS) {
            titleItems.remove(titleItems.keySet().iterator().next());
        }
        return item;
    }

    private Component rendered(Titles.Title title, String fontId) {
        RenderedTitleKey key = new RenderedTitleKey(title.key(), fontId);
        Component rendered = renderedTitles.get(key);
        if (rendered != null) {
            return rendered;
        }
        rendered = plugin.configuration().fonts().render(fontId, title.displayName());
        renderedTitles.put(key, rendered);
        if (renderedTitles.size() > MAX_CACHED_TITLE_ITEMS) {
            renderedTitles.remove(renderedTitles.keySet().iterator().next());
        }
        return rendered;
    }

    private record TitleDisplayKey(MenuItem template, String titleKey, String fontId, Component categoryName) {
    }

    private record RenderedTitleKey(String titleKey, String fontId) {
    }
}
