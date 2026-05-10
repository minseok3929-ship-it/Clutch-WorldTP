package com.clutch.movecommands.command;

import com.clutch.movecommands.ClutchMoveCommandsPlugin;
import com.clutch.movecommands.cast.CastType;
import com.clutch.movecommands.cast.TeleportCastService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final ClutchMoveCommandsPlugin plugin;
    private final TeleportCastService castService;

    public SpawnCommand(ClutchMoveCommandsPlugin plugin, TeleportCastService castService) {
        this.plugin = plugin;
        this.castService = castService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefixed("§c플레이어만 사용할 수 있습니다."));
            return true;
        }

        if (!player.hasPermission("clutch.move.spawn")) {
            player.sendMessage(plugin.prefixed("§c권한이 없습니다."));
            return true;
        }

        int castSeconds = plugin.getConfig().getInt("cast.seconds", 3);
        return castService.startCast(player, CastType.SPAWN, castSeconds, () -> completeSpawn(player));
    }

    private void completeSpawn(Player player) {
        String worldName = plugin.getConfig().getString("worlds.spawn", "home");
        World spawnWorld = Bukkit.getWorld(worldName);
        if (spawnWorld == null) {
            player.sendTitle("§8CLUTCH", "§c스폰 월드를 찾을 수 없습니다.", 0, 40, 10);
            return;
        }

        Location spawnLocation = spawnWorld.getSpawnLocation();
        player.teleportAsync(spawnLocation).thenAccept(success ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendTitle("§8CLUTCH", "§f스폰으로 이동했습니다!", 0, 40, 10);
                    } else {
                        player.sendTitle("§8CLUTCH", "§c스폰으로 이동하지 못했습니다.", 0, 40, 10);
                    }
                })
        );
    }
}
