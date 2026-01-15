package com.jodek.simplesleep.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SleepConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final transient Path configPath;

    // Config values
    public String mode = "percentage";

    public double percentageRequired = 0.5;

    public int amountRequired = 3;

    public boolean showSleepingPlayers = true;

    public String _comment = "Mode can be 'percentage' or 'amount'. If 'percentage', uses percentageRequired (0.0-1.0). If 'amount', uses amountRequired.";

    /**
     * Creates a new config with the specified file path.
     *
     * @param configPath Path to the config.json file
     */
    public SleepConfig(Path configPath) {
        this.configPath = configPath;
    }

    public void load() {
        File file = configPath.toFile();

        // Create config with defaults if it doesn't exist
        if (!file.exists()) {
            System.out.println("[SimpleSleep] Config not found, creating default config...");
            save();
            return;
        }

        try {
            // Read and parse JSON
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            SleepConfig loaded = GSON.fromJson(json, SleepConfig.class);

            // Copy values from loaded config
            if (loaded != null) {
                // Validate and set mode
                if ("percentage".equalsIgnoreCase(loaded.mode) || "amount".equalsIgnoreCase(loaded.mode)) {
                    this.mode = loaded.mode.toLowerCase();
                } else {
                    System.err.println("[SimpleSleep] WARNING: Invalid mode '" + loaded.mode + "'. Using 'percentage'. Valid modes: 'percentage' or 'amount'");
                    this.mode = "percentage";
                }

                this.percentageRequired = clamp(loaded.percentageRequired, 0.0, 1.0);
                this.amountRequired = Math.max(1, loaded.amountRequired); // At least 1 player
                this.showSleepingPlayers = loaded.showSleepingPlayers;

                System.out.println("[SimpleSleep] Config loaded successfully!");
                if (isUsingAmount()) {
                    System.out.println("[SimpleSleep] - Mode: Amount (" + amountRequired + " players)");
                } else {
                    System.out.println("[SimpleSleep] - Mode: Percentage (" + (percentageRequired * 100) + "%)");
                }
                System.out.println("[SimpleSleep] - Show sleeping players: " + showSleepingPlayers);
            } else {
                System.err.println("[SimpleSleep] Config file is empty, using defaults!");
                save();
            }

        } catch (IOException e) {
            System.err.println("[SimpleSleep] Failed to read config file: " + e.getMessage());
            System.err.println("[SimpleSleep] Using default values!");
            save();
        } catch (Exception e) {
            System.err.println("[SimpleSleep] Failed to parse config (invalid JSON): " + e.getMessage());
            System.err.println("[SimpleSleep] Using default values!");
            save();
        }
    }

    /**
     * Saves the current config
     */
    public void save() {
        try {
            // Create parent directories if they don't exist
            Path parentDir = configPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Convert to JSON and write to file
            String json = GSON.toJson(this);
            Files.writeString(configPath, json, StandardCharsets.UTF_8);

            System.out.println("[SimpleSleep] Config saved to: " + configPath);

        } catch (IOException e) {
            System.err.println("[SimpleSleep] Failed to save config: " + e.getMessage());
        }
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            System.err.println("[SimpleSleep] WARNING: percentageRequired (" + value + ") is below minimum (" + min + "), using " + min);
            return min;
        }
        if (value > max) {
            System.err.println("[SimpleSleep] WARNING: percentageRequired (" + value + ") is above maximum (" + max + "), using " + max);
            return max;
        }
        return value;
    }

    public double getPercentageRequired() {
        return percentageRequired;
    }

    public int getAmountRequired() {
        return amountRequired;
    }

    public boolean isShowSleepingPlayers() {
        return showSleepingPlayers;
    }

    public boolean isUsingAmount() {
        return "amount".equalsIgnoreCase(mode);
    }

    /**
     * Calculates how many players need to sleep based on the config mode
     *
     * @param totalPlayers Total number of online players
     * @return Number of players required to sleep
     */
    public int getRequiredSleepingPlayers(int totalPlayers) {
        if (isUsingAmount()) {
            // Amount mode: Use the exact number configured (but not more than total players)
            return Math.min(amountRequired, totalPlayers);
        } else {
            // Percentage mode: Calculate based on percentage
            return (int) Math.ceil(totalPlayers * percentageRequired);
        }
    }
}
