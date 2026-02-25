package com.clutch.movecommands.command;

import com.clutch.movecommands.ClutchMoveCommandsPlugin;
import com.clutch.movecommands.cast.CastType;
import com.clutch.movecommands.cast.TeleportCastService;
import com.clutch.movecommands.rtp.RTPService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
            player.sendTitle("§8CLUTCH", String.format("§c쿨타임입니다! 남은 시간: §f%02d:%02d", minutes, seconds), 0, 40, 10);
            return true;
        }

        int castSeconds = plugin.getConfig().getInt("cast.seconds", 3);
        return castService.startCast(player, CastType.WILD, castSeconds, () -> completeWild(player));
    }

    private void completeWild(Player player) {
        player.performCommand("mvtp wild");

        new BukkitRunnable() {
            private int elapsedTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                elapsedTicks++;
                if (player.getWorld().getName().equalsIgnoreCase("wild")) {
                    executeRtp(player);
                    cancel();
                    return;
                }

                if (elapsedTicks >= 40) {
                    player.sendTitle("§8CLUTCH", "§c월드 이동에 실패했습니다.", 0, 40, 10);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void executeRtp(Player player) {
        int minRadius = plugin.getConfig().getInt("rtp.minRadius", 500);
        int maxRadius = plugin.getConfig().getInt("rtp.maxRadius", 8000);
        int maxAttempts = plugin.getConfig().getInt("rtp.maxAttempts", 30);
        int cooldownSeconds = plugin.getConfig().getInt("rtp.cooldownSeconds", 180);

        rtpService.findAndTeleportAsync(
                player,
                minRadius,
                maxRadius,
                maxAttempts,
                () -> {
                    cooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
                    player.sendTitle("§8CLUTCH", "§f야생으로 이동했습니다!", 0, 40, 10);
                },
                () -> player.sendTitle("§8CLUTCH", "§c안전한 위치를 찾지 못했습니다.", 0, 40, 10)
        );
    }
}
