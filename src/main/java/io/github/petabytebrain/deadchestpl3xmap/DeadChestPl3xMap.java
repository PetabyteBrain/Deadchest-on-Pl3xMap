package io.github.petabytebrain.deadchestpl3xmap;

import me.crylonz.deadchest.DeadchestPickUpEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Shows every DeadChest as a marker on Pl3xMap.
 * <p>
 * How it works:
 * <ul>
 *     <li>Someone dies → DeadChest stores a chest → our layer picks it up on Pl3xMap's next
 *     layer refresh ({@code layer.update-interval}, default 2 s) and draws an icon.</li>
 *     <li>The chest is claimed → DeadChest fires {@link DeadchestPickUpEvent} → the marker is
 *     hidden right away and DeadChest removes the chest from its store.</li>
 *     <li>The DeadChest timer runs out (or an admin removes / gives back the chest) → the chest
 *     is gone from DeadChest's store → the marker is gone on the next refresh.</li>
 * </ul>
 */
public final class DeadChestPl3xMap extends JavaPlugin implements Listener {
    private volatile Settings settings;
    private DeadChestBridge bridge;
    private MapHook mapHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = Settings.load(getConfig());

        this.bridge = new DeadChestBridge(getLogger());
        if (this.bridge.hasGlobalAccess()) {
            getLogger().info("Hooked into DeadChest – showing all dead chests.");
        } else {
            getLogger().warning("Could not access DeadChest's chest list – only chests of ONLINE players will be shown. "
                    + "Please update DeadChest.");
        }

        getServer().getPluginManager().registerEvents(this, this);

        this.mapHook = new MapHook(this);
        this.mapHook.start();
        getLogger().info("Pl3xMap layer '" + this.settings.layerLabel() + "' ready.");
    }

    @Override
    public void onDisable() {
        if (this.mapHook != null) {
            this.mapHook.stop();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeadChestPickUp(DeadchestPickUpEvent event) {
        this.bridge.markClaimed(event.getChest());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                reloadConfig();
                this.settings = Settings.load(getConfig());
                this.mapHook.registerAll();
                sender.sendMessage("DeadChestPl3xMap reloaded.");
            }
            case "status" -> {
                int count = this.bridge.snapshot().size();
                sender.sendMessage("DeadChestPl3xMap: " + count + " dead chest(s) on the map"
                        + (this.bridge.hasGlobalAccess() ? "." : " (online players only)."));
            }
            default -> sender.sendMessage("Usage: /" + label + " reload|status");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return List.of("reload", "status").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }

    public Settings settings() {
        return this.settings;
    }

    public DeadChestBridge bridge() {
        return this.bridge;
    }
}
