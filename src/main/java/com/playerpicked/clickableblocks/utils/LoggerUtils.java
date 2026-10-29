package com.playerpicked.clickableblocks.utils;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.database.DatabaseManager;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LoggerUtils {
    private final Logger logger = LogManager.getLogger("ClickableBlocks");

    public void info(@NotNull String msg, @NotNull Object... objs) {
        logger.info(msg, objs);
    }

    public void warn(@NotNull String msg, @NotNull Object... objs) {
        logger.warn(msg, objs);
    }

    public void error(@NotNull String msg, @NotNull Object... objs) {
        logger.error(msg, objs);
    }

    public void printStartup() {
        String main = "\u001B[38;2;255;189;114m";
        String reset = "\u001B[0m";
        ClickableBlocks plugin = ClickableBlocks.getInstance();
        String software = plugin.getServer().getName();
        String version = plugin.getServer().getVersion();

        info("");
        info("{}   ____ _ _      _         _     _      {}", main, reset);
        info("{}  / ___| (_) ___| | ____ _| |__ | | ___ {}", main, reset);
        info("{} | |   | | |/ __| |/ / _` | '_ \\| |/ _ \\{}", main, reset);
        info("{} | |___| | | (__|   < (_| | |_) | |  __/{}", main, reset);
        info("{}  \\____|_|_|\\___|_|\\_\\__,_|_.__/|_|\\___|{}", main, reset);
        info("");
        info("{}   The plugin successfully started.{}", main, reset);
        info("{}   ClickableBlocks {} {}{}", main, software, version, reset);
        info("{}   Discord @ dc.playerpicked.com{}", main, reset);
        info("");
        info("\u001B[33m   [Database] Selected database type: {}\u001B[0m", DatabaseManager.getDatabaseType());
        info("\u001B[33m   [Blocks] Loaded {} clickable blocks.\u001B[0m", plugin.getBlockManager().size());
    }
}