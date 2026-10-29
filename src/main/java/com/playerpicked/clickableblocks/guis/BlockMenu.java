package com.playerpicked.clickableblocks.guis;

import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.identifiers.keys.DialogKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class BlockMenu extends Menu implements BlockBoundMenu {
    protected final ClickableBlock block;

    protected BlockMenu(@NotNull MenuController menuController, @NotNull ClickableBlock block) {
        super(menuController);
        this.block = block;
    }

    @NotNull
    protected abstract Menu reopen();

    @Override
    public @NotNull ClickableBlock getBlock() {
        return block;
    }

    @Override
    public String getMenuName() {
        return getKey().getTitle().replace("{id}", block.getId());
    }

    @Override
    public int getSlots() {
        return getKey().getSize();
    }

    @Override
    public int getMenuTick() {
        return getKey().getTick();
    }

    protected void edited(@NotNull String setting, @NotNull String value) {
        plugin.getEditorService().changed(block);
        MessageKeys.EDITOR_UPDATED.send(player(), "{setting}", MessageKeys.getSettingName(setting), "{value}", value);
        sound("action");
        updateMenuItems();
    }

    protected void editedQuietly() {
        plugin.getEditorService().changed(block);
        sound("action");
        updateMenuItems();
    }

    protected void error() {
        sound("error");
    }

    protected void ask(@NotNull DialogKeys key, @NotNull String initial, @NotNull Consumer<String> onSubmit, @NotNull String... replacements) {
        String[] withId = new String[replacements.length + 2];
        withId[0] = "{id}";
        withId[1] = block.getId();
        System.arraycopy(replacements, 0, withId, 2, replacements.length);

        plugin.getDialogInputManager().request(player(), key, initial, value -> {
            if (isStale()) {
                MessageKeys.EDITOR_BLOCK_REMOVED.send(player(), "{id}", block.getId());
                return;
            }

            onSubmit.accept(value);
            reopen().open();
        }, () -> {
            if (!isStale()) reopen().open();
        }, withId);
    }
}