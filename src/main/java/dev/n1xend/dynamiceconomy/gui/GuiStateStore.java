package dev.n1xend.dynamiceconomy.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player navigation state for all GUI screens.
 *
 * @author n1xend
 * @version 1.2.2
 */
public final class GuiStateStore {

    public enum SellMode { ALL, STACK, ONE }

    // ── Shop state ────────────────────────────────────────────────────────────
    private final Map<UUID, String>   category   = new HashMap<>();
    private final Map<UUID, Integer>  page       = new HashMap<>();
    private final Map<UUID, String>   sellItem   = new HashMap<>();
    private final Map<UUID, Integer>  sellAmount = new HashMap<>();
    private final Map<UUID, SellMode> sellMode   = new HashMap<>();

    // ── Auction state ─────────────────────────────────────────────────────────
    private final Map<UUID, Integer> auctionPage = new HashMap<>();
    private final Map<UUID, String>  auctionMode = new HashMap<>(); // "ALL" or "MY"

    // ── Shop ──────────────────────────────────────────────────────────────────

    public void setCategory(@Nullable UUID uuid, @Nullable String catId) {
        if (uuid != null) category.put(uuid, catId);
    }
    @Nullable public String getCategory(@Nullable UUID uuid) {
        return uuid == null ? null : category.get(uuid);
    }

    public void setPage(@Nullable UUID uuid, int p) {
        if (uuid != null) page.put(uuid, p);
    }
    public int getPage(@Nullable UUID uuid) {
        return uuid == null ? 0 : page.getOrDefault(uuid, 0);
    }

    public void setSellItem(@Nullable UUID uuid, @Nullable String matId) {
        if (uuid != null) sellItem.put(uuid, matId);
    }
    @Nullable public String getSellItem(@Nullable UUID uuid) {
        return uuid == null ? null : sellItem.get(uuid);
    }

    public void setSellAmount(@Nullable UUID uuid, int a) {
        if (uuid != null) sellAmount.put(uuid, a);
    }
    public int getSellAmount(@Nullable UUID uuid) {
        return uuid == null ? 0 : sellAmount.getOrDefault(uuid, 0);
    }

    public void setSellMode(@Nullable UUID uuid, @Nullable SellMode m) {
        if (uuid != null && m != null) sellMode.put(uuid, m);
    }
    @NotNull public SellMode getSellMode(@Nullable UUID uuid) {
        return uuid == null ? SellMode.ALL : sellMode.getOrDefault(uuid, SellMode.ALL);
    }

    // ── Auction ───────────────────────────────────────────────────────────────

    public void setAuctionPage(@Nullable UUID uuid, int p) {
        if (uuid != null) auctionPage.put(uuid, p);
    }
    public int getAuctionPage(@Nullable UUID uuid) {
        return uuid == null ? 0 : auctionPage.getOrDefault(uuid, 0);
    }

    public void setAuctionMode(@Nullable UUID uuid, @Nullable String mode) {
        if (uuid != null && mode != null) auctionMode.put(uuid, mode);
    }
    @NotNull public String getAuctionMode(@Nullable UUID uuid) {
        return uuid == null ? "ALL" : auctionMode.getOrDefault(uuid, "ALL");
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    public void cleanup(@Nullable UUID uuid) {
        if (uuid == null) return;
        category.remove(uuid);
        page.remove(uuid);
        sellItem.remove(uuid);
        sellAmount.remove(uuid);
        sellMode.remove(uuid);
        auctionPage.remove(uuid);
        auctionMode.remove(uuid);
    }
}
