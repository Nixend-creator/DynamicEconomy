package dev.n1xend.dynamiceconomy;

import dev.n1xend.dynamiceconomy.api.DynamicEconomyAPI;
import dev.n1xend.dynamiceconomy.commands.CommandManager;
import dev.n1xend.dynamiceconomy.config.ConfigManager;
import dev.n1xend.dynamiceconomy.config.MessageManager;
import dev.n1xend.dynamiceconomy.data.DataManager;
import dev.n1xend.dynamiceconomy.listeners.GuiListener;
import dev.n1xend.dynamiceconomy.services.ContractService;
import dev.n1xend.dynamiceconomy.services.EconomyService;
import dev.n1xend.dynamiceconomy.services.MarketLoader;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for DynamicEconomy plugin.
 *
 * <p>Dynamic supply & demand economy for Paper 1.21.1.
 * Prices fall as players sell more and recover over real time.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public final class DynamicEconomy extends JavaPlugin {

    private static DynamicEconomy instance;

    private Economy vaultEconomy;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DataManager dataManager;
    private EconomyService economyService;
    private ContractService contractService;
    private DynamicEconomyAPI api;

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

        getLogger().info("DynamicEconomy v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.save();
        }
        getLogger().info("DynamicEconomy disabled. Data saved.");
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    private void initManagers() {
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);

        MarketLoader marketLoader = new MarketLoader(this);
        economyService = new EconomyService(this, marketLoader.loadCategories());
        contractService = new ContractService(this);

        dataManager = new DataManager(this);
        dataManager.load();

        api = new DynamicEconomyAPI(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
    }

    private void registerCommands() {
        CommandManager commandManager = new CommandManager(this);
        commandManager.register();
    }

    private void startTasks() {
        economyService.startRecoveryTask();
        contractService.startContractTask();
        startAutoSave();
    }

    private void startAutoSave() {
        long intervalTicks = (long) configManager.getAutoSaveIntervalMinutes() * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, dataManager::save, intervalTicks, intervalTicks);
    }

    private boolean setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        vaultEconomy = rsp.getProvider();
        return true;
    }

    // -------------------------------------------------------------------------
    // Static accessor
    // -------------------------------------------------------------------------

    /**
     * Returns the plugin instance.
     *
     * @return plugin instance, never null after onEnable
     */
    public static DynamicEconomy getInstance() {
        return instance;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Economy getVaultEconomy() {
        return vaultEconomy;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public ContractService getContractService() {
        return contractService;
    }

    public DynamicEconomyAPI getAPI() {
        return api;
    }
}
