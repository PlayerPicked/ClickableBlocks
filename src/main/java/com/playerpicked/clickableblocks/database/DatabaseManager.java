package com.playerpicked.clickableblocks.database;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.database.impl.H2;
import com.playerpicked.clickableblocks.database.impl.MySQL;
import com.playerpicked.clickableblocks.utils.LoggerUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class DatabaseManager {
    private static Database database;

    private DatabaseManager() {
    }

    public static void initialize() {
        ClickableBlocks plugin = ClickableBlocks.getInstance();
        String databaseType = plugin.getConfiguration().getString("database.type", "h2").toLowerCase();

        try {
            switch (databaseType) {
                case "mysql" -> database = new MySQL();
                case "h2" -> database = new H2();
                default -> {
                    LoggerUtils.error("Unsupported database type: {}", databaseType);
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    return;
                }
            }

            database.initialize().join();
            LoggerUtils.info("Database initialized successfully");
        } catch (Exception exception) {
            LoggerUtils.error("Failed to initialize database: {}", exception.getMessage());
            shutdown();
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public static @NotNull Database getDatabase() {
        return database;
    }

    public static void shutdown() {
        if (database == null) return;

        database.shutdown();
        database = null;
    }

    @NotNull
    @Contract(pure = true)
    public static String getDatabaseType() {
        return database instanceof MySQL ? "MySQL" : "H2";
    }
}