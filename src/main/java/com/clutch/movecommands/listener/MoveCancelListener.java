package com.clutch.movecommands.listener;

import com.clutch.movecommands.cast.CastSession;
import com.clutch.movecommands.cast.TeleportCastService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MoveCancelListener implements Listener {

    private final TeleportCastService castService;

    public MoveCancelListener(TeleportCastService castService) {
        this.castService = castService;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        CastSession session = castService.getSession(player.getUniqueId());
        if (session == null) {
            return;
        }

        if (session.changedBlock(event.getTo())) {
            castService.cancelCast(player, "§c이동이 취소되었습니다.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        castService.clearCastOnQuit(event.getPlayer());
    }
}
