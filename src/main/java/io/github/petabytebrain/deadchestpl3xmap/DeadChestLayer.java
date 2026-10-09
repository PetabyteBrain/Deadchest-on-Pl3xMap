package io.github.petabytebrain.deadchestpl3xmap;

import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.Layer;
import net.pl3x.map.core.markers.marker.Icon;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.markers.option.Tooltip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * One Pl3xMap layer per world. Pl3xMap calls {@link #getMarkers()} every
 * {@code update-interval} seconds; we answer with whatever DeadChest currently has,
 * so new deaths appear and claimed/expired chests disappear automatically.
 */
public final class DeadChestLayer extends Layer {
    public static final String KEY = "deadchestpl3xmap_chests";

    private final String worldName;
    private final DeadChestBridge bridge;
    private final Supplier<Settings> settings;

    public DeadChestLayer(String worldName, DeadChestBridge bridge, Supplier<Settings> settings) {
        super(KEY, () -> settings.get().layerLabel());
        this.worldName = worldName;
        this.bridge = bridge;
        this.settings = settings;

        Settings s = settings.get();
        setUpdateInterval(s.updateInterval());
        setLiveUpdate(s.liveUpdate());
        setShowControls(s.showControls());
        setDefaultHidden(s.defaultHidden());
        setPriority(s.priority());
        setZIndex(s.zIndex());
    }

    @Override
    public Collection<Marker<?>> getMarkers() {
        Settings s = this.settings.get();
        if (s.disabledWorlds().contains(this.worldName)) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        List<Marker<?>> markers = new ArrayList<>();
        for (ChestSnapshot chest : this.bridge.snapshot()) {
            if (!this.worldName.equals(chest.worldName())) {
                continue;
            }
            if (chest.infinite() && !s.showInfiniteChests()) {
                continue;
            }
            markers.add(createMarker(chest, s, now));
        }
        return markers;
    }

    private static Marker<?> createMarker(ChestSnapshot chest, Settings s, long now) {
        String key = "deadchest_" + chest.x() + "_" + chest.y() + "_" + chest.z();
        Icon icon = Marker.icon(key, Point.of(chest.x() + 0.5D, chest.z() + 0.5D), MapHook.ICON_KEY, s.iconSize());

        Options.Builder options = Options.builder();
        if (!s.tooltip().isBlank()) {
            options.tooltipContent(Placeholders.apply(s.tooltip(), chest, s, now))
                    .tooltipDirection(Tooltip.Direction.TOP);
        }
        if (!s.popup().isBlank()) {
            options.popupContent(Placeholders.apply(s.popup(), chest, s, now));
        }
        return icon.setOptions(options.build());
    }
}
