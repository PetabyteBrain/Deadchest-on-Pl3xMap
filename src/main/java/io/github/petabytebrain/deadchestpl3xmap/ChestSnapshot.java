package io.github.petabytebrain.deadchestpl3xmap;

import org.jetbrains.annotations.Nullable;

/**
 * Plain copy of the DeadChest data we need to draw one marker.
 *
 * @param timeLeftMillis remaining time of the current DeadChest phase, {@code -1} if the chest
 *                       never expires, {@code null} if the installed DeadChest version can't tell us
 * @param publicLoot     whether the chest is in DeadChest's "public loot" phase, {@code null} if unknown
 */
public record ChestSnapshot(
        String worldName,
        int x,
        int y,
        int z,
        String playerName,
        long deathTimeMillis,
        boolean infinite,
        int items,
        int xp,
        @Nullable Long timeLeftMillis,
        @Nullable Boolean publicLoot
) {
}
