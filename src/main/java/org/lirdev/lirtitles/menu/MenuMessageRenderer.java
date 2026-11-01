package org.lirdev.lirtitles.menu;

import net.kyori.adventure.text.Component;

@FunctionalInterface
public interface MenuMessageRenderer {
    Component render(Component template);
}
