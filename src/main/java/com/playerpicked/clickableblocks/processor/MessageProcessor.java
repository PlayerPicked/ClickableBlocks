package com.playerpicked.clickableblocks.processor;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
@SuppressWarnings("deprecation")
public class MessageProcessor {
    private static final char COLOR_CHAR = '§';
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");
    private static final String FORMAT_CODES = "klmnoKLMNO";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(COLOR_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public @NotNull String process(@Nullable String message) {
        if (message == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(gradients(message));
        StringBuilder builder = new StringBuilder(message.length() + 4 * 8);

        while (matcher.find()) {
            String group = matcher.group(1);

            matcher.appendReplacement(builder, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }

        String hexProcessed = matcher.appendTail(builder).toString();
        return ChatColor.translateAlternateColorCodes('&', hexProcessed);
    }

    @NotNull
    public Component toComponent(@NotNull String processed) {
        return LEGACY.deserialize(processed);
    }

    @NotNull
    private String gradients(@NotNull String message) {
        if (!message.contains("<gradient:")) return message;

        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder(message.length() * 4);

        while (matcher.find()) {
            String gradient = gradient(Integer.parseInt(matcher.group(1), 16), Integer.parseInt(matcher.group(2), 16), matcher.group(3));
            matcher.appendReplacement(builder, Matcher.quoteReplacement(gradient));
        }

        return matcher.appendTail(builder).toString();
    }

    @NotNull
    private String gradient(int from, int to, @NotNull String text) {
        StringBuilder formats = new StringBuilder();
        StringBuilder visible = new StringBuilder();
        StringBuilder output = new StringBuilder();

        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '&' && index + 1 < text.length()) {
                index++;
                continue;
            }
            visible.append(current);
        }

        int total = Math.max(1, visible.length() - 1);
        int position = 0;

        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);

            if (current == '&' && index + 1 < text.length()) {
                char code = text.charAt(++index);
                if (FORMAT_CODES.indexOf(code) >= 0) formats.append('&').append(code);
                else if (code == 'r' || code == 'R') formats.setLength(0);
                continue;
            }

            float ratio = (float) position++ / total;
            output.append(String.format("&#%06X", interpolate(from, to, ratio))).append(formats).append(current);
        }

        return output.toString();
    }

    private int interpolate(int from, int to, float ratio) {
        int red = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * ratio);
        int green = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * ratio);
        int blue = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * ratio);
        return red << 16 | green << 8 | blue;
    }
}