package com.playerpicked.clickableblocks.identifiers.keys;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.config.Config;
import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.processor.MessageProcessor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

@Getter
public enum MessageKeys {
    RELOAD("messages.reload"),
    NO_PERMISSION("messages.no-permission"),
    PLAYER_REQUIRED("messages.player-required"),
    PLAYER_NOT_FOUND("messages.player-not-found"),
    MISSING_ARGUMENT("messages.missing-argument"),
    HELP("messages.help"),

    BLOCK_NOT_FOUND("messages.block.not-found"),
    BLOCK_INVALID_ID("messages.block.invalid-id"),
    BLOCK_ALREADY_EXISTS("messages.block.already-exists"),
    BLOCK_POSITION_TAKEN("messages.block.position-taken"),
    BLOCK_NO_TARGET("messages.block.no-target"),
    BLOCK_CREATED("messages.block.created"),
    BLOCK_CLONED("messages.block.cloned"),
    BLOCK_DELETED("messages.block.deleted"),
    BLOCK_MOVED("messages.block.moved"),
    BLOCK_TELEPORTED("messages.block.teleported"),
    BLOCK_WORLD_NOT_LOADED("messages.block.world-not-loaded"),
    BLOCK_ENABLED("messages.block.enabled"),
    BLOCK_DISABLED("messages.block.disabled"),
    BLOCK_TYPE_CHANGED("messages.block.type-changed"),
    BLOCK_ITEM_SET("messages.block.item-set"),
    BLOCK_NOT_A_BLOCK("messages.block.not-a-block"),
    BLOCK_MATERIAL_SET("messages.block.material-set"),
    BLOCK_TESTED("messages.block.tested"),
    BLOCK_NO_ACTIONS("messages.block.no-actions"),
    BLOCK_USES_RESET("messages.block.uses-reset"),
    BLOCK_FORCE_RUN("messages.block.force-run"),
    BLOCK_INFO("messages.block.info"),
    WAND_RECEIVED("messages.block.wand-received"),
    WAND_HINT("messages.block.wand-hint"),

    CLICK_DISABLED("messages.click.disabled"),
    CLICK_NO_PERMISSION("messages.click.no-permission"),
    CLICK_COOLDOWN("messages.click.cooldown"),
    CLICK_MAX_USES("messages.click.max-uses"),
    CLICK_NOT_ENOUGH_MONEY("messages.click.not-enough-money"),
    CLICK_PAID("messages.click.paid"),
    CLICK_ECONOMY_UNAVAILABLE("messages.click.economy-unavailable"),

    EDITOR_UPDATED("messages.editor.updated"),
    EDITOR_ACTION_ADDED("messages.editor.action-added"),
    EDITOR_ACTION_UPDATED("messages.editor.action-updated"),
    EDITOR_ACTION_REMOVED("messages.editor.action-removed"),
    EDITOR_ACTION_LIMIT("messages.editor.action-limit"),
    EDITOR_ACTION_HELP("messages.editor.action-help"),
    EDITOR_INVALID_ACTION("messages.editor.invalid-action"),
    EDITOR_INVALID_NUMBER("messages.editor.invalid-number"),
    EDITOR_INVALID_COLOR("messages.editor.invalid-color"),
    EDITOR_INVALID_PERMISSION("messages.editor.invalid-permission"),
    EDITOR_INPUT_CANCELLED("messages.editor.input-cancelled"),
    EDITOR_BLOCK_REMOVED("messages.editor.block-removed"),

    PLACEHOLDER_NONE("placeholders.none"),
    PLACEHOLDER_YES("placeholders.yes"),
    PLACEHOLDER_NO("placeholders.no"),
    PLACEHOLDER_ENABLED("placeholders.enabled"),
    PLACEHOLDER_DISABLED("placeholders.disabled"),
    PLACEHOLDER_AUTO("placeholders.auto"),
    PLACEHOLDER_DEFAULT("placeholders.default"),
    PLACEHOLDER_UNLIMITED("placeholders.unlimited"),
    PLACEHOLDER_FREE("placeholders.free"),
    PLACEHOLDER_READY("placeholders.ready"),

