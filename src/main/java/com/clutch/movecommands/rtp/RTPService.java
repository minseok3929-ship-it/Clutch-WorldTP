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
                                     World world,
                                     int range,
                                     int maxAttempts,
                                     Runnable onSuccess,
                                     Runnable onFail) {
        attemptCandidate(player, world, Math.max(0, range), maxAttempts, 0, onSuccess, onFail);
    }

    private void attemptCandidate(Player player,
                                  World world,
                                  int range,
                                  int maxAttempts,
                                  int attemptCount,
                                  Runnable onSuccess,
                                  Runnable onFail) {
        if (!player.isOnline()) {
            return;
        }

        if (!isWildOverworld(world)) {
            onFail.run();
            return;
        }

        if (attemptCount >= maxAttempts) {
            onFail.run();
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int x = random.nextInt(-range, range + 1);
        int z = random.nextInt(-range, range + 1);

        Location borderCheckLocation = new Location(world, x + 0.5, world.getSpawnLocation().getY(), z + 0.5);
        if (!world.getWorldBorder().isInside(borderCheckLocation)) {
            attemptCandidate(player, world, range, maxAttempts, attemptCount + 1, onSuccess, onFail);
            return;
        }

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        world.getChunkAtAsync(chunkX, chunkZ, true).thenAccept(chunk ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    Location safeLocation = findSafeLocationInLoadedChunk(world, x, z);
                    if (safeLocation == null) {
                        attemptCandidate(player, world, range, maxAttempts, attemptCount + 1, onSuccess, onFail);
                        return;
                    }

                    player.teleportAsync(safeLocation).thenAccept(success ->
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (success) {
                                    onSuccess.run();
                                } else {
                                    attemptCandidate(player, world, range, maxAttempts, attemptCount + 1, onSuccess, onFail);
                                }
                            })
                    );
                })
        ).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> attemptCandidate(player, world, range, maxAttempts, attemptCount + 1, onSuccess, onFail));
            return null;
        });
    }

    private Location findSafeLocationInLoadedChunk(World world, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
        Block ground = world.getBlockAt(x, highestY, z);
        Block feet = world.getBlockAt(x, highestY + 1, z);
        Block head = world.getBlockAt(x, highestY + 2, z);

        Location location = new Location(world, x + 0.5, highestY + 1, z + 0.5);
        if (!world.getWorldBorder().isInside(location)) {
            return null;
        }

        if (!isSafeGround(ground)) {
            return null;
        }

        if (!isPassable(feet) || !isPassable(head)) {
            return null;
        }

        return location;
    }

    private boolean isWildOverworld(World world) {
        return world.getEnvironment() == World.Environment.NORMAL;
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
                && material != Material.TALL_SEAGRASS
                && material != Material.MAGMA_BLOCK
                && material != Material.CACTUS
                && material != Material.CAMPFIRE
                && material != Material.SOUL_CAMPFIRE
                && material != Material.FIRE
                && material != Material.SOUL_FIRE
                && material != Material.POWDER_SNOW;
    }

    private boolean isPassable(Block block) {
        return block.isPassable() || block.getType() == Material.AIR;
    }
}
