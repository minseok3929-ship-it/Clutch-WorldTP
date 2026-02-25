package com.clutch.movecommands.command;

import com.clutch.movecommands.ClutchMoveCommandsPlugin;
import com.clutch.movecommands.cast.CastType;
import com.clutch.movecommands.cast.TeleportCastService;
import com.clutch.movecommands.rtp.RTPService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WildCommand implements CommandExecutor {

    private final ClutchMoveCommandsPlugin plugin;
    private final TeleportCastService castService;
    private final RTPService rtpService;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public WildCommand(ClutchMoveCommandsPlugin plugin, TeleportCastService castService, RTPService rtpService) {
        this.plugin = plugin;
        this.castService = castService;
        this.rtpService = rtpService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefixed("§c플레이어만 사용할 수 있습니다."));
            return true;
        }

        if (!player.hasPermission("clutch.move.wild")) {
            player.sendMessage(plugin.prefixed("§c권한이 없습니다."));
            return true;
        }

        long now = System.currentTimeMillis();
        long until = cooldownUntil.getOrDefault(player.getUniqueId(), 0L);
        if (until > now) {
            long remain = (until - now) / 1000L;
            long minutes = remain / 60;
            long seconds = remain % 60;
            player.sendMessage(plugin.prefixed(String.format("§c쿨타임입니다! 남은 시간 : %02d분 %02d초.", minutes, seconds)));
            return true;
        }

        int castSeconds = plugin.getConfig().getInt("cast.seconds", 3);
        player.sendMessage(plugin.prefixed("§f" + castSeconds + "초 후 야생으로 이동합니다. 이동 시 취소됩니다."));

        boolean started = castService.startCast(player, CastType.WILD, castSeconds, () -> completeWild(player));
        if (!started) {
            return true;
        }

        return true;
    }

    private void completeWild(Player player) {
        String wildcard = plugin.getConfig().getString("multiverse.wildCommand", "mvtp Wild %player%");
        String command = wildcard.replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            String worldName = plugin.getConfig().getString("worlds.wild", "Wild");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(plugin.prefixed("§c안전한 위치를 찾지 못했습니다. 잠시 후 다시 시도해주세요."));
                return;
            }

            int minRadius = plugin.getConfig().getInt("rtp.minRadius", 500);
            int maxRadius = plugin.getConfig().getInt("rtp.maxRadius", 8000);
            int maxAttempts = plugin.getConfig().getInt("rtp.maxAttempts", 30);
            int cooldownSeconds = plugin.getConfig().getInt("rtp.cooldownSeconds", 180);

            rtpService.findSafeLocation(world, minRadius, maxRadius, maxAttempts).ifPresentOrElse(location -> {
                player.teleport(location);
                cooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
                player.sendMessage(plugin.prefixed("§f야생으로 이동했습니다!"));
            }, () -> player.sendMessage(plugin.prefixed("§c안전한 위치를 찾지 못했습니다. 잠시 후 다시 시도해주세요.")));
        }, 2L);
    }
}
