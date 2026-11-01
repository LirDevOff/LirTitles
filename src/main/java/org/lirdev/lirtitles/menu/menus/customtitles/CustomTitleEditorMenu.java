package org.lirdev.lirtitles.menu.menus.customtitles;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.CustomTitleEditorMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.menu.menus.shared.StaticMenuItems;
import org.lirdev.lirtitles.utilities.HexColor;

public final class CustomTitleEditorMenu {
    private static final HexColor UNSET_SLOT_COLOR = HexColor.parse("#FFFFFF").orElseThrow();

    private final LirTitles plugin;
    private final MenuItemFactory itemFactory;
    private final StaticMenuItems staticItems;

    public CustomTitleEditorMenu(LirTitles plugin) {
        this.plugin = plugin;
        itemFactory = new MenuItemFactory();
        staticItems = new StaticMenuItems(itemFactory);
    }

    public void clearCaches() {
        staticItems.clearCaches();
    }

    public ChestMenu create(Player player, String text, Set<String> activeFormats, List<String> colors, Consumer<Player> onInput,
                            Consumer<Player> onConfirm, Consumer<Player> onCancel, Consumer<String> onFormat,
                            BiConsumer<Player, Integer> onColorSlot) {
        CustomTitleEditorMenuConfiguration appearance = plugin.configuration().customTitleEditorMenu();
        ChestMenu.Builder menu = ChestMenu.builder(appearance.title(), appearance.rows());
        staticItems.addTo(menu, player, appearance.staticItems());
        Component preview = text.isBlank() ? Component.text("Enter title text") : plugin.colorizer().colorize(text);
        MenuItemTokens tokens = MenuItemTokens.of(Map.of("title", preview, "preview", preview));
        add(menu, appearance.previewItem().slot(), ConfiguredMenuButtons.create(player, appearance.previewItem(),
                itemFactory.create(appearance.previewItem(), tokens), context -> onInput.accept(context.player())));
        add(menu, appearance.confirmItem().slot(), ConfiguredMenuButtons.create(player, appearance.confirmItem(),
                itemFactory.create(appearance.confirmItem(), tokens), context -> onConfirm.accept(context.player())));
        add(menu, appearance.cancelItem().slot(), ConfiguredMenuButtons.create(player, appearance.cancelItem(),
                itemFactory.create(appearance.cancelItem(), tokens), context -> onCancel.accept(context.player())));
        for (CustomTitleEditorMenuConfiguration.FormatButton format : appearance.formats()) {
            MenuItem formatItem = format.item(activeFormats.contains(format.tag()));
            add(menu, formatItem.slot(), ConfiguredMenuButtons.create(player, formatItem, itemFactory.create(formatItem, tokens),
                    context -> onFormat.accept(format.tag())));
        }
        addColorSlots(menu, player, appearance.colorSlots(), colors, onColorSlot);
        return menu.build();
    }

    private void addColorSlots(ChestMenu.Builder menu, Player player, List<MenuItem> colorSlots, List<String> colors,
                               BiConsumer<Player, Integer> onColorSlot) {
        for (int index = 0; index < colorSlots.size(); index++) {
            MenuItem slotItem = colorSlots.get(index);
            HexColor color = index < colors.size() ? HexColor.parse(colors.get(index)).orElse(UNSET_SLOT_COLOR) : UNSET_SLOT_COLOR;
            ItemStack display = itemFactory.createDyed(slotItem, MenuItemTokens.NONE, color);
            int colorSlot = index;
            add(menu, slotItem.slot(), ConfiguredMenuButtons.create(player, slotItem, display,
                    context -> onColorSlot.accept(context.player(), colorSlot)));
        }
    }

    private static void add(ChestMenu.Builder menu, int slot, MenuButton button) {
        if (button != null && slot >= 0) {
            menu.button(slot, button);
        }
    }
}
