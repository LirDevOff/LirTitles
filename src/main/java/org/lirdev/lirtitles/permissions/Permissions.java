package org.lirdev.lirtitles.permissions;

import org.lirdev.lirtitles.metadata.PluginInfo;

public final class Permissions {
    public static final String ADMIN = PluginInfo.ID + ".admin";
    public static final String ALERT = PluginInfo.ID + ".alert";
    public static final String MODERATE = PluginInfo.ID + ".moderate";
    public static final String UNLOCK_ALL = PluginInfo.ID + ".unlockall";

    private Permissions() {
    }
}
