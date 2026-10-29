package com.playerpicked.clickableblocks;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.playerpicked.clickableblocks.action.ActionExecutor;
import com.playerpicked.clickableblocks.config.Config;
import com.playerpicked.clickableblocks.database.DatabaseManager;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.hooks.HookManager;
import com.playerpicked.clickableblocks.manager.ClickableBlockManager;
import com.playerpicked.clickableblocks.manager.CooldownManager;
import com.playerpicked.clickableblocks.manager.DialogInputManager;
import com.playerpicked.clickableblocks.manager.UsageManager;
import com.playerpicked.clickableblocks.service.ClickService;
import com.playerpicked.clickableblocks.service.EditorService;
import com.playerpicked.clickableblocks.service.PersistenceService;
import com.playerpicked.clickableblocks.service.VisualService;
import com.playerpicked.clickableblocks.utils.LoggerUtils;
import com.playerpicked.clickableblocks.utils.RegisterUtils;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import revxrsal.zapper.ZapperJavaPlugin;

import java.io.File;

public final class ClickableBlocks extends ZapperJavaPlugin {

    @Getter private static ClickableBlocks instance;
    @Getter private static TaskScheduler scheduler;

    @Getter private Config language;
    @Getter private Config guis;
    @Getter private Config dialogs;
    @Getter private Config hooks;
    Config config;

    @Getter private HookManager hookManager;
    @Getter private ClickableBlockManager blockManager;
    @Getter private CooldownManager cooldownManager;
    @Getter private UsageManager usageManager;
    @Getter private DialogInputManager dialogInputManager;
    @Getter private PersistenceService persistenceService;
    @Getter private VisualService visualService;
    @Getter private ActionExecutor actionExecutor;
    @Getter private ClickService clickService;
    @Getter private EditorService editorService;

    @Override
    public void onLoad() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
    }

    @Override
    public void onEnable() {
        initializeComponents();

        DatabaseManager.initialize();
        if (!isEnabled()) return;

        hookManager = new HookManager();
        blockManager = new ClickableBlockManager();
        cooldownManager = new CooldownManager();
        usageManager = new UsageManager();
        dialogInputManager = new DialogInputManager();
        persistenceService = new PersistenceService(blockManager);
        visualService = new VisualService(blockManager);
        actionExecutor = new ActionExecutor(hookManager);
        clickService = new ClickService(cooldownManager, usageManager, actionExecutor, hookManager);
        editorService = new EditorService(blockManager, persistenceService, visualService, cooldownManager, usageManager);

        try {
            persistenceService.loadAll();
            usageManager.load();
        } catch (Exception exception) {
            LoggerUtils.error("Failed to load the clickable blocks, disabling to protect your data", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        RegisterUtils.registerCommands();
        RegisterUtils.registerListeners();

        scheduler.runTask(hookManager::load);
        persistenceService.start();
        cooldownManager.start();
        visualService.start();

        LoggerUtils.printStartup();
    }

    @Override
    public void onDisable() {
        getServer().getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) player.closeInventory();
        });

        if (visualService != null) visualService.shutdown();
        if (cooldownManager != null) cooldownManager.shutdown();
        if (persistenceService != null) persistenceService.shutdown();
        if (hookManager != null) hookManager.unload();

        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        DatabaseManager.shutdown();
    }

    public void reload() {
        config.reload();
        language.reload();
        guis.reload();
        dialogs.reload();
        hooks.reload();

        hookManager.reload();
        visualService.restart();
    }

    public Config getConfiguration() {
        return config;
    }

    private void initializeComponents() {
        final GeneralSettings generalSettings = GeneralSettings.builder()
                .setUseDefaults(false)
                .build();

        final LoaderSettings loaderSettings = LoaderSettings.builder()
                .setAutoUpdate(true)
                .build();

        final UpdaterSettings updaterSettings = UpdaterSettings.builder()
                .setKeepAll(true)
                .setVersioning(new BasicVersioning("version"))
                .build();

        config = loadConfig("config.yml", generalSettings, loaderSettings, updaterSettings);
        language = loadConfig("messages.yml", generalSettings, loaderSettings, updaterSettings);
        guis = loadConfig("guis.yml", generalSettings, loaderSettings, updaterSettings);
        dialogs = loadConfig("dialogs.yml", generalSettings, loaderSettings, updaterSettings);
        hooks = loadConfig("hooks.yml", generalSettings, loaderSettings, updaterSettings);
    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    private Config loadConfig(@NotNull String fileName, @NotNull GeneralSettings generalSettings, @NotNull LoaderSettings loaderSettings, @NotNull UpdaterSettings updaterSettings) {
        return new Config(
                new File(getDataFolder(), fileName),
                getResource(fileName),
                generalSettings,
                loaderSettings,
                DumperSettings.DEFAULT,
                updaterSettings
        );
    }
}