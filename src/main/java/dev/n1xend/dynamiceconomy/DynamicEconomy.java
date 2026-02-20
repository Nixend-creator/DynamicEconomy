package dev.n1xend.dynamiceconomy;

import dev.n1xend.dynamiceconomy.api.DynamicEconomyAPI;
import dev.n1xend.dynamiceconomy.auction.AuctionService;
import dev.n1xend.dynamiceconomy.commands.CommandManager;
import dev.n1xend.dynamiceconomy.config.ConfigManager;
import dev.n1xend.dynamiceconomy.config.MessageManager;
import dev.n1xend.dynamiceconomy.data.DataManager;
import dev.n1xend.dynamiceconomy.gui.GuiStateStore;
import dev.n1xend.dynamiceconomy.history.PriceHistoryService;
import dev.n1xend.dynamiceconomy.license.LicenseService;
import dev.n1xend.dynamiceconomy.listeners.GuiListener;
import dev.n1xend.dynamiceconomy.market.MarketEventService;
import dev.n1xend.dynamiceconomy.placeholder.PlaceholderHook;
import dev.n1xend.dynamiceconomy.region.RegionalMarketService;
import dev.n1xend.dynamiceconomy.rest.RestApiServer;
import dev.n1xend.dynamiceconomy.services.*;
import dev.n1xend.dynamiceconomy.treasury.TreasuryService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Main entry point for DynamicEconomy.
 *
 * @author n1xend
 * @version 1.2.1
 */
public final class DynamicEconomy extends JavaPlugin {

    private static DynamicEconomy instance;

    // Core
    private Economy          vaultEconomy;
    private ConfigManager    configManager;
    private MessageManager   messageManager;
    private DataManager      dataManager;
    private EconomyService   economyService;
    private ContractService  contractService;
    private BuyService       buyService;
    private GuiStateStore    guiStateStore;

    // Extended (1.2.x)
    private TreasuryService       treasuryService;
    private PriceHistoryService   priceHistoryService;
    private MarketEventService    marketEventService;
    private LicenseService        licenseService;
    private AuctionService        auctionService;
    private RegionalMarketService regionalMarketService;
    private RestApiServer         restApiServer;
    private DynamicEconomyAPI     api;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupVault()) {
            getLogger().severe("Vault not found or no economy provider! Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        initManagers();
        registerListeners();
        registerCommands();
        startTasks();
        registerPlaceholders();
        startRestApi();

        getLogger().info("DynamicEconomy v" + getDescription().getVersion() + " enabled. "
                + economyService.getItemIndex().size() + " items loaded.");
    }

    @Override
    public void onDisable() {
        if (marketEventService  != null) marketEventService.cancelAll();
        if (restApiServer       != null) restApiServer.stop();
        if (dataManager         != null) dataManager.save();
        if (treasuryService     != null) treasuryService.save();
        if (priceHistoryService != null) priceHistoryService.save();
        if (licenseService      != null) licenseService.save();
        if (auctionService      != null) auctionService.save();
        getLogger().info("DynamicEconomy disabled. Data saved.");
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initManagers() {
        configManager  = new ConfigManager(this);
        messageManager = new MessageManager(this);
        guiStateStore  = new GuiStateStore();

        MarketLoader loader = new MarketLoader(this);
        economyService  = new EconomyService(this, loader.loadCategories());
        contractService = new ContractService(this);
        buyService      = new BuyService(this);

        treasuryService       = new TreasuryService(this);
        licenseService        = new LicenseService(this);
        regionalMarketService = new RegionalMarketService(this);
        marketEventService    = new MarketEventService(this);
        auctionService        = new AuctionService(this);
        priceHistoryService   = new PriceHistoryService(this);

        dataManager = new DataManager(this);
        dataManager.load();

        api = new DynamicEconomyAPI(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
    }

    private void registerCommands() {
        new CommandManager(this).register();
    }

    private void startTasks() {
        economyService.startRecoveryTask();
        contractService.startContractTask();
        marketEventService.startEventTask();
        priceHistoryService.startSnapshotTask();
        startAutoSave();
    }

    private void startAutoSave() {
        long ticks = (long) configManager.getAutoSaveIntervalMinutes() * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            dataManager.save();
            treasuryService.save();
            licenseService.save();
            priceHistoryService.save();
        }, ticks, ticks);
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("[PlaceholderAPI] Expansion registered.");
        }
    }

    private void startRestApi() {
        restApiServer = new RestApiServer(this);
        Bukkit.getScheduler().runTaskAsynchronously(this, restApiServer::start);
    }

    private boolean setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        vaultEconomy = rsp.getProvider();
        return true;
    }

    // ── Static accessor ───────────────────────────────────────────────────────

    public static DynamicEconomy getInstance() {
        return Objects.requireNonNull(instance, "Plugin not yet initialized");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Economy                getVaultEconomy()           { return vaultEconomy; }
    public ConfigManager          getConfigManager()          { return configManager; }
    public MessageManager         getMessageManager()         { return messageManager; }
    public DataManager            getDataManager()            { return dataManager; }
    public EconomyService         getEconomyService()         { return economyService; }
    public ContractService        getContractService()        { return contractService; }
    public BuyService             getBuyService()             { return buyService; }
    public GuiStateStore          getGuiStateStore()          { return guiStateStore; }
    public TreasuryService        getTreasuryService()        { return treasuryService; }
    public PriceHistoryService    getPriceHistoryService()    { return priceHistoryService; }
    public MarketEventService     getMarketEventService()     { return marketEventService; }
    public LicenseService         getLicenseService()         { return licenseService; }
    public AuctionService         getAuctionService()         { return auctionService; }
    public RegionalMarketService  getRegionalMarketService()  { return regionalMarketService; }
    public DynamicEconomyAPI      getAPI()                    { return api; }
}
