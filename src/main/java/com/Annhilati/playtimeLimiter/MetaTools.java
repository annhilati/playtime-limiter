package com.Annhilati.playtimeLimiter;

import com.Annhilati.playtimeLimiter.Commands.Limiter;
import org.bukkit.plugin.java.JavaPlugin;

public final class MetaTools extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("test").setExecutor(new Limiter());

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}