package org.lirdev.lirtitles.utilities;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import org.lirdev.lirtitles.permissions.Permissions;

public final class UpdateChecker implements Listener {
    private static final String ALERT_PERMISSION = Permissions.ALERT;
    public static final long INITIAL_CHECK_DELAY_SECONDS = 0L;
    private static final long CHECK_INTERVAL_MINUTES = 60L;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final JavaPlugin plugin;
    private final Logger logger;
    private final String currentVersion;
    private final String versionsUrl;
    private final String releasePageUrl;
    private final boolean enabled;
    private volatile HttpClient httpClient;
    private final AtomicReference<String> availableVersion = new AtomicReference<>();

    public UpdateChecker(JavaPlugin plugin, Logger logger, String projectId, boolean enabled) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(projectId, "projectId");
        this.currentVersion = plugin.getPluginMeta().getVersion();
        this.versionsUrl = "https://api.modrinth.com/v2/project/" + projectId + "/version";
        this.releasePageUrl = "https://modrinth.com/plugin/" + projectId + "/version/";
        this.enabled = enabled;
    }

    public void start() {
        if (!enabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getAsyncScheduler()
                .runAtFixedRate(plugin, scheduledTask -> checkForUpdates(), INITIAL_CHECK_DELAY_SECONDS,
                        CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String version = availableVersion.get();
        if (version != null && player.hasPermission(ALERT_PERMISSION)) {
            player.sendMessage(alertMessage(version));
        }
    }

    private void checkForUpdates() {
        try {
            String latestVersion = fetchLatestVersion();
            if (latestVersion != null && isNewer(latestVersion, currentVersion)) {
                announce(latestVersion);
            }
        } catch (IOException exception) {
            logger.warn("Could not check for updates.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String fetchLatestVersion() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(versionsUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", plugin.getPluginMeta().getName() + "/" + currentVersion)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Modrinth API returned status " + response.statusCode());
        }
        JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
        if (versions.isEmpty()) {
            return null;
        }
        return versions.get(0).getAsJsonObject().get("version_number").getAsString();
    }

    private void announce(String latestVersion) {
        availableVersion.set(latestVersion);
        logger.raw(consoleMessage(latestVersion));
        notifyOnlinePlayers(latestVersion);
    }

    private void notifyOnlinePlayers(String latestVersion) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission(ALERT_PERMISSION)) {
                player.getScheduler().run(plugin, scheduledTask -> player.sendMessage(alertMessage(latestVersion)), null);
            }
        }
    }

    private Component consoleMessage(String latestVersion) {
        return Component.textOfChildren(
                messagePrefix(),
                newVersionAnnouncement(latestVersion),
                Component.text(" is available: ", Branding.SECONDARY),
                Component.text(releasePageUrl + latestVersion, Branding.PRIMARY),
                currentVersionNote());
    }

    private Component alertMessage(String latestVersion) {
        return Component.textOfChildren(
                messagePrefix(),
                newVersionAnnouncement(latestVersion),
                Component.text(" is available ", Branding.SECONDARY),
                releaseLink(releasePageUrl + latestVersion),
                currentVersionNote());
    }

    private Component messagePrefix() {
        return Component.textOfChildren(
                Component.text("[", Branding.PRIMARY),
                Component.text(plugin.getPluginMeta().getName(), Branding.PRIMARY, TextDecoration.BOLD),
                Component.text("]", Branding.PRIMARY),
                Component.text(" → ", NamedTextColor.WHITE));
    }

    private static Component newVersionAnnouncement(String latestVersion) {
        return Component.textOfChildren(
                Component.text("A new version ", Branding.SECONDARY),
                Component.text(latestVersion, Branding.PRIMARY));
    }

    private static Component releaseLink(String url) {
        return Component.text("[link]", Branding.PRIMARY, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text(url, Branding.SECONDARY)));
    }

    private Component currentVersionNote() {
        return Component.text(" (current: v" + currentVersion + ")", NamedTextColor.GRAY);
    }

    private HttpClient httpClient() {
        HttpClient existing = httpClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (httpClient == null) {
                httpClient = HttpClient.newHttpClient();
            }
            return httpClient;
        }
    }

    private static boolean isNewer(String candidate, String current) {
        String[] candidateParts = candidate.split("\\.");
        String[] currentParts = current.split("\\.");
        int length = Math.max(candidateParts.length, currentParts.length);
        for (int index = 0; index < length; index++) {
            int candidatePart = part(candidateParts, index);
            int currentPart = part(currentParts, index);
            if (candidatePart != currentPart) {
                return candidatePart > currentPart;
            }
        }
        return false;
    }

    private static int part(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
