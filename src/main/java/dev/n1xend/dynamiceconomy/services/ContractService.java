package dev.n1xend.dynamiceconomy.services;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketCategory;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages time-limited bulk supply contracts for the dynamic market.
 *
 * <p>Contracts spawn periodically, require delivering a set amount of an item,
 * and reward all contributors with a bonus sell price for that item.
 * Expired contracts are cleaned up automatically.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class ContractService {

    /**
     * Represents a single active bulk contract.
     *
     * @param id              unique contract identifier
     * @param materialId      target material (e.g. "WHEAT")
     * @param displayName     colored display name of the material
     * @param requiredAmount  total units required to complete the contract
     * @param expiresAt       epoch millis when this contract expires
     * @param bonusMultiplier extra sell price multiplier (e.g. 0.4 = +40%)
     */
    public record Contract(
        String id,
        String materialId,
        String displayName,
        int requiredAmount,
        long expiresAt,
        double bonusMultiplier
    ) {
        /** Returns true if this contract has expired. */
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        /** Returns remaining minutes until expiry, minimum 0. */
        public int getRemainingMinutes() {
            long ms = expiresAt - System.currentTimeMillis();
            return (int) Math.max(0, ms / 60_000);
        }
    }

    // -------------------------------------------------------------------------

    private final DynamicEconomy plugin;
    private final Map<String, Contract> activeContracts = new LinkedHashMap<>();
    private final Map<String, Integer> contractProgress = new ConcurrentHashMap<>();

    public ContractService(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Scheduling
    // -------------------------------------------------------------------------

    /**
     * Starts the contract spawn and expiry tick scheduler.
     * Does nothing if contracts are disabled in config.
     */
    public void startContractTask() {
        if (!plugin.getConfigManager().isContractsEnabled()) {
            return;
        }

        long intervalTicks = 20L * 60 * plugin.getConfigManager().getContractSpawnIntervalMinutes();
        // Initial tick after 10 seconds to let the server settle
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 200L, intervalTicks);
    }

    private void tick() {
        removeExpiredContracts();

        int maxActive = plugin.getConfigManager().getContractsMaxActive();
        if (activeContracts.size() < maxActive) {
            spawnRandomContract();
        }
    }

    private void removeExpiredContracts() {
        Iterator<Map.Entry<String, Contract>> iterator = activeContracts.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                contractProgress.remove(entry.getKey());
                iterator.remove();

                String msg = plugin.getMessageManager().get("contracts.expired",
                    "%item%", entry.getValue().displayName());
                plugin.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(msg));
            }
        }
    }

    private void spawnRandomContract() {
        Map<String, MarketCategory> categories = plugin.getEconomyService().getCategories();
        if (categories.isEmpty()) {
            return;
        }

        // Pick a random item from a random category
        List<MarketCategory> catList = new ArrayList<>(categories.values());
        MarketCategory category = catList.get(new Random().nextInt(catList.size()));
        List<MarketItem> itemList = new ArrayList<>(category.getItems());
        if (itemList.isEmpty()) {
            return;
        }
        MarketItem item = itemList.get(new Random().nextInt(itemList.size()));

        int amountMin = plugin.getConfigManager().getContractAmountMin();
        int amountMax = plugin.getConfigManager().getContractAmountMax();
        int required = amountMin + new Random().nextInt(amountMax - amountMin + 1);

        long durationMs = plugin.getConfigManager().getContractDurationMinutes() * 60_000L;
        double bonus = plugin.getConfigManager().getContractBonusMultiplier();
        String contractId = UUID.randomUUID().toString().substring(0, 8);

        Contract contract = new Contract(
            contractId, item.getId(), item.getDisplayName(),
            required, System.currentTimeMillis() + durationMs, bonus
        );

        activeContracts.put(contractId, contract);
        contractProgress.put(contractId, 0);

        String msg = plugin.getMessageManager().get("contracts.new",
            "%amount%", required,
            "%item%", item.getDisplayName(),
            "%time%", plugin.getConfigManager().getContractDurationMinutes(),
            "%bonus%", (int) (bonus * 100));

        plugin.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        plugin.getLogger().info("New contract: " + required + "x " + item.getId());
    }

    // -------------------------------------------------------------------------
    // Public sell hook
    // -------------------------------------------------------------------------

    /**
     * Called when a player sells items — updates contract progress if applicable.
     *
     * @param player     the selling player
     * @param materialId material that was sold
     * @param amount     quantity sold
     */
    public void onSell(@NotNull Player player, @NotNull String materialId, int amount) {
        for (var entry : activeContracts.entrySet()) {
            Contract contract = entry.getValue();
            if (!contract.materialId().equals(materialId) || contract.isExpired()) {
                continue;
            }

            String contractId = entry.getKey();
            int before = contractProgress.getOrDefault(contractId, 0);
            int after = Math.min(before + amount, contract.requiredAmount());
            contractProgress.put(contractId, after);

            if (after >= contract.requiredAmount()) {
                completeContract(contractId);
                return;
            }
        }
    }

    private void completeContract(@NotNull String contractId) {
        Contract contract = activeContracts.remove(contractId);
        if (contract == null) {
            return;
        }
        contractProgress.remove(contractId);

        String msg = plugin.getMessageManager().get("contracts.completed",
            "%item%", contract.displayName());
        plugin.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        plugin.getLogger().info("Contract completed: " + contract.materialId());
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    /**
     * Returns true if there is at least one active non-expired contract for the material.
     *
     * @param materialId material to check
     * @return whether a contract bonus applies
     */
    public boolean hasActiveContractFor(@NotNull String materialId) {
        return activeContracts.values().stream()
            .anyMatch(c -> c.materialId().equals(materialId) && !c.isExpired());
    }

    @NotNull
    public Collection<Contract> getActiveContracts() {
        return activeContracts.values();
    }

    public int getContractProgress(@NotNull String contractId) {
        return contractProgress.getOrDefault(contractId, 0);
    }
}