    TIME_DAY("messages.time.short.day"),
    TIME_HOUR("messages.time.short.hour"),
    TIME_MINUTE("messages.time.short.minute"),
    TIME_SECOND("messages.time.short.second");

    private final String path;
    private static final Config config = ClickableBlocks.getInstance().getLanguage();

    MessageKeys(@NotNull String path) {
        this.path = path;
    }

    @NotNull
    public static String getTriggerName(@NotNull ClickTrigger trigger) {
        return MessageProcessor.process(config.getString("triggers." + trigger.name(), trigger.name()));
    }

    @NotNull
    public static String getTypeName(@NotNull ClickableType type) {
        return MessageProcessor.process(config.getString("types." + type.name(), type.name()));
    }

    @NotNull
    public static String getSettingName(@NotNull String setting) {
        return MessageProcessor.process(config.getString("settings." + setting, setting));
    }

    @NotNull
    public static String state(boolean enabled) {
        return enabled ? PLACEHOLDER_ENABLED.getMessage() : PLACEHOLDER_DISABLED.getMessage();
    }

    public boolean isEmpty() {
        String raw = config.getString(path);
        return raw == null || raw.isEmpty();
    }

    public @NotNull String getMessage() {
        return MessageProcessor.process(config.getString(path))
                .replace("%prefix%", MessageProcessor.process(config.getString("prefix", "")));
    }

    public @NotNull String getMessage(@NotNull String... replacements) {
        return replace(getMessage(), replacements);
    }

    @NotNull
    public @Unmodifiable List<String> getMessages() {
        String prefix = MessageProcessor.process(config.getString("prefix", ""));

        return config.getStringList(path, List.of())
                .stream()
                .map(MessageProcessor::process)
                .map(line -> line.replace("%prefix%", prefix))
                .toList();
    }

    @NotNull
    public @Unmodifiable List<String> getMessages(@NotNull String... replacements) {
        return getMessages().stream()
                .map(line -> replace(line, replacements))
                .toList();
    }

    public void send(@NotNull CommandSender sender, @NotNull String... replacements) {
        sendRaw(sender, getMessage(replacements));
    }

    public void sendList(@NotNull CommandSender sender, @NotNull String... replacements) {
        getMessages(replacements).forEach(line -> sendRaw(sender, line));
    }

    @NotNull
    public static String prefix() {
        return MessageProcessor.process(config.getString("prefix", ""));
    }

    public static void sendRaw(@NotNull CommandSender sender, @NotNull String message) {
        if (message.isEmpty()) return;
        sender.sendMessage(component(message));
    }

    @NotNull
    @SuppressWarnings("deprecation")
    private static Component component(@NotNull String message) {
        String prefix = prefix();
        if (prefix.isEmpty() || !message.startsWith(prefix)) return MessageProcessor.toComponent(message);

        String rest = ChatColor.getLastColors(prefix) + message.substring(prefix.length());
        return prefixComponent(prefix).append(MessageProcessor.toComponent(rest));
    }

    @NotNull
    private static Component prefixComponent(@NotNull String prefix) {
        Component component = MessageProcessor.toComponent(prefix);
        if (!config.getBoolean("prefix-hover.enabled", true)) return component;

        List<String> lines = config.getStringList("prefix-hover.lines", List.of());
        if (!lines.isEmpty()) {
            Component hover = Component.join(JoinConfiguration.newlines(), lines.stream()
                    .map(line -> MessageProcessor.toComponent(MessageProcessor.process(line
                            .replace("{version}", ClickableBlocks.getInstance().getPluginMeta().getVersion()))))
                    .toList());
            component = component.hoverEvent(HoverEvent.showText(hover));
        }

        String command = config.getString("prefix-hover.click-command", "");
        if (command != null && !command.isBlank()) component = component.clickEvent(ClickEvent.runCommand(command.startsWith("/") ? command : "/" + command));

        return component;
    }

    @NotNull
    public static String replace(@NotNull String message, @NotNull String... replacements) {
        for (int index = 0; index + 1 < replacements.length; index += 2) message = message.replace(replacements[index], replacements[index + 1]);
        return message;
    }
}