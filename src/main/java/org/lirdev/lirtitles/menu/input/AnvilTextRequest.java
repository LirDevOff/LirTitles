package org.lirdev.lirtitles.menu.input;

import java.util.Objects;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;

public record AnvilTextRequest(Component title, AnvilTextLayout layout, Consumer<String> onConfirm, Runnable onCancel) {
    public AnvilTextRequest {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(onConfirm, "onConfirm");
        Objects.requireNonNull(onCancel, "onCancel");
    }
}
