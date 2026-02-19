package dev.n1xend.dynamiceconomy.data.models;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a single tradeable item in the dynamic market.
 *
 * <p>Tracks the current price multiplier, last sell timestamp for recovery
 * calculation, and cumulative sell statistics.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class MarketItem {

    private final String id;
    private final String categoryId;
    private final String displayName;
    private final Material material;
    private final double basePrice;

    // Dynamic state — persisted to JSON between restarts
    private double currentMultiplier;
    private long lastSellTimestamp;
    private long totalSold;

    /**
     * Creates a new market item with default multiplier of 1.0.
     *
     * @param id          material name used as unique identifier (e.g. "WHEAT")
     * @param categoryId  parent category identifier
     * @param displayName colored display name shown in GUI
     * @param material    bukkit material
     * @param basePrice   base price in Vault currency
     */
    public MarketItem(@NotNull String id, @NotNull String categoryId,
                      @NotNull String displayName, @NotNull Material material, double basePrice) {
        this.id = id;
        this.categoryId = categoryId;
        this.displayName = displayName;
        this.material = material;
        this.basePrice = basePrice;
        this.currentMultiplier = 1.0;
        this.lastSellTimestamp = System.currentTimeMillis();
        this.totalSold = 0;
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Returns the actual current price = basePrice × currentMultiplier.
     *
     * @return current price before tax and bonuses
     */
    public double getCurrentPrice() {
        return basePrice * currentMultiplier;
    }

    /**
     * Returns price trend relative to base as a value from -1.0 to +1.0.
     * Negative means price is suppressed, positive means above base.
     *
     * @return trend value
     */
    public double getTrend() {
        return currentMultiplier - 1.0;
    }

    /**
     * Records a sell event — updates timestamp and total.
     *
     * @param amount units sold
     */
    public void recordSell(int amount) {
        this.lastSellTimestamp = System.currentTimeMillis();
        this.totalSold += amount;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getCategoryId() {
        return categoryId;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public Material getMaterial() {
        return material;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getCurrentMultiplier() {
        return currentMultiplier;
    }

    public long getLastSellTimestamp() {
        return lastSellTimestamp;
    }

    public long getTotalSold() {
        return totalSold;
    }

    // -------------------------------------------------------------------------
    // Setters (package-private for service layer)
    // -------------------------------------------------------------------------

    public void setCurrentMultiplier(double multiplier) {
        this.currentMultiplier = multiplier;
    }

    public void setLastSellTimestamp(long timestamp) {
        this.lastSellTimestamp = timestamp;
    }

    public void setTotalSold(long totalSold) {
        this.totalSold = totalSold;
    }
}
