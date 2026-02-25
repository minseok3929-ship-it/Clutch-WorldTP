package com.clutch.movecommands.cast;

import com.clutch.movecommands.ClutchMoveCommandsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportCastService {

    private final ClutchMoveCommandsPlugin plugin;
    private final Map<UUID, CastSession> sessions = new ConcurrentHashMap<>();

    public TeleportCastService(ClutchMoveCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startCast(Player player, CastType type, int seconds, Runnable onComplete) {
        if (sessions.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.prefixed("§c이미 이동 준비 중입니다."));
            return false;
        }

        player.sendTitle("§8CLUTCH", "§f3초 후 이동합니다. §7(이동 시 취소)", 0, 20, 5);

        Location startLocation = player.getLocation().getBlock().getLocation();

        for (int i = 0; i < seconds; i++) {
            int count = seconds - i;
            long delayTicks = i * 20L;

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!sessions.containsKey(player.getUniqueId())) {
                    return;
                }

                player.sendTitle("§8CLUTCH", "§e" + count, 0, 20, 0);
            }, delayTicks);
        }

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sessions.remove(player.getUniqueId());
            onComplete.run();
        }, seconds * 20L);

        CastSession session = new CastSession(type, startLocation, task, System.currentTimeMillis());
        sessions.put(player.getUniqueId(), session);
        return true;
    }

    public boolean isCasting(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public CastSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public void cancelCast(Player player, String reasonMessage) {
        CastSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        session.cancelTask();

        if (reasonMessage != null && !reasonMessage.isBlank()) {
            player.sendTitle("§8CLUTCH", reasonMessage, 0, 40, 10);
            player.sendMessage(plugin.prefixed(reasonMessage));
        }
    }

    public void clearCastOnQuit(Player player) {
        CastSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.cancelTask();
        }
    }
}
