package org.lirdev.lirtitles.menu.input.bukkit;

import java.util.Objects;
import java.util.Optional;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.event.inventory.PrepareAnvilEvent;

import org.lirdev.lirtitles.menu.input.AnvilTextRequest;

final class BukkitAnvilTextSession {
    static final int CANCEL_SLOT = 0;
    static final int PREVIEW_SLOT = 1;
    static final int CONFIRM_SLOT = 2;

    private final AnvilView view;
    private final AnvilTextRequest request;
    private String shownText = "";
    private String preparedText;
    private boolean preparedTextAcceptable;
    private boolean closed;
    private boolean completed;

    BukkitAnvilTextSession(AnvilView view, AnvilTextRequest request) {
        this.view = Objects.requireNonNull(view, "view");
        this.request = Objects.requireNonNull(request, "request");
    }

    boolean owns(InventoryView candidate) {
        return candidate.getTopInventory() == view.getTopInventory();
    }

    void render() {
        Inventory inventory = view.getTopInventory();
        inventory.setItem(CANCEL_SLOT, request.layout().cancelItem());
        inventory.setItem(PREVIEW_SLOT, request.layout().previewItem(shownText));
    }

    private void refreshPreview(String text) {
        if (shownText.equals(text)) {
            return;
        }
        shownText = text;
        view.getTopInventory().setItem(PREVIEW_SLOT, request.layout().previewItem(text));
    }

    void prepare(PrepareAnvilEvent event) {
        if (closed) {
            event.setResult(null);
            return;
        }
        String text = typedText();
        refreshPreview(text);
        var confirmItem = request.layout().confirmItem(text);
        preparedText = text;
        preparedTextAcceptable = confirmItem.isPresent();
        event.setResult(confirmItem.orElse(null));
    }

    void confirm() {
        if (completed) {
            return;
        }
        String text = typedText();
        boolean acceptable = text.equals(preparedText)
                ? preparedTextAcceptable
                : request.layout().confirmItem(text).isPresent();
        if (!acceptable) {
            return;
        }
        completed = true;
        dispose();
        request.onConfirm().accept(text);
    }

    void cancel() {
        if (completed) {
            return;
        }
        completed = true;
        dispose();
        request.onCancel().run();
    }

    void dispose() {
        if (closed) {
            return;
        }
        closed = true;
        Inventory inventory = view.getTopInventory();
        inventory.setItem(CANCEL_SLOT, null);
        inventory.setItem(PREVIEW_SLOT, null);
    }

    private String typedText() {
        String renameText = view.getRenameText();
        return renameText == null ? "" : renameText.strip();
    }
}
