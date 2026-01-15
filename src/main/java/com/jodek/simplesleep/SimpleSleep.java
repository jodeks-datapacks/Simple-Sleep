package com.jodek.simplesleep;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.jodek.simplesleep.config.SleepConfig;
import com.jodek.simplesleep.events.SleepEventHandler;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Main plugin class for SimpleSleep
 * Allows configuration of sleep mechanics on multiplayer servers
 *
 * @author Jodek
 * @version 1.0.0
 */
public class SimpleSleep extends JavaPlugin {

    private static SimpleSleep instance;
    private SleepConfig config;
    private SleepEventHandler sleepEventHandler;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> sleepCheckTask;

    /**
     * Constructor - Called when plugin is loaded
     */
    public SimpleSleep(@NotNull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    /**
     * Called when plugin starts
     */
    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("SimpleSleep plugin enabled!");

        // Load config (config/SimpleSleep.json)
        Path configPath = Paths.get("config", "SimpleSleep.json");
        config = new SleepConfig(configPath);
        config.load();

        sleepEventHandler = new SleepEventHandler(this, config);

        // Scheduler checks every second if enough players are sleeping
        scheduler = Executors.newSingleThreadScheduledExecutor();
        sleepCheckTask = scheduler.scheduleAtFixedRate(
            this::checkSleepingPlayers,
            1L, // Initial delay
            1L, // Period
            TimeUnit.SECONDS
        );

        String modeDescription = config.isUsingAmount()
            ? config.getAmountRequired() + " players"
            : (config.getPercentageRequired() * 100) + "%";
        getLogger().at(Level.INFO).log("SimpleSleep ready! Sleep mode: " + modeDescription);
    }

    // Checks every second
    private void checkSleepingPlayers() {
        try {
            sleepEventHandler.checkAllWorlds();
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Error checking sleeping players: " + e.getMessage());
        }
    }

    /**
     * Called when plugin shuts down.
     */
    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("SimpleSleep plugin disabled!");

        if (sleepCheckTask != null) {
            sleepCheckTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }

        // Save config if needed
        if (config != null) {
            config.save();
        }
    }

    /**
     * Get plugin instance.
     */
    public static SimpleSleep getInstance() {
        return instance;
    }

    /**
     * Get the plugin configuration.
     */
    public SleepConfig getConfig() {
        return config;
    }

    /**
     * Get the sleep event handler.
     */
    public SleepEventHandler getSleepEventHandler() {
        return sleepEventHandler;
    }
}

