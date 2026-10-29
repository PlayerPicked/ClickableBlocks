package com.playerpicked.clickableblocks.database.impl;

import com.playerpicked.clickableblocks.database.AbstractDatabase;
import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.List;

public final class MySQL extends AbstractDatabase {

    @Override
    protected HikariDataSource createDataSource() {
        HikariConfig hikari = new HikariConfig();

        var mysql = ConfigKeys.DATABASE_MYSQL.getSection();

        hikari.setJdbcUrl("jdbc:mysql://" +
                mysql.getString("host") + ":" +
                mysql.getInt("port") + "/" +
                mysql.getString("database") +
                "?useSSL=" + mysql.getBoolean("useSSL", false));

        hikari.setUsername(mysql.getString("username"));
        hikari.setPassword(mysql.getString("password"));

        applyPoolSettings(hikari);

        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        hikari.addDataSourceProperty("rewriteBatchedStatements", "true");

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
                    data MEDIUMTEXT NOT NULL,
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
            INSERT INTO clickable_blocks (id, world, x, y, z, type, data, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z),
                type=VALUES(type), data=VALUES(data), updated_at=VALUES(updated_at)
        """;
    }

    @Override
    protected String getUsesUpsertSql() {
        return """
            INSERT INTO clickable_uses (block_id, player_uuid, uses)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE uses=VALUES(uses)
        """;
    }
}