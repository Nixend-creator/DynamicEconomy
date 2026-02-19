package dev.n1xend.dynamiceconomy.services;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketCategory;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central service managing the dynamic market economy.
 *
 * <p>Handles player sells, price recovery scheduling, seasonal demand rotation,
 * and diversity bonus tracking. All sell operations run on the main thread.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class EconomyService {

    /** Result codes for sell operations. */
    public enum SellResult {
        SUCCESS, COOLDOWN, ITEM_NOT_SOLD, NOT_ENOUGH_ITEMS
    }

    /**
     * Immutable result of a sell transaction.
     *
     * @param result          outcome code
     * @param payout          amount credited to player (0 if not SUCCESS)
     * @param pricePerUnit    net price per single unit
     * @param multiplierAfter predicted multiplier after this sale
     * @param itemDisplayName colored display name of item
     * @param hadDiversityBonus whether diversity bonus was applied
     * @param hadContractBonus whether contract bonus was applied
     */
    public record SellData(
        SellResult result,
        double payout,
        double pricePerUnit,
        double multiplierAfter,
        String itemDisplayName,
        boolean hadDiversityBonus,
        boolean hadContractBonus
    ) {}

    // -------------------------------------------------------------------------

    private final DynamicEconomy plugin;
    private final Logger logger;
    private final Map<String, MarketCategory> categories;
    private final Map<String, MarketItem> itemIndex = new HashMap<>();
    private final PriceCalculator priceCalculator;

    // Player sell cooldown — uuid → epoch millis of last sell
    private final Map<UUID, Long> sellCooldowns = new ConcurrentHashMap<>();

    // Diversity tracker — uuid → categoryId → last sell epoch millis
    private final Map<UUID, Map<String, Long>> diversityTracker = new ConcurrentHashMap<>();

    private BukkitTask recoveryTask;
    private String hotCategoryId = null;

    public EconomyService(@NotNull DynamicEconomy plugin, @NotNull Map<String, MarketCategory> categories) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.categories = categories;
        this.priceCalculator = new PriceCalculator(plugin.getConfigManager());

        // Build flat item index for O(1) lookup
        for (MarketCategory category : categories.values()) {
            for (MarketItem item : category.getItems()) {
                itemIndex.put(item.getId(), item);
            }
        }

        if (plugin.getConfigManager().isSeasonalDemandEnabled()) {
            scheduleSeasonalDemand();
        }
    }

    // -------------------------------------------------------------------------
    // Sell API
    // -------------------------------------------------------------------------

    /**
     * Attempts to sell all of a material from the player's inventory.
     *
     * @param player     the selling player
     * @param materialId material identifier (e.g. "WHEAT")
     * @return sell result with payout and market impact data
     */
    @NotNull
    public SellData trySellAll(@NotNull Player player, @NotNull String materialId) {
        return trySell(player, materialId, -1);
    }

    /**
     * Attempts to sell a specific amount of a material.
     *
     * @param player     the selling player
     * @param materialId material identifier
     * @param amount     amount to sell, or -1 to sell all
     * @return sell result
     */
    @NotNull
    public SellData trySell(@NotNull Player player, @NotNull String materialId, int amount) {
        if (isOnCooldown(player)) {
            return failResult(SellResult.COOLDOWN, "");
        }

        MarketItem item = itemIndex.get(materialId);
        if (item == null) {
            return failResult(SellResult.ITEM_NOT_SOLD, materialId);
        }

        int inInventory = countItemsInInventory(player, item);
        if (inInventory <= 0) {
            return failResult(SellResult.NOT_ENOUGH_ITEMS, item.getDisplayName());
        }

        int toSell = (amount == -1) ? inInventory : Math.min(amount, inInventory);
        toSell = Math.min(toSell, plugin.getConfigManager().getMaxSellAmount());

        // Calculate bonuses before applying sale
        MarketCategory category = categories.get(item.getCategoryId());
        double seasonalMult = (category != null) ? category.getSeasonalMultiplier() : 1.0;

        boolean hasDiversity = hasDiversityBonus(player, item.getCategoryId());
        double diversityMult = hasDiversity
            ? (1.0 + plugin.getConfigManager().getDiversityBonusMultiplier())
            : 1.0;

        boolean hasContract = plugin.getContractService().hasActiveContractFor(materialId);
        double contractMult = hasContract
            ? (1.0 + plugin.getConfigManager().getContractBonusMultiplier())
            : 1.0;

        double payout = priceCalculator.calculatePayout(item, toSell, seasonalMult, diversityMult, contractMult);
        double pricePerUnit = payout / toSell;
        double multAfter = priceCalculator.previewMultiplierAfterSale(item, toSell);

        // Execute — remove items, apply drop, pay
        removeItemsFromInventory(player, item, toSell);
        priceCalculator.applySale(item, toSell);
        plugin.getVaultEconomy().depositPlayer(player, payout);

        // Update tracking
        sellCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        updateDiversityTracker(player, item.getCategoryId());
        plugin.getContractService().onSell(player, materialId, toSell);

        if (plugin.getConfigManager().isLogSales()) {
            logger.info("[SALE] " + player.getName() + " sold " + toSell + "x " + materialId
                + " for " + String.format("%.2f", payout)
                + " (mult: " + String.format("%.2f", item.getCurrentMultiplier()) + ")");
        }

        return new SellData(SellResult.SUCCESS, payout, pricePerUnit, multAfter,
            item.getDisplayName(), hasDiversity, hasContract);
    }

    // -------------------------------------------------------------------------
    // Price Recovery Task
    // -------------------------------------------------------------------------

    /**
     * Starts the periodic price recovery scheduler.
     * Runs asynchronously every 3 real minutes, treating each tick as 3/60 hours.
     */
    public void startRecoveryTask() {
        // 3-minute intervals = 3/60 hours per tick
        long intervalTicks = 20L * 60 * 3;
        double hoursPerTick = 3.0 / 60.0;

        recoveryTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (MarketItem item : itemIndex.values()) {
                priceCalculator.applyRecovery(item, hoursPerTick);
            }
        }, intervalTicks, intervalTicks);
    }

    // -------------------------------------------------------------------------
    // Seasonal Demand
    // -------------------------------------------------------------------------

    private void scheduleSeasonalDemand() {
        long intervalTicks = 20L * 60 * plugin.getConfigManager().getSeasonalChangeIntervalMinutes();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::rotateHotCategory, intervalTicks, intervalTicks);
        rotateHotCategory();
    }

    private void rotateHotCategory() {
        double hotMult = plugin.getConfigManager().getHotMultiplier();

        if (hotCategoryId != null && categories.containsKey(hotCategoryId)) {
            categories.get(hotCategoryId).setHot(false, 1.0);
        }

        List<String> keys = new ArrayList<>(categories.keySet());
        hotCategoryId = keys.get(new Random().nextInt(keys.size()));
        categories.get(hotCategoryId).setHot(true, hotMult);

        String catName = categories.get(hotCategoryId).getDisplayName();
        String msg = plugin.getMessageManager().get("seasonal.rotation",
            "%category%", catName,
            "%multiplier%", String.format("%.1f", hotMult));

        plugin.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        logger.info("Seasonal hot category: " + hotCategoryId);
    }

    // -------------------------------------------------------------------------
    // Diversity Bonus
    // -------------------------------------------------------------------------

    private boolean hasDiversityBonus(@NotNull Player player, @NotNull String soldCategoryId) {
        if (!plugin.getConfigManager().isDiversityBonusEnabled()) {
            return false;
        }

        int minCategories = plugin.getConfigManager().getDiversityMinCategories();
        long windowMs = plugin.getConfigManager().getDiversityWindowMinutes() * 60_000L;
        long now = System.currentTimeMillis();

        Map<String, Long> tracker = diversityTracker.getOrDefault(player.getUniqueId(), Collections.emptyMap());

        // Count distinct categories sold within the time window
        long distinctCount = tracker.entrySet().stream()
            .filter(e -> (now - e.getValue()) <= windowMs)
            .count();

        // Include current category if it's new or expired
        Long lastSell = tracker.get(soldCategoryId);
        if (lastSell == null || (now - lastSell) > windowMs) {
            distinctCount++;
        }

        return distinctCount >= minCategories;
    }

    private void updateDiversityTracker(@NotNull Player player, @NotNull String categoryId) {
        diversityTracker
            .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
            .put(categoryId, System.currentTimeMillis());
    }

    // -------------------------------------------------------------------------
    // Inventory utilities
    // -------------------------------------------------------------------------

    private int countItemsInInventory(@NotNull Player player, @NotNull MarketItem item) {
        int count = 0;
        for (var stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == item.getMaterial()) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void removeItemsFromInventory(@NotNull Player player, @NotNull MarketItem item, int amount) {
        int remaining = amount;
        var contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            var stack = contents[i];
            if (stack == null || stack.getType() != item.getMaterial()) {
                continue;
            }
            if (stack.getAmount() <= remaining) {
                remaining -= stack.getAmount();
                player.getInventory().setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - remaining);
                remaining = 0;
            }
        }

        player.updateInventory();
    }

    // -------------------------------------------------------------------------
    // Cooldown
    // -------------------------------------------------------------------------

    private boolean isOnCooldown(@NotNull Player player) {
        if (player.hasPermission("dynamiceconomy.bypass.cooldown")) {
            return false;
        }
        long lastSell = sellCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long elapsedMs = System.currentTimeMillis() - lastSell;
        return elapsedMs < plugin.getConfigManager().getSellCooldownSeconds() * 1000L;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SellData failResult(@NotNull SellResult result, @NotNull String displayName) {
        return new SellData(result, 0, 0, 0, displayName, false, false);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    @NotNull
    public Map<String, MarketCategory> getCategories() {
        return categories;
    }

    @Nullable
    public MarketCategory getCategory(@NotNull String id) {
        return categories.get(id);
    }

    @Nullable
    public MarketItem getItem(@NotNull String materialId) {
        return itemIndex.get(materialId);
    }

    @NotNull
    public Map<String, MarketItem> getItemIndex() {
        return itemIndex;
    }

    @NotNull
    public PriceCalculator getPriceCalculator() {
        return priceCalculator;
    }

    @Nullable
    public String getHotCategoryId() {
        return hotCategoryId;
    }
}
