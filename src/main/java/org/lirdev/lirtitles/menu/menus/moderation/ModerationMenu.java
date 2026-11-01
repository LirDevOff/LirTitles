package org.lirdev.lirtitles.menu.menus.moderation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.ModerationMenuConfiguration;
import org.lirdev.lirtitles.data.model.CustomTitle;
import org.lirdev.lirtitles.data.model.PendingModerationEntry;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.MenuClickKind;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.menu.menus.shared.StaticMenuItems;

public final class ModerationMenu {
    private final LirTitles plugin;
    private final MenuItemFactory itemFactory;
    private final StaticMenuItems staticItems;
    private ModerationMenuConfiguration cachedAppearance;
    private List<Integer> cachedAvailableSlots;

    public ModerationMenu(LirTitles plugin) {
        this.plugin = plugin;
        itemFactory = new MenuItemFactory();
        staticItems = new StaticMenuItems(itemFactory);
    }

    public synchronized void clearCaches() {
        staticItems.clearCaches();
        cachedAppearance = null;
        cachedAvailableSlots = null;
    }

    public ChestMenu create(Player player, List<PendingModerationEntry> entries, int page,
                            BiConsumer<Player, PendingModerationEntry> onApprove,
                            BiConsumer<Player, PendingModerationEntry> onReject,
                            BiConsumer<Player, PendingModerationEntry> onRejectWithReason,
                            Consumer<Player> onBack, Consumer<Player> onNext, Consumer<Player> onPrevious) {
        ModerationMenuConfiguration appearance = plugin.configuration().moderationMenu();
        ChestMenu.Builder menu = ChestMenu.builder(appearance.title(), appearance.rows());
        staticItems.addTo(menu, player, appearance.staticItems());
        add(menu, appearance.backItem().slot(), ConfiguredMenuButtons.create(player, appearance.backItem(),
                itemFactory.create(appearance.backItem(), MenuItemTokens.NONE), context -> onBack.accept(context.player())));

        List<Integer> availableSlots = availableSlots(appearance);
        int capacity = availableSlots.size();
        int start = Math.max(page, 0) * capacity;
        List<PendingModerationEntry> visible = entries.subList(Math.min(start, entries.size()), Math.min(start + capacity, entries.size()));
        for (int index = 0; index < visible.size(); index++) {
            add(menu, availableSlots.get(index), entryButton(player, appearance, visible.get(index), onApprove, onReject, onRejectWithReason));
        }
        if (start + capacity < entries.size()) {
            add(menu, appearance.nextPageItem().slot(), ConfiguredMenuButtons.create(player, appearance.nextPageItem(),
                    itemFactory.create(appearance.nextPageItem(), MenuItemTokens.NONE), context -> onNext.accept(context.player())));
        }
        if (page > 0) {
            add(menu, appearance.previousPageItem().slot(), ConfiguredMenuButtons.create(player, appearance.previousPageItem(),
                    itemFactory.create(appearance.previousPageItem(), MenuItemTokens.NONE), context -> onPrevious.accept(context.player())));
        }
        return menu.build();
    }

    private MenuButton entryButton(Player player, ModerationMenuConfiguration appearance, PendingModerationEntry entry,
                                   BiConsumer<Player, PendingModerationEntry> onApprove, BiConsumer<Player, PendingModerationEntry> onReject,
                                   BiConsumer<Player, PendingModerationEntry> onRejectWithReason) {
        CustomTitle title = entry.title();
        Component rendered = plugin.colorizer().colorize(title.text());
        MenuItemTokens tokens = MenuItemTokens.of(Map.of(
                "title", rendered,
                "preview", rendered,
                "id", Component.text(String.valueOf(title.numericId())),
                "author", Component.text(entry.playerName())));
        return ConfiguredMenuButtons.create(player, appearance.entryItem(), itemFactory.create(appearance.entryItem(), tokens), Map.of(
                MenuClickKind.LEFT, context -> onApprove.accept(context.player(), entry),
                MenuClickKind.RIGHT, context -> onReject.accept(context.player(), entry),
                MenuClickKind.DROP, context -> onRejectWithReason.accept(context.player(), entry)
        ));
    }

    private synchronized List<Integer> availableSlots(ModerationMenuConfiguration appearance) {
        if (cachedAppearance == appearance && cachedAvailableSlots != null) {
            return cachedAvailableSlots;
        }
        Set<Integer> occupied = new HashSet<>();
        appearance.staticItems().forEach(item -> occupied.addAll(item.slots()));
        Stream.of(appearance.backItem(), appearance.nextPageItem(), appearance.previousPageItem())
                .filter(Objects::nonNull).filter(item -> item.slot() >= 0).forEach(item -> occupied.add(item.slot()));
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

    private static void add(ChestMenu.Builder menu, int slot, MenuButton button) {
        if (button != null && slot >= 0) {
            menu.button(slot, button);
        }
    }
}
