package com.clutch.movecommands.rtp;

import com.clutch.movecommands.ClutchMoveCommandsPlugin;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class RTPService {

    private final ClutchMoveCommandsPlugin plugin;

    public RTPService(ClutchMoveCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    public Optional<Location> findSafeLocation(World world, int minRadius, int maxRadius, int maxAttempts) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < maxAttempts; i++) {
            double angle = random.nextDouble(0, Math.PI * 2);
            int radius = random.nextInt(minRadius, maxRadius + 1);

            int x = (int) Math.round(Math.cos(angle) * radius);
            int z = (int) Math.round(Math.sin(angle) * radius);

            int highestY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
            Block ground = world.getBlockAt(x, highestY, z);
            Block feet = world.getBlockAt(x, highestY + 1, z);
            Block head = world.getBlockAt(x, highestY + 2, z);

            if (!isSafeGround(ground)) {
                continue;
            }

            if (!isPassable(feet) || !isPassable(head)) {
                continue;
            }

            Location location = new Location(world, x + 0.5, highestY + 1, z + 0.5);
            return Optional.of(location);
        }

        return Optional.empty();
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
