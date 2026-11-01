package org.lirdev.lirtitles.menu.input;

import java.util.Optional;

import org.bukkit.inventory.ItemStack;

public interface AnvilTextLayout {
    ItemStack cancelItem();

    ItemStack previewItem(String text);

    Optional<ItemStack> confirmItem(String text);
}
