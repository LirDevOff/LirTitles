package org.lirdev.lirtitles.menu.menus.customtitles;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import org.lirdev.lirtitles.configuration.menus.CustomTitleInputMenuConfiguration;
import org.lirdev.lirtitles.menu.input.AnvilTextLayout;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemTokens;

public final class ConfiguredTextInputLayout implements AnvilTextLayout {
    private final CustomTitleInputMenuConfiguration appearance;
    private final MenuItemFactory itemFactory;
    private final Predicate<String> acceptable;
    private final Function<String, Component> previewRenderer;
    private final int minLength;
    private final int maxLength;

    public ConfiguredTextInputLayout(CustomTitleInputMenuConfiguration appearance, MenuItemFactory itemFactory,
                                    Predicate<String> acceptable, Function<String, Component> previewRenderer,
                                    int minLength, int maxLength) {
        this.appearance = Objects.requireNonNull(appearance, "appearance");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory");
        this.acceptable = Objects.requireNonNull(acceptable, "acceptable");
        this.previewRenderer = Objects.requireNonNull(previewRenderer, "previewRenderer");
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public ItemStack cancelItem() {
        return itemFactory.create(appearance.cancelItem(), MenuItemTokens.NONE);
    }

    @Override
    public ItemStack previewItem(String text) {
        Component preview = text.isBlank() ? appearance.emptyPreview() : previewRenderer.apply(text);
        return itemFactory.create(appearance.previewItem(), tokens(preview));
    }

    @Override
    public Optional<ItemStack> confirmItem(String text) {
        if (!acceptable.test(text)) {
            return Optional.empty();
        }
        return Optional.of(itemFactory.create(appearance.confirmItem(), tokens(previewRenderer.apply(text))));
    }

    private MenuItemTokens tokens(Component preview) {
        return MenuItemTokens.of(Map.of(
                "title", preview,
                "preview", preview,
                "min", Component.text(minLength),
                "max", Component.text(maxLength)));
    }
}
