package dev.n1xend.dynamiceconomy.utils;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI utility helpers — item building, filling, formatting.
 *
 * @author n1xend
 * @version 1.2.1
 */
public final class GUIHelper {

    private GUIHelper() {}

    // ── Item building ─────────────────────────────────────────────────────────

    /** Builds an ItemStack with coloured display name and lore. */
    @NotNull
    public static ItemStack item(@NotNull Material mat, @NotNull String name,
                                  @Nullable List<String> lore) {
        ItemStack is   = new ItemStack(mat);
        ItemMeta  meta = is.getItemMeta();
        if (meta == null) return is;
        meta.setDisplayName(color(name));
        if (lore != null) {
            meta.setLore(lore.stream().map(GUIHelper::color).collect(Collectors.toList()));
        }
        is.setItemMeta(meta);
        return is;
    }

    /** Builds an ItemStack with coloured display name, no lore. */
    @NotNull
    public static ItemStack item(@NotNull Material mat, @NotNull String name) {
        return item(mat, name, null);
    }

    /** Creates a filler pane with a single space name (no lore). */
    @NotNull
    public static ItemStack filler(@NotNull Material mat) {
        return item(mat, " ");
    }

    // ── Inventory filling ─────────────────────────────────────────────────────

    /** Fills every slot with {@code filler}. */
    public static void fill(@NotNull Inventory inv, @NotNull ItemStack filler) {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    /** Fills the border of a multi-row inventory (top row, bottom row, side columns). */
    public static void fillBorder(@NotNull Inventory inv, @NotNull ItemStack border) {
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0;          i < 9;    i++) inv.setItem(i, border);
        for (int i = size - 9;   i < size; i++) inv.setItem(i, border);
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * 9,     border);
            inv.setItem(r * 9 + 8, border);
        }
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    /** Translates {@code &} colour codes to {@code §}. */
    @NotNull
    public static String color(@NotNull String s) {
        return s.replace("&", "§");
    }

    /** Formats a price value as "$12.50". */
    @NotNull
    public static String formatPrice(double price) {
        return String.format("$%.2f", price);
    }

    /** Trend arrow based on multiplier level. */
    @NotNull
    public static String trendArrow(double mult) {
        if (mult >= 0.9) return "&a▲";
        if (mult >= 0.5) return "&e▼";
        return "&c▼▼";
    }

    /** Colour code string based on multiplier level. */
    @NotNull
    public static String priceColor(double mult) {
        if (mult >= 0.9) return "&a";
        if (mult >= 0.6) return "&e";
        if (mult >= 0.35) return "&6";
        return "&c";
    }

    /** 10-character visual bar for price level. */
    @NotNull
    public static String bar(double mult, double minMult, double maxMult) {
        final int LEN = 10;
        double norm   = (mult - minMult) / Math.max(0.001, maxMult - minMult);
        int filled    = (int) Math.round(Math.min(1.0, Math.max(0.0, norm)) * LEN);
        return priceColor(mult) + "█".repeat(filled) + "&8" + "█".repeat(LEN - filled);
    }

    // ── Legacy aliases (kept for backward compat with old callers) ────────────

    /** @deprecated Use {@link #item(Material, String, List)} */
    @Deprecated
    @NotNull
    public static ItemStack buildItem(@NotNull Material mat, @NotNull String name,
                                       @Nullable List<String> lore) {
        return item(mat, name, lore);
    }

    /** @deprecated Use {@link #item(Material, String)} */
    @Deprecated
    @NotNull
    public static ItemStack buildItem(@NotNull Material mat, @NotNull String name) {
        return item(mat, name);
    }

    /** @deprecated Use {@link #filler(Material)} */
    @Deprecated
    @NotNull
    public static ItemStack buildFiller(@NotNull Material mat) {
        return filler(mat);
    }

    /** @deprecated Use {@link #color(String)} */
    @Deprecated
    @NotNull
    public static String colorize(@NotNull String s) {
        return color(s);
    }

    /** @deprecated Use {@link #bar(double, double, double)} */
    @Deprecated
    @NotNull
    public static String multiplierBar(double m, double min, double max) {
        return bar(m, min, max);
    }
}
