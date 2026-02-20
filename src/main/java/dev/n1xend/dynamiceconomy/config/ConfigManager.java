package dev.n1xend.dynamiceconomy.config;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed accessor for plugin configuration values.
 *
 * <p>All config reads are centralized here to avoid raw string keys
 * scattered throughout the codebase.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class ConfigManager {

    private final DynamicEconomy plugin;

    public ConfigManager(DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Economy
    // -------------------------------------------------------------------------

    public double getMinPriceMultiplier() {
        return getConfig().getDouble("economy.min-price-multiplier", 0.2);
    }

    public double getMaxPriceMultiplier() {
        return getConfig().getDouble("economy.max-price-multiplier", 1.0);
    }

    public double getPriceDropPerStack() {
        return getConfig().getDouble("economy.price-drop-per-stack", 0.02);
    }

    public double getPriceRecoveryPerHour() {
        return getConfig().getDouble("economy.price-recovery-per-hour", 0.05);
    }

    public double getSellTax() {
        return getConfig().getDouble("economy.sell-tax", 0.05);
    }

    public int getSellCooldownSeconds() {
        return getConfig().getInt("economy.sell-cooldown-seconds", 3);
    }

    public int getMaxSellAmount() {
        return getConfig().getInt("economy.max-sell-amount", 2304);
    }

    // -------------------------------------------------------------------------
    // Seasonal demand
    // -------------------------------------------------------------------------

    public boolean isSeasonalDemandEnabled() {
        return getConfig().getBoolean("seasonal-demand.enabled", true);
    }

    public long getSeasonalChangeIntervalMinutes() {
        return getConfig().getLong("seasonal-demand.change-interval-minutes", 1440);
    }

    public double getHotMultiplier() {
        return getConfig().getDouble("seasonal-demand.hot-multiplier", 1.5);
    }

    // -------------------------------------------------------------------------
    // Contracts
    // -------------------------------------------------------------------------

    public boolean isContractsEnabled() {
        return getConfig().getBoolean("contracts.enabled", true);
    }

    public int getContractsMaxActive() {
        return getConfig().getInt("contracts.max-active", 3);
    }

    public long getContractSpawnIntervalMinutes() {
        return getConfig().getLong("contracts.spawn-interval-minutes", 60);
    }

    public double getContractBonusMultiplier() {
        return getConfig().getDouble("contracts.bonus-multiplier", 0.4);
    }

    public long getContractDurationMinutes() {
        return getConfig().getLong("contracts.duration-minutes", 180);
    }

    public int getContractAmountMin() {
        return getConfig().getInt("contracts.amount-min", 200);
    }

    public int getContractAmountMax() {
        return getConfig().getInt("contracts.amount-max", 800);
    }

    // -------------------------------------------------------------------------
    // Diversity bonus
    // -------------------------------------------------------------------------

    public boolean isDiversityBonusEnabled() {
        return getConfig().getBoolean("diversity-bonus.enabled", true);
    }

    public int getDiversityMinCategories() {
        return getConfig().getInt("diversity-bonus.min-categories", 3);
    }

    public long getDiversityWindowMinutes() {
        return getConfig().getLong("diversity-bonus.window-minutes", 30);
    }

    public double getDiversityBonusMultiplier() {
        return getConfig().getDouble("diversity-bonus.bonus-multiplier", 0.1);
    }

    // -------------------------------------------------------------------------
    // GUI
    // -------------------------------------------------------------------------

    public Material getGuiFiller() {
        String name = getConfig().getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE");
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : Material.BLACK_STAINED_GLASS_PANE;
    }

    public Material getGuiEmpty() {
        String name = getConfig().getString("gui.empty-material", "GRAY_STAINED_GLASS_PANE");
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : Material.GRAY_STAINED_GLASS_PANE;
    }

    // -------------------------------------------------------------------------
    // Data / Logging
    // -------------------------------------------------------------------------

    public int getAutoSaveIntervalMinutes() {
        return getConfig().getInt("data.auto-save-interval-minutes", 5);
    }

    public boolean isLogSales() {
        return getConfig().getBoolean("logging.log-sales", false);
    }

    public String getLanguage() {
        return getConfig().getString("language", "ru");
    }

    // -------------------------------------------------------------------------
    // Private
    // -------------------------------------------------------------------------

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    // ── Buy mode (1.2.0) ──────────────────────────────────────────────────────

    public boolean isBuyModeEnabled() {
        return getConfig().getBoolean("buy-mode.enabled", true);
    }

    public double getBuySpreadMultiplier() {
        return getConfig().getDouble("buy-mode.spread-multiplier", 1.3);
    }

    public double getBuyTreasuryCut() {
        return getConfig().getDouble("buy-mode.treasury-cut", 0.10);
    }

    // ── Market events (1.2.0) ─────────────────────────────────────────────────

    public boolean isMarketEventsEnabled() {
        return getConfig().getBoolean("market-events.enabled", true);
    }

    public int getEventMinIntervalMinutes() {
        return getConfig().getInt("market-events.min-interval-minutes", 20);
    }

    public int getEventMaxIntervalMinutes() {
        return getConfig().getInt("market-events.max-interval-minutes", 60);
    }

    public int getEventDurationMinutes() {
        return getConfig().getInt("market-events.duration-minutes", 30);
    }

    // ── Licenses (1.2.0) ──────────────────────────────────────────────────────

    public boolean isLicensesEnabled() {
        return getConfig().getBoolean("licenses.enabled", true);
    }

    public long getMerchantThreshold() {
        return getConfig().getLong("licenses.merchant-threshold", 10_000L);
    }

    public long getTycoonThreshold() {
        return getConfig().getLong("licenses.tycoon-threshold", 50_000L);
    }

    // ── History (1.2.0) ───────────────────────────────────────────────────────

    public int getHistorySnapshotIntervalMinutes() {
        return getConfig().getInt("history.snapshot-interval-minutes", 15);
    }

    // ── Logging extras (1.2.0) ────────────────────────────────────────────────

    public boolean isLogBuys() {
        return getConfig().getBoolean("logging.log-buys", false);
    }

    public boolean isLogEvents() {
        return getConfig().getBoolean("logging.log-events", true);
    }

}
