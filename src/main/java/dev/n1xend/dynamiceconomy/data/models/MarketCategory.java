package dev.n1xend.dynamiceconomy.data.models;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a category in the market GUI.
 *
 * <p>Categories group related items together and support seasonal demand
 * modifiers that apply price bonuses to all items within the category.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class MarketCategory {

    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final int guiSlot;
    private final Map<String, MarketItem> items = new LinkedHashMap<>();

    // Seasonal demand state
    private boolean hotCategory = false;
    private double hotMultiplier = 1.0;

    /**
     * Creates a new market category.
     *
     * @param id          unique category identifier
     * @param displayName colored display name
     * @param description short description shown in GUI lore
     * @param icon        material icon in the main menu
     * @param guiSlot     slot in the main menu inventory
     */
    public MarketCategory(@NotNull String id, @NotNull String displayName,
                           @NotNull String description, @NotNull Material icon, int guiSlot) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.guiSlot = guiSlot;
    }

    // -------------------------------------------------------------------------
    // Item management
    // -------------------------------------------------------------------------

    public void addItem(@NotNull MarketItem item) {
        items.put(item.getId(), item);
    }

    @Nullable
    public MarketItem getItem(@NotNull String materialId) {
        return items.get(materialId);
    }

    @NotNull
    public Collection<MarketItem> getItems() {
        return items.values();
    }

    @NotNull
    public Map<String, MarketItem> getItemMap() {
        return items;
    }

    // -------------------------------------------------------------------------
    // Seasonal demand
    // -------------------------------------------------------------------------

    /**
     * Sets this category as hot (seasonal demand) with a price multiplier.
     *
     * @param hot        whether this category is currently hot
     * @param multiplier price multiplier applied to all items in this category
     */
    public void setHot(boolean hot, double multiplier) {
        this.hotCategory = hot;
        this.hotMultiplier = hot ? multiplier : 1.0;
    }

    /**
     * Returns the effective seasonal multiplier.
     * Returns 1.0 if this category is not hot.
     *
     * @return seasonal price multiplier
     */
    public double getSeasonalMultiplier() {
        return hotCategory ? hotMultiplier : 1.0;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public Material getIcon() {
        return icon;
    }

    public int getGuiSlot() {
        return guiSlot;
    }

    public boolean isHotCategory() {
        return hotCategory;
    }

    public double getHotMultiplier() {
        return hotMultiplier;
    }
}
