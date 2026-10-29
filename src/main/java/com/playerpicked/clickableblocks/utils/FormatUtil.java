package com.playerpicked.clickableblocks.utils;

import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public final class FormatUtil {
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(ms|d|h|m|s)?", Pattern.CASE_INSENSITIVE);

    @NotNull
    public String number(double value) {
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
        return decimal.scale() < 0 ? decimal.setScale(0, RoundingMode.UNNECESSARY).toPlainString() : decimal.toPlainString();
    }

    @NotNull
    public String duration(long millis) {
        if (millis <= 0) return MessageKeys.PLACEHOLDER_NONE.getMessage();

        long seconds = (millis + 999) / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder builder = new StringBuilder();
        append(builder, days, MessageKeys.TIME_DAY);
        append(builder, hours, MessageKeys.TIME_HOUR);
        append(builder, minutes, MessageKeys.TIME_MINUTE);
        append(builder, secs, MessageKeys.TIME_SECOND);

        return builder.isEmpty() ? "0" + MessageKeys.TIME_SECOND.getMessage() : builder.toString();
    }

    private void append(@NotNull StringBuilder builder, long value, @NotNull MessageKeys unit) {
        if (value <= 0) return;
        if (!builder.isEmpty()) builder.append(' ');
        builder.append(value).append(unit.getMessage());
    }

    public long parseDuration(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return -1;

        String input = raw.trim().toLowerCase(Locale.ROOT).replace(",", ".");
        Matcher matcher = DURATION_PART.matcher(input);

        long total = 0;
        int end = 0;
        boolean found = false;

        while (matcher.find()) {
            if (!input.substring(end, matcher.start()).isBlank()) return -1;

            double amount = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2) == null ? "s" : matcher.group(2);

            total += (long) switch (unit) {
                case "d" -> amount * 86_400_000d;
                case "h" -> amount * 3_600_000d;
                case "m" -> amount * 60_000d;
                case "ms" -> amount;
                default -> amount * 1000d;
            };

            end = matcher.end();
            found = true;
        }

        if (!found || !input.substring(end).isBlank()) return -1;
        return Math.min(total, 365L * 86_400_000L);
    }

    @Nullable
    public Double parseDouble(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;

        try {
            double value = Double.parseDouble(raw.trim().replace(",", "."));
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Nullable
    public Integer parseInt(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Nullable
    public double[] parseVector(@Nullable String raw, int size) {
        if (raw == null || raw.isBlank()) return null;

        String[] parts = raw.trim().split("[\\s;,]+");
        double[] result = new double[size];

        if (parts.length == 1) {
            Double value = parseDouble(parts[0]);
            if (value == null) return null;
            Arrays.fill(result, value);
            return result;
        }

        if (parts.length != size) return null;

        for (int index = 0; index < size; index++) {
            Double value = parseDouble(parts[index]);
            if (value == null) return null;
            result[index] = value;
        }

        return result;
    }

    @Nullable
    public Integer parseColor(@Nullable String raw) {
        if (raw == null) return null;

        String input = raw.trim().replace("&#", "").replace("#", "");
        if (!input.matches("[0-9a-fA-F]{6}")) return null;

        return Integer.parseInt(input, 16);
    }

    @NotNull
    public String color(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}