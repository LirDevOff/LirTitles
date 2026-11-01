package org.lirdev.lirtitles.menu.menus.customtitles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.CustomTitlesMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.data.model.CustomTitle;
import org.lirdev.lirtitles.data.model.CustomTitleStatus;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.MenuClickKind;
import org.lirdev.lirtitles.menu.menus.fonts.FontSelectionCache;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.menu.menus.shared.StaticMenuItems;

public final class CustomTitlesMenu {
    private final LirTitles plugin;
    private final MenuItemFactory itemFactory;
    private final StaticMenuItems staticItems;
    private final FontSelectionCache fontSelection = new FontSelectionCache();
    private CustomTitlesMenuConfiguration cachedAppearance;
    private List<Integer> cachedAvailableSlots;

    public CustomTitlesMenu(LirTitles plugin) {
        this.plugin = plugin;
        itemFactory = new MenuItemFactory();
        staticItems = new StaticMenuItems(itemFactory);
    }

    public synchronized void clearCaches() {
        staticItems.clearCaches();
        fontSelection.clearCaches();
        cachedAppearance = null;
        cachedAvailableSlots = null;
    }

    public ChestMenu create(Player player, List<CustomTitle> titles, String selectedCustomTitleKey, int page, String fontId, int tokens,
                            Consumer<Player> onCreate, BiConsumer<Player, CustomTitle> onTitleActivated,
                            BiConsumer<Player, CustomTitle> onTitleDeleted, Consumer<Player> onBack, Consumer<Player> onNext,
                            Consumer<Player> onPrevious, BiConsumer<Player, String> onFontChanged) {
        CustomTitlesMenuConfiguration appearance = plugin.configuration().customTitlesMenu();
        ChestMenu.Builder menu = ChestMenu.builder(appearance.title(), appearance.rows());
        staticItems.addTo(menu, player, appearance.staticItems());
        add(menu, appearance.backItem().slot(), ConfiguredMenuButtons.create(player, appearance.backItem(),
                itemFactory.create(appearance.backItem(), MenuItemTokens.NONE), context -> onBack.accept(context.player())));
        MenuButton font = fontSelection.button(plugin, appearance.fontItem(), player, itemFactory, fontId, onFontChanged);
        if (font != null) {
            add(menu, appearance.fontItem().slot(), font);
        }

        List<Integer> availableTitleSlots = availableSlots(appearance);
        int capacity = appearance.createLayout() == CustomTitlesMenuConfiguration.Layout.DYNAMIC
                ? Math.max(0, availableTitleSlots.size() - 1)
                : availableTitleSlots.size();
        int start = Math.max(page, 0) * capacity;
        List<CustomTitle> visible = titles.subList(Math.min(start, titles.size()), Math.min(start + capacity, titles.size()));
        int createSlot = createSlot(appearance, visible.size());
        List<Integer> titleSlots = titleSlots(availableTitleSlots, appearance.createLayout(), createSlot);
        for (int index = 0; index < visible.size(); index++) {
            CustomTitle title = visible.get(index);
            int slot = appearance.layout() == CustomTitlesMenuConfiguration.Layout.STATIC ? title.slot() : titleSlots.get(index);
            if (slot < 0 || slot >= appearance.rows() * 9) {
                continue;
            }
            add(menu, slot, titleButton(player, appearance, title, selectedCustomTitleKey, fontId, onTitleActivated, onTitleDeleted));
        }
        add(menu, createSlot, ConfiguredMenuButtons.create(player, appearance.createItem(),
                itemFactory.create(appearance.createItem(), MenuItemTokens.of(Map.of("tokens", Component.text(tokens)))), context -> onCreate.accept(context.player())));
        if (start + capacity < titles.size()) {
            add(menu, appearance.nextPageItem().slot(), ConfiguredMenuButtons.create(player, appearance.nextPageItem(),
                    itemFactory.create(appearance.nextPageItem(), MenuItemTokens.NONE), context -> onNext.accept(context.player())));
        }
        if (page > 0) {
            add(menu, appearance.previousPageItem().slot(), ConfiguredMenuButtons.create(player, appearance.previousPageItem(),
                    itemFactory.create(appearance.previousPageItem(), MenuItemTokens.NONE), context -> onPrevious.accept(context.player())));
        }
        return menu.build();
    }

