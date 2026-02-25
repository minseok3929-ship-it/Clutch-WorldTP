package com.clutch.movecommands;

import com.clutch.movecommands.cast.TeleportCastService;
import com.clutch.movecommands.command.SpawnCommand;
import com.clutch.movecommands.command.WildCommand;
import com.clutch.movecommands.listener.MoveCancelListener;
import com.clutch.movecommands.rtp.RTPService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClutchMoveCommandsPlugin extends JavaPlugin {

    private TeleportCastService castService;
    private RTPService rtpService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.castService = new TeleportCastService(this);
        this.rtpService = new RTPService(this);

        WildCommand wildCommand = new WildCommand(this, castService, rtpService);
        SpawnCommand spawnCommand = new SpawnCommand(this, castService);

        registerCommand("야생", wildCommand);
        registerCommand("wild", wildCommand);
        registerCommand("스폰", spawnCommand);
        registerCommand("spawn", spawnCommand);

        getServer().getPluginManager().registerEvents(new MoveCancelListener(castService), this);
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("Command not found in plugin.yml: " + name);
        }
    }

    public String prefixed(String message) {
        return getConfig().getString("prefix", "§8[CLUTCH] ") + message;
    }
}
