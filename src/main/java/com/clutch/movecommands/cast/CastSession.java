package com.clutch.movecommands.cast;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

public record CastSession(
        CastType type,
        Location startLocation,
        BukkitTask task,
        long startedAt
) {
    public void cancelTask() {
        if (!task.isCancelled()) {
            task.cancel();
        }
    }

    public boolean changedBlock(Location location) {
        if (location == null) {
            return false;
        }

        Location start = startLocation;
        return start.getWorld() != location.getWorld()
                || start.getBlockX() != location.getBlockX()
                || start.getBlockY() != location.getBlockY()
                || start.getBlockZ() != location.getBlockZ();
    }
}
