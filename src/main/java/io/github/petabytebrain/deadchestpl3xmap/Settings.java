package io.github.petabytebrain.deadchestpl3xmap;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable snapshot of config.yml.
 */
public record Settings(
        String layerLabel,
        int updateInterval,
        boolean liveUpdate,
        boolean showControls,
        boolean defaultHidden,
        int priority,
        int zIndex,
        int iconSize,
        boolean showInfiniteChests,
        String tooltip,
        String popup,
        String statusPrivate,
        String statusPublic,
        String timeNever,
        Set<String> disabledWorlds
) {

    public static Settings load(FileConfiguration config) {
        List<String> disabled = config.getStringList("disabled-worlds");
        return new Settings(
                config.getString("layer.label", "Dead Chests"),
                Math.max(1, config.getInt("layer.update-interval", 2)),
                config.getBoolean("layer.live-update", true),
                config.getBoolean("layer.show-controls", true),
                config.getBoolean("layer.default-hidden", false),
                config.getInt("layer.priority", 50),
                config.getInt("layer.z-index", 50),
                Math.max(4, config.getInt("marker.icon-size", 20)),
                config.getBoolean("marker.show-infinite-chests", true),
                config.getString("marker.tooltip", "<b>{player}</b>'s dead chest"),
                config.getString("marker.popup", ""),
                config.getString("marker.status-private", "Owner only"),
                config.getString("marker.status-public", "Public loot"),
                config.getString("marker.time-never", "never expires"),
                Set.copyOf(new HashSet<>(disabled))
        );
    }
}
