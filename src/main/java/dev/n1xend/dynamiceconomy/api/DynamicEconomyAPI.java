package dev.n1xend.dynamiceconomy.api;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketCategory;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Public API for DynamicEconomy plugin.
 *
 * <p>External plugins can access market data through this class.
 * Access via: {@code DynamicEconomy.getInstance().getAPI()}</p>
 *
 * <p>Example:
 * <pre>{@code
 * DynamicEconomyAPI api = DynamicEconomy.getInstance().getAPI();
 * double price = api.getCurrentPrice("WHEAT");
 * }</pre></p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class DynamicEconomyAPI {

    private final DynamicEconomy plugin;

    public DynamicEconomyAPI(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Price queries
    // -------------------------------------------------------------------------

    /**
     * Returns the current sell price per unit for a material, after tax.
     *
     * @param materialId material name (e.g. "WHEAT")
     * @return current net price per unit, or -1 if material is not in the market
     */
    public double getCurrentPrice(@NotNull String materialId) {
        MarketItem item = plugin.getEconomyService().getItem(materialId);
        if (item == null) {
            return -1;
        }
        MarketCategory category = plugin.getEconomyService().getCategory(item.getCategoryId());
        double seasonalMult = (category != null) ? category.getSeasonalMultiplier() : 1.0;
        double taxRate = plugin.getEconomyService().getPriceCalculator().getSellTaxRate();
        return item.getCurrentPrice() * seasonalMult * (1.0 - taxRate);
    }

    /**
     * Returns the current price multiplier for a material.
     *
     * @param materialId material name
     * @return current multiplier (1.0 = base price), or -1 if not found
     */
    public double getPriceMultiplier(@NotNull String materialId) {
        MarketItem item = plugin.getEconomyService().getItem(materialId);
        return item != null ? item.getCurrentMultiplier() : -1;
    }

    /**
     * Returns the base price for a material.
     *
     * @param materialId material name
     * @return base price in Vault currency, or -1 if not found
     */
    public double getBasePrice(@NotNull String materialId) {
        MarketItem item = plugin.getEconomyService().getItem(materialId);
        return item != null ? item.getBasePrice() : -1;
    }

    // -------------------------------------------------------------------------
    // Market data
    // -------------------------------------------------------------------------

    /**
     * Returns all registered market categories.
     *
     * @return unmodifiable view of categories
     */
    @NotNull
    public Map<String, MarketCategory> getCategories() {
        return plugin.getEconomyService().getCategories();
    }

    /**
     * Returns a market item by material ID.
     *
     * @param materialId material identifier
     * @return market item or null if not registered
     */
    @Nullable
    public MarketItem getMarketItem(@NotNull String materialId) {
        return plugin.getEconomyService().getItem(materialId);
    }

    /**
     * Returns true if the given material is registered in the market.
     *
     * @param materialId material name
     * @return whether the material can be sold
     */
    public boolean isMarketItem(@NotNull String materialId) {
        return plugin.getEconomyService().getItem(materialId) != null;
    }

    /**
     * Returns the currently active hot category ID (seasonal demand), or null if none.
     *
     * @return hot category ID or null
     */
    @Nullable
    public String getHotCategoryId() {
        return plugin.getEconomyService().getHotCategoryId();
    }

    /**
     * Returns all currently active contracts.
     */
    @NotNull
    public Collection<?> getActiveContracts() {
        return plugin.getContractService().getActiveContracts();
    }
}
