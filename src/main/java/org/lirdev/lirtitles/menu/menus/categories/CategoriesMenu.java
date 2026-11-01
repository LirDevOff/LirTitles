package org.lirdev.lirtitles.menu.menus.categories;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.CategoriesMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.configuration.titles.Titles;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.action.MenuClickAction;
import org.lirdev.lirtitles.menu.menus.fonts.FontSelectionCache;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.menu.menus.shared.StaticMenuItems;

public final class CategoriesMenu {
    private final LirTitles plugin;
    private final MenuItemFactory itemFactory;
    private final StaticMenuItems staticItems;
    private final FontSelectionCache fontSelection = new FontSelectionCache();
    private final Map<Titles.Category, MenuButton> categoryButtons = new IdentityHashMap<>();
    private MenuItem cachedCustomTitlesMenuItem;
    private MenuButton cachedCustomTitlesButton;

    public CategoriesMenu(LirTitles plugin) {
        this.plugin = plugin;
        itemFactory = new MenuItemFactory();
        staticItems = new StaticMenuItems(itemFactory);
    }

    public synchronized void clearCaches() {
        staticItems.clearCaches();
        fontSelection.clearCaches();
        categoryButtons.clear();
        cachedCustomTitlesMenuItem = null;
        cachedCustomTitlesButton = null;
    }

    public ChestMenu create(Player viewer, BiConsumer<Player, Titles.Category> onCategorySelected,
                            String selectedFontId,
                            BiConsumer<Player, String> onFontChanged, Consumer<Player> onCustomTitles) {
        Titles titles = plugin.configuration().titles();
        CategoriesMenuConfiguration appearance = plugin.configuration().categoriesMenu();
        ChestMenu.Builder menu = ChestMenu.builder(appearance.title(), appearance.rows());
        staticItems.addTo(menu, viewer, appearance.staticItems());
        MenuButton fontButton = fontSelection.button(plugin, appearance.fontItem(), viewer, itemFactory, selectedFontId, onFontChanged);
        if (fontButton != null) {
            menu.button(appearance.fontItem().slot(), fontButton);
        }
        if (plugin.configuration().customTitles().enabled() && appearance.customTitlesItem() != null) {
            MenuButton customTitles = customTitlesButton(viewer, appearance.customTitlesItem(), onCustomTitles);
            if (customTitles != null) {
                menu.button(appearance.customTitlesItem().slot(), customTitles);
            }
        }
        int nextSlot = 0;
        for (Titles.Category category : titles.categories().values()) {
            int slot = category.slot() >= 0 ? category.slot() : nextSlot;
            nextSlot = Math.max(nextSlot + 1, slot + 1);
            if (slot >= appearance.rows() * 9) {
                continue;
            }
            MenuButton button = categoryButton(viewer, appearance.categoryItem(), category, onCategorySelected);
            if (button != null) {
                menu.button(slot, button);
            }
        }
        return menu.build();
    }

    private synchronized MenuButton customTitlesButton(Player viewer, MenuItem template, Consumer<Player> onCustomTitles) {
        if (!template.viewRequirements().isEmpty() || !template.clickRequirements().isEmpty()) {
            return buildCustomTitlesButton(viewer, template, onCustomTitles);
        }
        if (template == cachedCustomTitlesMenuItem) {
            return cachedCustomTitlesButton;
        }
        cachedCustomTitlesButton = buildCustomTitlesButton(null, template, onCustomTitles);
        cachedCustomTitlesMenuItem = template;
        return cachedCustomTitlesButton;
    }

    private MenuButton buildCustomTitlesButton(Player viewer, MenuItem template, Consumer<Player> onCustomTitles) {
        return ConfiguredMenuButtons.create(viewer, template, itemFactory.create(template, MenuItemTokens.BLANK),
                context -> onCustomTitles.accept(context.player()));
    }

    private synchronized MenuButton categoryButton(Player viewer, MenuItem template, Titles.Category category,
                                                   BiConsumer<Player, Titles.Category> onCategorySelected) {
        if (!template.viewRequirements().isEmpty() || !template.clickRequirements().isEmpty()) {
            return buildCategoryButton(viewer, template, category, onCategorySelected);
        }
        return categoryButtons.computeIfAbsent(category,
                key -> buildCategoryButton(null, template, key, onCategorySelected));
    }

    private MenuButton buildCategoryButton(Player viewer, MenuItem template, Titles.Category category,
                                           BiConsumer<Player, Titles.Category> onCategorySelected) {
        MenuClickAction select = context -> onCategorySelected.accept(context.player(), category);
        return ConfiguredMenuButtons.create(viewer, template,
                itemFactory.create(template, MenuItemTokens.titled(category.name(), category.name()), category.material(), ""), select);
    }
}
