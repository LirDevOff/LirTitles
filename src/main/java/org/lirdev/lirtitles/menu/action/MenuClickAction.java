package org.lirdev.lirtitles.menu.action;

@FunctionalInterface
public interface MenuClickAction {
    void execute(MenuClickContext context);
}
