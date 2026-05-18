package com.clutch.movecommands.listener;

import com.clutch.movecommands.ClutchMoveCommandsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final ClutchMoveCommandsPlugin plugin;

    public PlayerRespawnListener(ClutchMoveCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equalsIgnoreCase("wild")) {
            return;
        }

        World home = Bukkit.getWorld("home");
        if (home == null) {
            return;
        }

        event.setRespawnLocation(home.getSpawnLocation());
        plugin.getServer().getScheduler().runTask(plugin,
                () -> player.sendTitle("§8CLUTCH", "§f스폰으로 복귀했습니다.", 0, 40, 10));
    }
}
