package io.github.petabytebrain.deadchestpl3xmap;

import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.event.EventHandler;
import net.pl3x.map.core.event.EventListener;
import net.pl3x.map.core.event.server.Pl3xMapEnabledEvent;
import net.pl3x.map.core.event.world.WorldLoadedEvent;
import net.pl3x.map.core.event.world.WorldUnloadedEvent;
import net.pl3x.map.core.image.IconImage;
import net.pl3x.map.core.world.World;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Registers the dead chest icon and one {@link DeadChestLayer} per Pl3xMap world,
 * and keeps them registered across Pl3xMap reloads and world loads.
 */
public final class MapHook implements EventListener {
    public static final String ICON_KEY = "deadchestpl3xmap_chest";

    private final DeadChestPl3xMap plugin;
    private volatile boolean active;

    public MapHook(DeadChestPl3xMap plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.active = true;
        // Pl3xMap has no way to unregister listeners, so we register only once
        // and use the "active" flag to ignore events while we are disabled.
        Pl3xMap.api().getEventRegistry().register(this);
        if (Pl3xMap.api().isEnabled()) {
            registerAll();
        }
    }

    public void stop() {
        this.active = false;
        if (!Pl3xMap.api().isEnabled()) {
            return;
        }
        for (World world : Pl3xMap.api().getWorldRegistry()) {
            world.getLayerRegistry().unregister(DeadChestLayer.KEY);
        }
        Pl3xMap.api().getIconRegistry().unregister(ICON_KEY);
    }

    /**
     * (Re)registers the icon and the layer on every loaded map world.
     */
    public void registerAll() {
        if (!this.active || !Pl3xMap.api().isEnabled()) {
            return;
        }
        registerIcon();
        for (World world : Pl3xMap.api().getWorldRegistry()) {
            registerLayer(world);
        }
    }

    @EventHandler
    public void onPl3xMapEnabled(Pl3xMapEnabledEvent event) {
        // Pl3xMap wipes registered icons on every (re)enable; worlds follow via WorldLoadedEvent
        if (this.active) {
            registerIcon();
        }
    }

    @EventHandler
    public void onWorldLoaded(WorldLoadedEvent event) {
        if (this.active) {
            registerLayer(event.getWorld());
        }
    }

    @EventHandler
    public void onWorldUnloaded(WorldUnloadedEvent event) {
        event.getWorld().getLayerRegistry().unregister(DeadChestLayer.KEY);
    }

    private void registerLayer(World world) {
        if (!world.isEnabled()) {
            return;
        }
        world.getLayerRegistry().register(
                new DeadChestLayer(world.getName(), this.plugin.bridge(), this.plugin::settings));
    }

    private void registerIcon() {
        try {
            BufferedImage image = loadIcon();
            Pl3xMap.api().getIconRegistry().register(new IconImage(ICON_KEY, image, "png"));
        } catch (Throwable t) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not register the dead chest icon on Pl3xMap", t);
        }
    }

    /**
     * Uses plugins/DeadChestPl3xMap/icon.png so server owners can swap the icon.
     */
    private BufferedImage loadIcon() throws IOException {
        File file = new File(this.plugin.getDataFolder(), "icon.png");
        if (!file.exists()) {
            this.plugin.saveResource("icon.png", false);
        }
        BufferedImage image = ImageIO.read(file);
        if (image != null) {
            return image;
        }
        this.plugin.getLogger().warning("icon.png is not a valid image, using the built-in icon");
        try (InputStream in = this.plugin.getResource("icon.png")) {
            if (in == null) {
                throw new IOException("Built-in icon.png missing from jar");
            }
            return ImageIO.read(in);
        }
    }
}
