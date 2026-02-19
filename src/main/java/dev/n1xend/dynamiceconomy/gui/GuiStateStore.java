package dev.n1xend.dynamiceconomy.gui;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory store for per-player GUI navigation state.
 *
 * <p>Tracks which category and page each player is viewing, and the pending
 * sell operation data needed by the confirmation screen.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public final class GuiStateStore {

    /** Sell mode options available in the category GUI. */
    public enum SellMode {
        /** Sell all items of this material in inventory. */
        ALL,
        /** Sell one full stack (64). */
        STACK,
        /** Sell exactly one item. */
        ONE
    }

    private GuiStateStore() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<UUID, String> playerCategory = new HashMap<>();
    private static final Map<UUID, Integer> playerPage = new HashMap<>();
    private static final Map<UUID, String> playerSellItem = new HashMap<>();
    private static final Map<UUID, Integer> playerSellAmount = new HashMap<>();
    private static final Map<UUID, SellMode> playerSellMode = new HashMap<>();

    public static void setCategory(@Nullable UUID uuid, @Nullable String categoryId) {
        if (uuid != null) playerCategory.put(uuid, categoryId);
    }

    @Nullable
    public static String getCategory(@Nullable UUID uuid) {
        return uuid != null ? playerCategory.get(uuid) : null;
    }

    public static void setPage(@Nullable UUID uuid, int page) {
        if (uuid != null) playerPage.put(uuid, page);
    }

    public static int getPage(@Nullable UUID uuid) {
        return uuid != null ? playerPage.getOrDefault(uuid, 0) : 0;
    }

    public static void setSellItem(@Nullable UUID uuid, @Nullable String materialId) {
        if (uuid != null) playerSellItem.put(uuid, materialId);
    }

    @Nullable
    public static String getSellItem(@Nullable UUID uuid) {
        return uuid != null ? playerSellItem.get(uuid) : null;
    }

    public static void setSellAmount(@Nullable UUID uuid, int amount) {
        if (uuid != null) playerSellAmount.put(uuid, amount);
    }

    public static int getSellAmount(@Nullable UUID uuid) {
        return uuid != null ? playerSellAmount.getOrDefault(uuid, 0) : 0;
    }

    public static void setSellMode(@Nullable UUID uuid, @Nullable SellMode mode) {
        if (uuid != null && mode != null) playerSellMode.put(uuid, mode);
    }

    public static SellMode getSellMode(@Nullable UUID uuid) {
        return uuid != null ? playerSellMode.getOrDefault(uuid, SellMode.ALL) : SellMode.ALL;
    }

    /** Clears all state for a player. Call when they close the shop entirely. */
    public static void cleanup(@Nullable UUID uuid) {
        if (uuid == null) return;
        playerCategory.remove(uuid);
        playerPage.remove(uuid);
        playerSellItem.remove(uuid);
        playerSellAmount.remove(uuid);
        playerSellMode.remove(uuid);
    }
}
