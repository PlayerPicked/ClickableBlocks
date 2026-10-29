package com.playerpicked.clickableblocks.config;

import com.playerpicked.clickableblocks.utils.LoggerUtils;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.File;
import java.io.InputStream;
import java.util.List;

@SuppressWarnings("unchecked")
public class Config {
    private YamlDocument configuration;

    public Config(File file, InputStream defaults, GeneralSettings generalSettings, LoaderSettings loaderSettings, DumperSettings dumperSettings, UpdaterSettings updaterSettings) {
        try {
            configuration = YamlDocument.create(file, defaults, generalSettings, loaderSettings, dumperSettings, updaterSettings);
        } catch (Exception exception) {
            LoggerUtils.error("Failed to load " + file.getName(), exception);
        }
    }

    public <T> T get(String route) {
        return (T) configuration.get(route);
    }

    public String getString(String key) {
        return configuration.getString(key);
    }

    public String getString(String key, String def) {
        return configuration.getString(key, def);
    }

    public boolean getBoolean(String key, boolean def) {
        return configuration.getBoolean(key, def);
    }

    public int getInt(String key, int def) {
        return configuration.getInt(key, def);
    }

    public long getLong(String key, long def) {
        return configuration.getLong(key, def);
    }

    public double getDouble(String key, double def) {
        return configuration.getDouble(key, def);
    }

    public List<Integer> getIntList(String key, List<Integer> def) {
        return configuration.getIntList(key, def);
    }

    public List<String> getStringList(String key, List<String> def) {
        return getList(key, def);
    }

    public <T> List<T> getList(String key, List<T> def) {
        return (List<T>) configuration.getList(key, def);
    }

    public Section getSection(String key) {
        return configuration.getSection(key);
    }

    public void reload() {
        try {
            configuration.reload();
        } catch (Exception ignored) {}
    }
}