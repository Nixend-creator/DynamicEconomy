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
 * Utility methods for building and manipulating Paper inventory GUIs.
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public final class GUIHelper {

    private GUIHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    // -------------------------------------------------------------------------
    // Item building
    // -------------------------------------------------------------------------

    /**
     * Builds an ItemStack with a colored display name and lore.
     *
     * @param material material of the item
     * @param name     display name (& color codes supported)
     * @param lore     lore lines (& color codes supported), may be null
     * @return configured item stack
     */
    @NotNull
    public static ItemStack buildItem(@NotNull Material material, @NotNull String name, @Nullable List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            if (lore != null) {
                meta.setLore(lore.stream().map(GUIHelper::colorize).collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Builds an ItemStack with name and no lore.
     *
     * @param material material
     * @param name     display name
     * @return item stack
     */
    @NotNull
    public static ItemStack buildItem(@NotNull Material material, @NotNull String name) {
        return buildItem(material, name, null);
    }

    /**
     * Creates a named glass pane used as a filler item (no lore, blank name).
     *
     * @param material glass pane material
     * @return filler item
     */
    @NotNull
    public static ItemStack buildFiller(@NotNull Material material) {
        return buildItem(material, " ");
    }

    // -------------------------------------------------------------------------
    // Inventory filling
    // -------------------------------------------------------------------------

    /**
     * Fills the entire inventory with the given item.
     *
     * @param inventory target inventory
     * @param filler    filler item
     */
    public static void fill(@NotNull Inventory inventory, @NotNull ItemStack filler) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    /**
     * Fills the border slots of a multi-row inventory.
     * Border = top row + bottom row + left/right columns.
     *
     * @param inventory target inventory
     * @param border    border item
     */
    public static void fillBorder(@NotNull Inventory inventory, @NotNull ItemStack border) {
        int size = inventory.getSize();
        int rows = size / 9;

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
        }
        for (int i = size - 9; i < size; i++) {
            inventory.setItem(i, border);
        }
        for (int row = 1; row < rows - 1; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    // -------------------------------------------------------------------------
    // Formatting helpers
    // -------------------------------------------------------------------------

    /**
     * Translates {@code &} color codes to {@code §}.
     *
     * @param text input text
     * @return colorized text
     */
    @NotNull
    public static String colorize(@NotNull String text) {
        return text.replace("&", "§");
    }

    /**
     * Formats a double as a price string with 2 decimal places.
     *
     * @param price price value
     * @return formatted string, e.g. "$12.50"
     */
    @NotNull
    public static String formatPrice(double price) {
        return String.format("$%.2f", price);
    }

    /**
     * Returns a colored trend arrow for the given price multiplier.
     * Green up arrow for full price, yellow for mid, red double-down for low.
     *
     * @param multiplier current price multiplier
     * @return colored arrow string with & codes
     */
    @NotNull
    public static String trendArrow(double multiplier) {
        if (multiplier >= 0.9) return "&a▲";
        if (multiplier >= 0.5) return "&e▼";
        return "&c▼▼";
    }

    /**
     * Returns a color code prefix based on price multiplier level.
     *
     * @param multiplier current price multiplier
     * @return color code string (e.g. "&a", "&e", "&c")
     */
    @NotNull
    public static String priceColor(double multiplier) {
        if (multiplier >= 0.9) return "&a";
        if (multiplier >= 0.6) return "&e";
        if (multiplier >= 0.35) return "&6";
        return "&c";
    }

    /**
     * Builds a 10-character visual bar representing the price level.
     *
     * @param multiplier current multiplier
     * @param minMult    minimum possible multiplier
     * @param maxMult    maximum possible multiplier (typically 1.0)
     * @return bar string with & color codes
     */
    @NotNull
    public static String multiplierBar(double multiplier, double minMult, double maxMult) {
        final int barLength = 10;
        double normalized = (multiplier - minMult) / Math.max(0.001, maxMult - minMult);
        int filled = (int) Math.round(Math.min(1.0, Math.max(0.0, normalized)) * barLength);

        String color = priceColor(multiplier);
        return color + "█".repeat(filled) + "&8" + "█".repeat(barLength - filled);
    }
}
