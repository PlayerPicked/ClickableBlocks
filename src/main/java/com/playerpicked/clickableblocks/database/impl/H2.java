package com.playerpicked.clickableblocks.database.impl;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.database.AbstractDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.List;

public final class H2 extends AbstractDatabase {

    @Override
    protected HikariDataSource createDataSource() {
        HikariConfig hikari = new HikariConfig();

        String dbPath = ClickableBlocks.getInstance().getDataFolder().getAbsolutePath() + "/data";
        hikari.setJdbcUrl("jdbc:h2:file:" + dbPath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        hikari.setDriverClassName("org.h2.Driver");

        applyPoolSettings(hikari);
        hikari.setAutoCommit(true);

        return new HikariDataSource(hikari);
    }

    @Override
    protected List<String> getTableDefinitions() {
        return List.of(
                """
                CREATE TABLE IF NOT EXISTS clickable_blocks (
                    id VARCHAR(64) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    data CLOB NOT NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    PRIMARY KEY (id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS clickable_uses (
                    block_id VARCHAR(64) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    uses INT NOT NULL,
                    PRIMARY KEY (block_id, player_uuid)
                )
                """
        );
    }

    @Override
    protected String getBlockUpsertSql() {
        return """
            MERGE INTO clickable_blocks (id, world, x, y, z, type, data, created_at, updated_at)
            KEY(id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    }

    @Override
    protected String getUsesUpsertSql() {
        return """
            MERGE INTO clickable_uses (block_id, player_uuid, uses)
            KEY(block_id, player_uuid)
            VALUES (?, ?, ?)
        """;
    }
}