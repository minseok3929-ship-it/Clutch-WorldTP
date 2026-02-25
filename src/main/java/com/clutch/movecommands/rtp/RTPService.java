package com.clutch.movecommands.rtp;

import com.clutch.movecommands.ClutchMoveCommandsPlugin;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class RTPService {

    private final ClutchMoveCommandsPlugin plugin;

    public RTPService(ClutchMoveCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    public void findAndTeleportAsync(Player player,
                                     int minRadius,
                                     int maxRadius,
                                     int maxAttempts,
                                     Runnable onSuccess,
                                     Runnable onFail) {
        attemptCandidate(player, minRadius, maxRadius, maxAttempts, 0, onSuccess, onFail);
    }

    private void attemptCandidate(Player player,
                                  int minRadius,
                                  int maxRadius,
                                  int maxAttempts,
                                  int attemptCount,
                                  Runnable onSuccess,
                                  Runnable onFail) {
        if (!player.isOnline()) {
            return;
        }

        if (attemptCount >= maxAttempts) {
            onFail.run();
            return;
        }

        World world = player.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double angle = random.nextDouble(0, Math.PI * 2);
        int radius = random.nextInt(minRadius, maxRadius + 1);

        int x = (int) Math.round(Math.cos(angle) * radius);
        int z = (int) Math.round(Math.sin(angle) * radius);

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        world.getChunkAtAsync(chunkX, chunkZ, true).thenAccept(chunk ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    if (!player.getWorld().getName().equalsIgnoreCase("wild")) {
                        onFail.run();
                        return;
                    }

                    Location safeLocation = findSafeLocationInLoadedChunk(world, x, z);
                    if (safeLocation == null) {
                        attemptCandidate(player, minRadius, maxRadius, maxAttempts, attemptCount + 1, onSuccess, onFail);
                        return;
                    }

                    player.teleportAsync(safeLocation).thenAccept(success ->
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (success) {
                                    onSuccess.run();
                                } else {
                                    attemptCandidate(player, minRadius, maxRadius, maxAttempts, attemptCount + 1, onSuccess, onFail);
                                }
                            })
                    );
                })
        ).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> attemptCandidate(player, minRadius, maxRadius, maxAttempts, attemptCount + 1, onSuccess, onFail));
            return null;
        });
    }

    private Location findSafeLocationInLoadedChunk(World world, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
        Block ground = world.getBlockAt(x, highestY, z);
        Block feet = world.getBlockAt(x, highestY + 1, z);
        Block head = world.getBlockAt(x, highestY + 2, z);

        if (!isSafeGround(ground)) {
            return null;
        }

        if (!isPassable(feet) || !isPassable(head)) {
            return null;
        }

        return new Location(world, x + 0.5, highestY + 1, z + 0.5);
    }

    private boolean isSafeGround(Block block) {
        if (!block.getType().isSolid()) {
            return false;
        }

        Material material = block.getType();
        return material != Material.WATER
                && material != Material.LAVA
                && material != Material.KELP
                && material != Material.SEAGRASS
                && material != Material.TALL_SEAGRASS;
    }

    private boolean isPassable(Block block) {
        return block.isPassable() || block.getType() == Material.AIR;
    }
}
