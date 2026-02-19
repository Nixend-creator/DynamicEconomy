package dev.n1xend.dynamiceconomy.gui;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketCategory;
import dev.n1xend.dynamiceconomy.services.ContractService;
import dev.n1xend.dynamiceconomy.utils.GUIHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Main market menu GUI — shows all categories and active contracts.
 *
 * <p>Categories are placed at their configured slots. Contracts board
 * occupies the bottom area of the 6-row inventory.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class MainMenuGui {

    public static final String TITLE = "§6§lDynamic Market";

    private static final int INFO_SLOT = 4;
    private static final int[] CONTRACT_SLOTS = {46, 49, 52};
    private static final int NO_CONTRACTS_SLOT = 49;

    private final DynamicEconomy plugin;

    public MainMenuGui(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the main market menu for a player.
     *
     * @param player the player to open the menu for
     */
    public void open(@NotNull Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);

        ItemStack filler = GUIHelper.buildFiller(plugin.getConfigManager().getGuiFiller());
        ItemStack empty = GUIHelper.buildFiller(plugin.getConfigManager().getGuiEmpty());

        GUIHelper.fill(inventory, empty);
        GUIHelper.fillBorder(inventory, filler);

        placeCategoryIcons(inventory);
        placeContractsArea(inventory);
        inventory.setItem(INFO_SLOT, buildInfoItem());

        player.openInventory(inventory);
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private void placeCategoryIcons(@NotNull Inventory inventory) {
        for (MarketCategory category : plugin.getEconomyService().getCategories().values()) {
            inventory.setItem(category.getGuiSlot(), buildCategoryItem(category));
        }
    }

    private ItemStack buildCategoryItem(@NotNull MarketCategory category) {
        boolean isHot = category.getId().equals(plugin.getEconomyService().getHotCategoryId());
        String name = category.getDisplayName() + (isHot ? " §6🔥" : "");

        List<String> lore = new ArrayList<>();
        lore.add(category.getDescription());
        lore.add("");
        lore.add(GUIHelper.colorize("&7Предметов: &f" + category.getItems().size()));

        if (isHot) {
            int bonusPct = (int) ((category.getHotMultiplier() - 1.0) * 100);
            lore.add(GUIHelper.colorize("&6🔥 Горячая! Бонус: &e+" + bonusPct + "%"));
        }

        // Average price level indicator
        double avgMult = category.getItems().stream()
            .mapToDouble(i -> i.getCurrentMultiplier())
            .average()
            .orElse(1.0);

        lore.add("");
        lore.add(GUIHelper.colorize("&7Средняя цена: " + GUIHelper.priceColor(avgMult)
            + String.format("%.0f%%", avgMult * 100) + " от базовой"));
        lore.add(GUIHelper.colorize(GUIHelper.multiplierBar(avgMult,
            plugin.getConfigManager().getMinPriceMultiplier(), 1.0)));
        lore.add("");
        lore.add(GUIHelper.colorize("&eНажмите для просмотра →"));

        return GUIHelper.buildItem(category.getIcon(), name, lore);
    }

    private void placeContractsArea(@NotNull Inventory inventory) {
        Collection<ContractService.Contract> contracts = plugin.getContractService().getActiveContracts();

        if (contracts.isEmpty()) {
            ItemStack noContracts = GUIHelper.buildItem(
                plugin.getConfigManager().getGuiEmpty(),
                plugin.getMessageManager().get("gui.no-contracts")
            );
            inventory.setItem(NO_CONTRACTS_SLOT, noContracts);
            return;
        }

        int slotIndex = 0;
        for (ContractService.Contract contract : contracts) {
            if (slotIndex >= CONTRACT_SLOTS.length) {
                break;
            }
            inventory.setItem(CONTRACT_SLOTS[slotIndex], buildContractItem(contract));
            slotIndex++;
        }
    }

    private ItemStack buildContractItem(@NotNull ContractService.Contract contract) {
        int progress = plugin.getContractService().getContractProgress(contract.id());
        int required = contract.requiredAmount();
        int progressPct = (int) (((double) progress / required) * 100);
        int bonusPct = (int) (contract.bonusMultiplier() * 100);

        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessageManager().get("contracts.lore.deliver",
            "%amount%", required, "%item%", contract.displayName()));
        lore.add(plugin.getMessageManager().get("contracts.lore.progress",
            "%done%", progress, "%total%", required, "%percent%", progressPct));
        lore.add(plugin.getMessageManager().get("contracts.lore.time-left",
            "%minutes%", contract.getRemainingMinutes()));
        lore.add("");
        lore.add(plugin.getMessageManager().get("contracts.lore.bonus",
            "%bonus%", bonusPct));
        lore.add(plugin.getMessageManager().get("contracts.lore.hint"));

        return GUIHelper.buildItem(
            org.bukkit.Material.GOLD_INGOT,
            GUIHelper.colorize("&6📦 Контракт: " + contract.displayName()),
            lore
        );
    }

    private ItemStack buildInfoItem() {
        String hotCategoryId = plugin.getEconomyService().getHotCategoryId();
        String hotName = hotCategoryId != null
            ? plugin.getEconomyService().getCategory(hotCategoryId).getDisplayName()
            : GUIHelper.colorize("&7Нет");

        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.colorize("&7Цены меняются от спроса и предложения."));
        lore.add(GUIHelper.colorize("&7Чем больше продают — тем ниже цена."));
        lore.add(GUIHelper.colorize("&7Цены восстанавливаются со временем."));
        lore.add("");
        lore.add(GUIHelper.colorize("&6🔥 Горячая категория: " + hotName));
        lore.add("");
        lore.add(GUIHelper.colorize("&7Продавайте разные категории для бонуса!"));
        lore.add(GUIHelper.colorize("&7Выполняйте контракты для дополнительных наград!"));

        return GUIHelper.buildItem(
            org.bukkit.Material.NETHER_STAR,
            GUIHelper.colorize("&b&lDynamic Market"),
            lore
        );
    }
}
