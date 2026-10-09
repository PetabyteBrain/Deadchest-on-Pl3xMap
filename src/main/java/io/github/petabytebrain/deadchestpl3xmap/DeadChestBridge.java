package io.github.petabytebrain.deadchestpl3xmap;

import me.crylonz.deadchest.ChestData;
import me.crylonz.deadchest.DeadChestAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads the live list of DeadChests from the DeadChest plugin.
 * <p>
 * DeadChest only exposes a per-player API ({@link DeadChestAPI#getChests(Player)}), which would
 * miss chests of offline players. So we first try to reach DeadChest's global in-memory chest
 * store (reflectively, so a DeadChest update that moves things around can't crash us) and only
 * fall back to the public per-player API if that fails.
 * <p>
 * All methods are safe to call from Pl3xMap's async marker thread.
 */
public final class DeadChestBridge {
    private static final String PKG = "me.crylonz.deadchest.";
    /** How long a chest stays hidden after a pickup event, in case DeadChest removes it with a delay. */
    private static final long CLAIM_GRACE_MILLIS = 15_000L;

    private final Logger logger;
    private final @Nullable Supplier<Collection<?>> globalSource;
    private final @Nullable Method remainingMillisMethod;
    private final @Nullable Method publicLootMethod;
    private final Map<String, Long> recentlyClaimed = new ConcurrentHashMap<>();
    private volatile boolean warnedFailure;

    public DeadChestBridge(Logger logger) {
        this.logger = logger;
        this.globalSource = resolveGlobalSource();
        this.remainingMillisMethod = findStatic("DeadChestManager", "getCurrentPhaseRemainingMillis", ChestData.class, Date.class);
        this.publicLootMethod = findStatic("DeadChestManager", "isPublicLootPhase", ChestData.class, Date.class);
    }

    /**
     * @return true if we can see every chest (also those of offline players)
     */
    public boolean hasGlobalAccess() {
        return this.globalSource != null;
    }

    /**
     * Called when DeadChest fires its pickup event, so the marker vanishes immediately.
     */
    public void markClaimed(ChestData chest) {
        Location loc = chest.getChestLocation();
        if (loc == null) {
            return;
        }
        this.recentlyClaimed.put(key(worldNameOf(chest), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                System.currentTimeMillis() + CLAIM_GRACE_MILLIS);
    }

    /**
     * Snapshot of all DeadChests that are currently placed in the world.
     */
    public List<ChestSnapshot> snapshot() {
        long now = System.currentTimeMillis();
        this.recentlyClaimed.values().removeIf(until -> until < now);

        Collection<?> raw = rawChests();
        List<ChestSnapshot> result = new ArrayList<>(raw.size());
        Date nowDate = new Date(now);
        for (Object obj : raw) {
            if (!(obj instanceof ChestData chest)) {
                continue;
            }
            try {
                ChestSnapshot snap = toSnapshot(chest, nowDate);
                if (snap != null && !this.recentlyClaimed.containsKey(key(snap.worldName(), snap.x(), snap.y(), snap.z()))) {
                    result.add(snap);
                }
            } catch (Throwable t) {
                logOnce("Could not read a DeadChest entry", t);
            }
        }
        return result;
    }

    private Collection<?> rawChests() {
        if (this.globalSource != null) {
            try {
                return this.globalSource.get();
            } catch (Throwable t) {
                logOnce("Failed to read DeadChest's chest store, falling back to online players", t);
            }
        }
        // Fallback: public API, online players only
        List<ChestData> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                list.addAll(DeadChestAPI.getChests(player));
            } catch (Throwable t) {
                logOnce("DeadChestAPI.getChests failed", t);
            }
        }
        return list;
    }

    private @Nullable ChestSnapshot toSnapshot(ChestData chest, Date now) {
        // DeadChest sets this flag when the timer ran out and the block was already removed
        if (chest.isRemovedBlock()) {
            return null;
        }
        Location loc = chest.getChestLocation();
        if (loc == null) {
            return null;
        }
        String worldName = worldNameOf(chest);
        if (worldName == null) {
            return null;
        }

        int items = 0;
        List<ItemStack> inv = chest.getInventory();
        if (inv != null) {
            for (ItemStack stack : inv) {
                if (stack != null && !stack.getType().isAir()) {
                    items++;
                }
            }
        }

        Date died = chest.getChestDate();
        Long timeLeft = invokeLong(this.remainingMillisMethod, chest, now);
        if (chest.isInfinity()) {
            timeLeft = -1L;
        }

        return new ChestSnapshot(
                worldName,
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                chest.getPlayerName() == null ? "?" : chest.getPlayerName(),
                died == null ? now.getTime() : died.getTime(),
                chest.isInfinity(),
                items,
                chest.getXpStored(),
                timeLeft,
                invokeBoolean(this.publicLootMethod, chest, now)
        );
    }

    private static @Nullable String worldNameOf(ChestData chest) {
        String name = chest.getWorldName();
        if (name != null) {
            return name;
        }
        Location loc = chest.getChestLocation();
        World world = loc == null ? null : loc.getWorld();
        return world == null ? null : world.getName();
    }

    private static String key(@Nullable String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    // ------------------------------------------------------------------ reflection helpers

    private @Nullable Supplier<Collection<?>> resolveGlobalSource() {
        // DeadChest 4.2x+: DeadChestLoader.getChestDataCache().getChestData()
        try {
            Class<?> loaderClass = deadChestClass("DeadChestLoader");
            Method cacheGetter = loaderClass.getMethod("getChestDataCache");
            Object store = cacheGetter.invoke(null);
            if (store != null) {
                Method listGetter = store.getClass().getMethod("getChestData");
                return () -> {
                    try {
                        Object currentStore = cacheGetter.invoke(null);
                        return asCollection(listGetter.invoke(currentStore));
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                };
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }

        // Older DeadChest versions kept a public static "chestData" list
        for (String owner : new String[]{"DeadChestLoader", "DeadChest"}) {
            try {
                Field field = deadChestClass(owner).getField("chestData");
                if (Modifier.isStatic(field.getModifiers())) {
                    return () -> {
                        try {
                            return asCollection(field.get(null));
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    };
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return null;
    }

    private static Collection<?> asCollection(@Nullable Object value) {
        if (value instanceof Map<?, ?> map) {
            return new ArrayList<>(map.values());
        }
        if (value instanceof Collection<?> collection) {
            // copy so DeadChest modifying its list doesn't break our iteration
            synchronized (collection) {
                return new ArrayList<>(collection);
            }
        }
        return Collections.emptyList();
    }

    private static Class<?> deadChestClass(String simpleName) throws ClassNotFoundException {
        return Class.forName(PKG + simpleName, true, ChestData.class.getClassLoader());
    }

    private static @Nullable Method findStatic(String owner, String name, Class<?>... params) {
        try {
            Method method = deadChestClass(owner).getMethod(name, params);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private @Nullable Long invokeLong(@Nullable Method method, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(null, args);
            return value instanceof Number n ? n.longValue() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private @Nullable Boolean invokeBoolean(@Nullable Method method, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(null, args);
            return value instanceof Boolean b ? b : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private void logOnce(String message, Throwable t) {
        if (!this.warnedFailure) {
            this.warnedFailure = true;
            this.logger.log(Level.WARNING, message + " (further errors are suppressed)", t);
        }
    }
}
