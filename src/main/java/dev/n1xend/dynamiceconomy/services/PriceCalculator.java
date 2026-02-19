package dev.n1xend.dynamiceconomy.services;

import dev.n1xend.dynamiceconomy.config.ConfigManager;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import org.jetbrains.annotations.NotNull;

/**
 * Handles all price calculations for the dynamic market economy.
 *
 * <p>This class is stateless and contains only pure mathematical operations.
 * Price drop formula: {@code drop = dropPerStack × (amount / 64.0)}<br>
 * Recovery formula: {@code recovered = currentMultiplier + (recoveryPerHour × hoursElapsed)}<br>
 * Payout formula: {@code gross = price × seasonalMult × diversityMult × contractMult × amount}<br>
 * {@code net = gross × (1 - taxRate)}</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class PriceCalculator {

    private final ConfigManager configManager;

    public PriceCalculator(@NotNull ConfigManager configManager) {
        this.configManager = configManager;
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Applies a sell event to an item — drops multiplier proportional to quantity.
     *
     * <p>Each 64 units equals one full {@code price-drop-per-stack} tick.
     * Multiplier is clamped to configured minimum.</p>
     *
     * @param item   the market item
     * @param amount units sold
     */
    public void applySale(@NotNull MarketItem item, int amount) {
        double drop = configManager.getPriceDropPerStack() * (amount / 64.0);
        double newMultiplier = Math.max(configManager.getMinPriceMultiplier(), item.getCurrentMultiplier() - drop);
        item.setCurrentMultiplier(newMultiplier);
        item.recordSell(amount);
    }

    /**
     * Applies time-based price recovery toward base multiplier of 1.0.
     *
     * <p>Called periodically by the recovery scheduler.
     * Does nothing if the item is already at full base price.</p>
     *
     * @param item         the market item
     * @param hoursElapsed hours elapsed since last recovery tick
     */
    public void applyRecovery(@NotNull MarketItem item, double hoursElapsed) {
        if (item.getCurrentMultiplier() >= 1.0) {
            return;
        }
        double recovered = item.getCurrentMultiplier() + (configManager.getPriceRecoveryPerHour() * hoursElapsed);
        item.setCurrentMultiplier(Math.min(1.0, recovered));
    }

    // -------------------------------------------------------------------------
    // Payout calculation
    // -------------------------------------------------------------------------

    /**
     * Calculates the final payout for a sell transaction.
     *
     * @param item               market item being sold
     * @param amount             quantity sold
     * @param seasonalMultiplier category seasonal demand multiplier (1.0 = none)
     * @param diversityMultiplier extra multiplier for selling diverse categories (1.0 = none)
     * @param contractMultiplier  extra multiplier if an active contract exists (1.0 = none)
     * @return net payout after tax, minimum 0.01
     */
    public double calculatePayout(@NotNull MarketItem item, int amount,
                                   double seasonalMultiplier, double diversityMultiplier, double contractMultiplier) {
        double rawPrice = item.getCurrentPrice() * seasonalMultiplier * diversityMultiplier * contractMultiplier;
        double gross = rawPrice * amount;
        return Math.max(0.01, gross * (1.0 - configManager.getSellTax()));
    }

    /**
     * Calculates preview payout for GUI display without applying bonuses.
     * Used to show base expected earnings before confirming a sell.
     *
     * @param item               market item
     * @param amount             quantity
     * @param seasonalMultiplier seasonal demand multiplier
     * @return preview net payout
     */
    public double calculatePreviewPayout(@NotNull MarketItem item, int amount, double seasonalMultiplier) {
        double rawPrice = item.getCurrentPrice() * seasonalMultiplier;
        double gross = rawPrice * amount;
        return Math.max(0.01, gross * (1.0 - configManager.getSellTax()));
    }

    // -------------------------------------------------------------------------
    // Previews
    // -------------------------------------------------------------------------

    /**
     * Returns what the multiplier will be AFTER selling the given amount.
     * Used in the sell confirmation GUI to show price impact.
     *
     * @param item   market item
     * @param amount quantity to be sold
     * @return predicted multiplier after sale
     */
    public double previewMultiplierAfterSale(@NotNull MarketItem item, int amount) {
        double drop = configManager.getPriceDropPerStack() * (amount / 64.0);
        return Math.max(configManager.getMinPriceMultiplier(), item.getCurrentMultiplier() - drop);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public double getMinMultiplier() {
        return configManager.getMinPriceMultiplier();
    }

    public double getMaxMultiplier() {
        return configManager.getMaxPriceMultiplier();
    }

    public double getSellTaxRate() {
        return configManager.getSellTax();
    }
}
