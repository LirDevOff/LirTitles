package org.lirdev.lirtitles.menu.menus.titles;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.TitleApplyMenuConfiguration;
import org.lirdev.lirtitles.configuration.titles.Titles;
import org.lirdev.lirtitles.fonts.FontTextRenderer;
import org.lirdev.lirtitles.fonts.TitleFont;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.MenuClickKind;
import org.lirdev.lirtitles.menu.action.MenuClickAction;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.menu.menus.shared.StaticMenuItems;
import org.lirdev.lirtitles.utilities.PlaceholderReplacer;

public final class TitleApplyMenu {
    private static final int DEFAULT_TITLE_SLOT = 13;
    private static final int DEFAULT_FONT_SLOT = 22;
    private static final int DEFAULT_APPLY_SLOT = 15;
    private static final int DEFAULT_DENY_SLOT = 11;

    private final LirTitles plugin;
    private final MenuItemFactory itemFactory;
    private final StaticMenuItems staticItems;

    public TitleApplyMenu(LirTitles plugin) {
        this.plugin = plugin;
        itemFactory = new MenuItemFactory();
        staticItems = new StaticMenuItems(itemFactory);
    }

    public void clearCaches() {
        staticItems.clearCaches();
    }

    public ChestMenu create(Player player, Titles.Title title, String previewFontId,
                            BiConsumer<Player, String> onFontChange, BiConsumer<Player, String> onApply, Consumer<Player> onDeny) {
        TitleApplyMenuConfiguration appearance = plugin.configuration().titleApplyMenu();
        List<TitleFont> fonts = plugin.configuration().fonts().fontList();
        TitleFont previewFont = plugin.configuration().fonts().findOrDefault(previewFontId).orElse(null);

        ChestMenu.Builder menu = ChestMenu.builder(PlaceholderReplacer.replace(appearance.title(), Map.of("title", title.displayName())), appearance.rows());
        staticItems.addTo(menu, player, appearance.staticItems());

        String appliedFontId = previewFont == null ? "" : previewFont.id();
        List<Component> fontOptions = fontOptions(fonts, previewFont, appearance);
        MenuItemTokens titleTokens = MenuItemTokens.of(Map.of(
                "title", title.displayName(),
                "category", Component.text(title.categoryId()),
                "preview", previewFont == null ? title.displayName() : FontTextRenderer.render(previewFont, title.displayName())
        )).withBlock("font_options", fontOptions);
        ItemStack titleDisplayItem = itemFactory.create(appearance.titleItem(), titleTokens);
        placeAt(menu, appearance.titleItem().slot(), DEFAULT_TITLE_SLOT,
                ConfiguredMenuButtons.create(player, appearance.titleItem(), titleDisplayItem, (MenuClickAction) null));

        Map<MenuClickKind, MenuClickAction> fontActions = previewFont == null ? Map.of() : Map.of(
                MenuClickKind.LEFT, context -> onFontChange.accept(context.player(), cycle(fonts, previewFont, 1)),
                MenuClickKind.SHIFT_LEFT, context -> onFontChange.accept(context.player(), cycle(fonts, previewFont, 1)),
                MenuClickKind.RIGHT, context -> onFontChange.accept(context.player(), cycle(fonts, previewFont, -1)),
                MenuClickKind.SHIFT_RIGHT, context -> onFontChange.accept(context.player(), cycle(fonts, previewFont, -1))
        );
        ItemStack fontDisplayItem = itemFactory.create(appearance.fontItem(), MenuItemTokens.NONE.withBlock("font_options", fontOptions));
        placeAt(menu, appearance.fontItem().slot(), DEFAULT_FONT_SLOT,
                ConfiguredMenuButtons.create(player, appearance.fontItem(), fontDisplayItem, fontActions));

        ItemStack applyDisplayItem = itemFactory.create(appearance.applyItem(), titleTokens);
        placeAt(menu, appearance.applyItem().slot(), DEFAULT_APPLY_SLOT,
                ConfiguredMenuButtons.create(player, appearance.applyItem(), applyDisplayItem, context -> onApply.accept(context.player(), appliedFontId)));

        ItemStack denyDisplayItem = itemFactory.create(appearance.denyItem(), titleTokens);
        placeAt(menu, appearance.denyItem().slot(), DEFAULT_DENY_SLOT,
                ConfiguredMenuButtons.create(player, appearance.denyItem(), denyDisplayItem, context -> onDeny.accept(context.player())));

        return menu.build();
    }

    private List<Component> fontOptions(List<TitleFont> fonts, TitleFont selectedFont, TitleApplyMenuConfiguration appearance) {
        return fonts.stream().map(font -> formatFont(font, selectedFont, appearance)).toList();
    }

    private Component formatFont(TitleFont font, TitleFont selectedFont, TitleApplyMenuConfiguration appearance) {
        Component format = font.equals(selectedFont) ? appearance.selectedFontFormat() : appearance.availableFontFormat();
        return PlaceholderReplacer.replace(format, Map.of("font", plugin.colorizer().colorize(font.displayName())));
    }

    private static String cycle(List<TitleFont> fonts, TitleFont current, int direction) {
        if (fonts.isEmpty()) {
            return "";
        }
        int index = Math.max(fonts.indexOf(current), 0);
        int size = fonts.size();
        int nextIndex = ((index + direction) % size + size) % size;
        return fonts.get(nextIndex).id();
    }

    private static void placeAt(ChestMenu.Builder menu, int configuredSlot, int defaultSlot, MenuButton button) {
        if (button == null) {
            return;
        }
        menu.button(configuredSlot >= 0 ? configuredSlot : defaultSlot, button);
    }
}
