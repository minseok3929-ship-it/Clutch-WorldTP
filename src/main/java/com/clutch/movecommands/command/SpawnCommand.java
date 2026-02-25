package com.clutch.movecommands.command;

import com.clutch.movecommands.ClutchMoveCommandsPlugin;
import com.clutch.movecommands.cast.CastType;
import com.clutch.movecommands.cast.TeleportCastService;
import org.bukkit.Bukkit;
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
        player.sendMessage(plugin.prefixed("§f" + castSeconds + "초 후 스폰으로 이동합니다. 이동 시 취소됩니다."));

        boolean started = castService.startCast(player, CastType.SPAWN, castSeconds, () -> completeSpawn(player));
        if (!started) {
            return true;
        }

        return true;
    }

    private void completeSpawn(Player player) {
        String template = plugin.getConfig().getString("multiverse.spawnCommand", "mvtp home %player%");
        String command = template.replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        player.sendMessage(plugin.prefixed("§f스폰으로 이동했습니다!"));
    }
}
