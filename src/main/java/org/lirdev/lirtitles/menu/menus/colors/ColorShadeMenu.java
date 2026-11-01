package org.lirdev.lirtitles.menu.menus.colors;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.ColorShadeMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.colors.PaletteColor;
import org.lirdev.lirtitles.menu.ChestMenu;
import org.lirdev.lirtitles.menu.MenuButton;
import org.lirdev.lirtitles.menu.menus.shared.ConfiguredMenuButtons;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;
import org.lirdev.lirtitles.menu.menus.shared.StaticMenuItems;
import org.lirdev.lirtitles.utilities.HexColor;
import org.lirdev.lirtitles.utilities.PlaceholderReplacer;

public final class ColorShadeMenu {
    private final LirTitles plugin;
    private final MenuItemFactory itemFactory;
    private final StaticMenuItems staticItems;
    private volatile GeneratedShadeLayout generatedLayout;

    public ColorShadeMenu(LirTitles plugin) {
        this.plugin = plugin;
        itemFactory = new MenuItemFactory();
        staticItems = new StaticMenuItems(itemFactory);
    }

    public void clearCaches() {
        staticItems.clearCaches();
        itemFactory.clearCaches();
        generatedLayout = null;
    }

    public ChestMenu create(Player player, PaletteColor mainColor, HexColor selected, Consumer<HexColor> onSelect,
                            Consumer<Player> onCustomColor, Consumer<Player> onBack) {
        ColorShadeMenuConfiguration appearance = plugin.configuration().colorShadeMenu();
        Component title = PlaceholderReplacer.replace(appearance.title(), Map.of("color", plugin.colorizer().colorize(mainColor.id())));
        ChestMenu.Builder menu = ChestMenu.builder(title, appearance.rows());
        staticItems.addTo(menu, player, appearance.staticItems());
        add(menu, appearance.backItem().slot(), ConfiguredMenuButtons.create(player, appearance.backItem(),
                itemFactory.create(appearance.backItem(), MenuItemTokens.NONE), context -> onBack.accept(context.player())));
        add(menu, appearance.customItem().slot(), ConfiguredMenuButtons.create(player, appearance.customItem(),
                itemFactory.create(appearance.customItem(), MenuItemTokens.NONE), context -> onCustomColor.accept(context.player())));
        if (mainColor.shades().isEmpty()) {
            addGeneratedShades(menu, player, appearance, mainColor, selected, onSelect);
        } else {
            addConfiguredShades(menu, player, mainColor, selected, onSelect);
        }
        return menu.build();
    }

    private void addConfiguredShades(ChestMenu.Builder menu, Player player, PaletteColor mainColor, HexColor selected,
                                     Consumer<HexColor> onSelect) {
        for (PaletteColor shade : mainColor.shades()) {
            MenuItemTokens tokens = selectionTokens(shade.hex(), selected);
            ItemStack display = itemFactory.createDyedCached(shade.item(), tokens, shade.hex());
            add(menu, shade.item().slot(), ConfiguredMenuButtons.create(player, shade.item(), display,
                    context -> onSelect.accept(shade.hex())));
        }
    }

    private void addGeneratedShades(ChestMenu.Builder menu, Player player, ColorShadeMenuConfiguration appearance,
                                    PaletteColor mainColor, HexColor selected, Consumer<HexColor> onSelect) {
        GeneratedShadeLayout layout = layoutFor(appearance);
        List<HexColor> shades = layout.shadesOf(mainColor.hex());
        for (int index = 0; index < shades.size(); index++) {
            HexColor shade = shades.get(index);
            MenuItemTokens tokens = selectionTokens(shade, selected);
            ItemStack display = itemFactory.createDyedCached(appearance.shadeItem(), tokens, shade);
            add(menu, layout.slotAt(index), ConfiguredMenuButtons.create(player, appearance.shadeItem(), display,
                    context -> onSelect.accept(shade)));
        }
    }

    private GeneratedShadeLayout layoutFor(ColorShadeMenuConfiguration appearance) {
        GeneratedShadeLayout current = generatedLayout;
        if (current != null && current.describes(appearance)) {
            return current;
        }
        GeneratedShadeLayout created = new GeneratedShadeLayout(appearance);
        generatedLayout = created;
        return created;
    }

    private static MenuItemTokens selectionTokens(HexColor shade, HexColor selected) {
        boolean isSelected = selected != null && selected.equals(shade);
        return MenuItemTokens.of(Map.of(
                "selected", Component.text(isSelected ? "✔" : ""),
                "hex", Component.text("(" + shade.hex() + ")").color(NamedTextColor.GRAY)));
    }

    private static void add(ChestMenu.Builder menu, int slot, MenuButton button) {
        if (button != null && slot >= 0) {
            menu.button(slot, button);
        }
    }
}
