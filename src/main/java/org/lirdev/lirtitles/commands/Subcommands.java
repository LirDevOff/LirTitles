package org.lirdev.lirtitles.commands;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.commands.implementation.admin.GiveCommand;
import org.lirdev.lirtitles.commands.implementation.admin.ReloadCommand;
import org.lirdev.lirtitles.commands.implementation.moderation.OpenModerationMenuCommand;
import org.lirdev.lirtitles.configuration.Configuration;
import org.lirdev.lirtitles.menu.MenuCloser;
import org.lirdev.lirtitles.menu.menus.TitleMenuController;

public final class Subcommands {
    private Subcommands() {
    }

    public static void configure(LirTitles plugin, Configuration configuration, MenuCloser menuCloser, TitleMenuController titleMenus,
                                 CommandTree commandTree) {
        commandTree.add(
                new ReloadCommand(plugin, configuration, menuCloser, titleMenus::clearConfigurationCaches),
                new GiveCommand(plugin, configuration),
                new OpenModerationMenuCommand(configuration, titleMenus)
        );
    }
}