    private MenuButton titleButton(Player player, CustomTitlesMenuConfiguration appearance, CustomTitle title, String selectedCustomTitleKey,
                                   String fontId, BiConsumer<Player, CustomTitle> onTitleActivated, BiConsumer<Player, CustomTitle> onTitleDeleted) {
        TitleDisplayState state = displayState(title, selectedCustomTitleKey);
        MenuItem template = appearance.titleItems().forState(state);
        Component rendered = plugin.configuration().fonts().render(fontId, plugin.colorizer().colorize(title.text()));
        Component reason = state == TitleDisplayState.REJECTED
                ? Component.text(title.rejectionReason())
                : Component.empty();
        MenuItemTokens tokens = MenuItemTokens.of(Map.of(
                "title", rendered,
                "preview", rendered,
                "reason", reason));
        return ConfiguredMenuButtons.create(player, template, itemFactory.create(template, tokens), Map.of(
                MenuClickKind.LEFT, context -> onTitleActivated.accept(context.player(), title),
                MenuClickKind.DROP, context -> onTitleDeleted.accept(context.player(), title)
        ));
    }

    private static TitleDisplayState displayState(CustomTitle title, String selectedCustomTitleKey) {
        if (title.status() == CustomTitleStatus.PENDING) {
            return TitleDisplayState.PENDING;
        }
        if (title.status() == CustomTitleStatus.REJECTED) {
            return TitleDisplayState.REJECTED;
        }
        return title.key().equals(selectedCustomTitleKey) ? TitleDisplayState.ACTIVE : TitleDisplayState.NORMAL;
    }

    private synchronized List<Integer> availableSlots(CustomTitlesMenuConfiguration appearance) {
        if (cachedAppearance == appearance && cachedAvailableSlots != null) {
            return cachedAvailableSlots;
        }
        Set<Integer> occupied = new HashSet<>();
        appearance.staticItems().forEach(item -> occupied.addAll(item.slots()));
        java.util.stream.Stream.of(appearance.backItem(), appearance.nextPageItem(), appearance.previousPageItem(), appearance.fontItem())
                .filter(java.util.Objects::nonNull).filter(item -> item.slot() >= 0).forEach(item -> occupied.add(item.slot()));
        if (appearance.createLayout() == CustomTitlesMenuConfiguration.Layout.STATIC && appearance.createItem().slot() >= 0) {
            occupied.add(appearance.createItem().slot());
        }
        List<Integer> slots = new ArrayList<>();
        for (int slot = Math.max(0, appearance.firstSlot()); slot < appearance.rows() * 9; slot++) {
            if (!occupied.contains(slot)) {
                slots.add(slot);
            }
        }
        cachedAppearance = appearance;
        cachedAvailableSlots = List.copyOf(slots);
        return cachedAvailableSlots;
    }

    private static int createSlot(CustomTitlesMenuConfiguration appearance, int visibleTitleCount) {
        if (appearance.createLayout() == CustomTitlesMenuConfiguration.Layout.STATIC) {
            return appearance.createItem().slot();
        }
        return visibleTitleCount < appearance.rows() * 9 ? visibleTitleCount : -1;
    }

    private static List<Integer> titleSlots(List<Integer> availableSlots, CustomTitlesMenuConfiguration.Layout createLayout, int createSlot) {
        if (createLayout == CustomTitlesMenuConfiguration.Layout.STATIC) {
            return availableSlots;
        }
        return availableSlots.stream().filter(slot -> slot != createSlot).toList();
    }

    private static void add(ChestMenu.Builder menu, int slot, MenuButton button) {
        if (button != null && slot >= 0) {
            menu.button(slot, button);
        }
    }
}
