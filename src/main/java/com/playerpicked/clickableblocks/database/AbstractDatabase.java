package com.playerpicked.clickableblocks.database;

import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.playerpicked.clickableblocks.model.StoredBlock;
import com.playerpicked.clickableblocks.utils.LoggerUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDatabase implements Database {

    protected final HikariDataSource dataSource;

    protected final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ClickableBlocks-Writer");
        thread.setDaemon(true);
        return thread;
    });

    protected AbstractDatabase() {
        this.dataSource = createDataSource();
    }

    protected abstract HikariDataSource createDataSource();

    protected abstract List<String> getTableDefinitions();

    protected abstract String getBlockUpsertSql();

    protected abstract String getUsesUpsertSql();

    protected void applyPoolSettings(@NotNull HikariConfig hikari) {
        var pool = ConfigKeys.DATABASE_POOL.getSection();

        hikari.setPoolName("ClickableBlocks-Pool");
        hikari.setMaximumPoolSize(pool.getInt("maximumPoolSize", 10));
        hikari.setMinimumIdle(pool.getInt("minimumIdle", 2));
        hikari.setConnectionTimeout(pool.getLong("connectionTimeout", 30000L));
        hikari.setIdleTimeout(pool.getLong("idleTimeout", 600000L));
        hikari.setMaxLifetime(pool.getLong("maxLifetime", 1800000L));

        if (pool.contains("setLeakDetectionThreshold")) hikari.setLeakDetectionThreshold(pool.getLong("setLeakDetectionThreshold"));
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {

                for (String ddl : getTableDefinitions()) statement.execute(ddl);

            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }
        }, writer);
    }

    @Override
    public CompletableFuture<List<StoredBlock>> loadBlocks() {
        return CompletableFuture.supplyAsync(() -> {
            List<StoredBlock> list = new ArrayList<>();

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("SELECT * FROM clickable_blocks");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(new StoredBlock(
                            rs.getString("id"),
                            rs.getString("world"),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            rs.getInt("z"),
                            rs.getString("type"),
                            rs.getString("data"),
                            rs.getLong("created_at")));
                }

            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }

            return list;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveBlock(@NotNull StoredBlock block) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(getBlockUpsertSql())) {

                ps.setString(1, block.id());
                ps.setString(2, block.world());
                ps.setInt(3, block.x());
                ps.setInt(4, block.y());
                ps.setInt(5, block.z());
                ps.setString(6, block.type());
                ps.setString(7, block.data());
                ps.setLong(8, block.createdAt());
                ps.setLong(9, System.currentTimeMillis());

                ps.executeUpdate();

            } catch (SQLException exception) {
                LoggerUtils.error("Saving clickable block '{}' failed: {}", block.id(), exception.getMessage());
            }
        }, writer);
    }

    @Override
    public CompletableFuture<Void> deleteBlock(@NotNull String id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM clickable_blocks WHERE id = ?")) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM clickable_uses WHERE block_id = ?")) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                }

            } catch (SQLException exception) {
                LoggerUtils.error("Deleting clickable block '{}' failed: {}", id, exception.getMessage());
            }
        }, writer);
    }

    @Override
    public CompletableFuture<Map<String, Map<UUID, Integer>>> loadUses() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Map<UUID, Integer>> result = new HashMap<>();

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("SELECT block_id, player_uuid, uses FROM clickable_uses");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(rs.getString("player_uuid"));
                    } catch (IllegalArgumentException exception) {
                        continue;
                    }

                    result.computeIfAbsent(rs.getString("block_id"), _ -> new HashMap<>()).put(uuid, rs.getInt("uses"));
                }

            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }

            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveUses(@NotNull String blockId, @NotNull UUID player, int uses) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(getUsesUpsertSql())) {

                ps.setString(1, blockId);
                ps.setString(2, player.toString());
                ps.setInt(3, uses);

                ps.executeUpdate();

            } catch (SQLException exception) {
                LoggerUtils.error("Saving uses of '{}' failed: {}", blockId, exception.getMessage());
            }
        }, writer);
    }

    @Override
    public CompletableFuture<Void> resetUses(@NotNull String blockId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("DELETE FROM clickable_uses WHERE block_id = ?")) {

                ps.setString(1, blockId);
                ps.executeUpdate();

            } catch (SQLException exception) {
                LoggerUtils.error("Resetting uses of '{}' failed: {}", blockId, exception.getMessage());
            }
        }, writer);
    }

    @Override
    public void shutdown() {
        awaitShutdown(writer, 15);
        awaitShutdown(executor, 3);

        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    private void awaitShutdown(@NotNull ExecutorService service, int seconds) {
        try {
            service.shutdown();
            if (!service.awaitTermination(seconds, TimeUnit.SECONDS)) {
                LoggerUtils.warn("Database tasks did not finish in {} seconds, forcing shutdown.", seconds);
                service.shutdownNow();
            }
        } catch (InterruptedException exception) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}