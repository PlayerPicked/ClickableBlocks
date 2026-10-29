package com.playerpicked.clickableblocks.manager;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.config.Config;
import com.playerpicked.clickableblocks.identifiers.keys.DialogKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.processor.MessageProcessor;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class DialogInputManager {
    private static final String INPUT_KEY = "input";

    public void request(@NotNull Player player, @NotNull DialogKeys key, @NotNull String initial, @NotNull Consumer<String> onSubmit, @NotNull Runnable onCancel, @NotNull String... replacements) {
        Config config = ClickableBlocks.getInstance().getDialogs();
        String path = key.getPath();
        AtomicBoolean handled = new AtomicBoolean(false);

        ClickCallback.Options options = ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofSeconds(Math.max(1, config.getLong("settings.timeout", 300L))))
                .build();

        DialogActionCallback confirmAction = (view, _) -> {
            if (!handled.compareAndSet(false, true)) return;

            String text = view.getText(INPUT_KEY);
            String value = text == null ? "" : text.strip();

            ClickableBlocks.getScheduler().runTask(player, () -> {
                if (player.isOnline()) onSubmit.accept(value);
            });
        };

        DialogActionCallback cancelAction = (_, _) -> {
            if (!handled.compareAndSet(false, true)) return;

            ClickableBlocks.getScheduler().runTask(player, () -> {
                if (!player.isOnline()) return;
                if (config.getBoolean("settings.cancel-message", true)) MessageKeys.EDITOR_INPUT_CANCELLED.send(player);
                onCancel.run();
            });
        };

        int maxLength = Math.clamp(config.getInt(path + ".input.max-length", 256), 1, 4096);

        DialogBase base = DialogBase.builder(text(config.getString(path + ".title", ""), replacements))
                .canCloseWithEscape(config.getBoolean(path + ".can-close-with-escape", true))
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(body(config, path, replacements))
                .inputs(List.of(input(config, path + ".input", initial, maxLength, replacements)))
                .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(
                        button(config, path + ".confirm", DialogAction.customClick(confirmAction, options), replacements),
                        button(config, path + ".cancel", DialogAction.customClick(cancelAction, options), replacements))));

        player.closeInventory();
        player.showDialog(dialog);
    }

    @NotNull
    private @Unmodifiable List<DialogBody> body(@NotNull Config config, @NotNull String path, @NotNull String[] replacements) {
        Object raw = config.get(path + ".body");

        List<String> lines;
        if (raw instanceof List<?> list) lines = list.stream().map(String::valueOf).toList();
        else if (raw instanceof String string && !string.isEmpty()) lines = List.of(string);
        else return List.of();

        if (lines.isEmpty()) return List.of();

        Component contents = text(String.join("\n", lines), replacements);
        return List.of(DialogBody.plainMessage(contents, width(config.getInt(path + ".body-width", 300))));
    }

    @NotNull
    private TextDialogInput input(@NotNull Config config, @NotNull String path, @NotNull String initial, int maxLength, @NotNull String[] replacements) {
        String value = initial.length() > maxLength ? initial.substring(0, maxLength) : initial;

        TextDialogInput.Builder builder = DialogInput.text(INPUT_KEY, text(config.getString(path + ".label", ""), replacements))
                .labelVisible(config.getBoolean(path + ".label-visible", true))
                .width(width(config.getInt(path + ".width", 300)))
                .maxLength(maxLength)
                .initial(value);

        if (config.getBoolean(path + ".multiline.enabled", false)) {
            builder.multiline(TextDialogInput.MultilineOptions.create(
                    Math.max(1, config.getInt(path + ".multiline.max-lines", 10)),
                    Math.clamp(config.getInt(path + ".multiline.height", 128), 1, 512)));
        }

        return builder.build();
    }

    @NotNull
    private ActionButton button(@NotNull Config config, @NotNull String path, @NotNull DialogAction action, @NotNull String[] replacements) {
        return ActionButton.builder(text(config.getString(path + ".label", ""), replacements))
                .tooltip(optional(config.getString(path + ".tooltip", ""), replacements))
                .width(width(config.getInt(path + ".width", 100)))
                .action(action)
                .build();
    }

    private static int width(int width) {
        return Math.clamp(width, 1, 1024);
    }

    @NotNull
    private static Component text(@Nullable String raw, @NotNull String[] replacements) {
        return MessageProcessor.toComponent(MessageProcessor.process(MessageKeys.replace(raw == null ? "" : raw, replacements)));
    }

    @Nullable
    private static Component optional(@Nullable String raw, @NotNull String[] replacements) {
        return raw == null || raw.isEmpty() ? null : text(raw, replacements);
    }
}