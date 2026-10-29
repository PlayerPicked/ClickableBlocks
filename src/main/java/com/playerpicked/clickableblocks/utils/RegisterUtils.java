package com.playerpicked.clickableblocks.utils;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.command.ClickableBlocksCommand;
import com.playerpicked.clickableblocks.handler.CommandExceptionHandler;
import com.playerpicked.clickableblocks.listener.ChunkListener;
import com.playerpicked.clickableblocks.listener.ClickListener;
import com.playerpicked.clickableblocks.listener.MenuListener;
import com.playerpicked.clickableblocks.listener.ProtectionListener;
import lombok.experimental.UtilityClass;
import org.bukkit.plugin.PluginManager;
import revxrsal.commands.bukkit.BukkitLamp;

@UtilityClass
public class RegisterUtils {

    public void registerCommands() {
        ClickableBlocks plugin = ClickableBlocks.getInstance();

        var lamp = BukkitLamp.builder(plugin)
                .exceptionHandler(new CommandExceptionHandler())
                .build();

        lamp.register(new ClickableBlocksCommand());
    }

    public void registerListeners() {
        ClickableBlocks plugin = ClickableBlocks.getInstance();
        PluginManager pluginManager = plugin.getServer().getPluginManager();

        pluginManager.registerEvents(new MenuListener(), plugin);
        pluginManager.registerEvents(new ClickListener(plugin.getBlockManager(), plugin.getClickService(), plugin.getEditorService(), plugin.getVisualService()), plugin);
        pluginManager.registerEvents(new ProtectionListener(plugin.getBlockManager()), plugin);
        pluginManager.registerEvents(new ChunkListener(plugin.getVisualService()), plugin);
    }
}