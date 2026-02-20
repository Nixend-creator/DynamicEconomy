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
 * Main market menu — category icons + active contracts.
 *
 * @author n1xend
 * @version 1.2.1
 */
public final class MainMenuGui {

    public static final String TITLE = "§6§lDynamic Market";

    private static final int INFO_SLOT        = 4;
    private static final int[] CONTRACT_SLOTS = {46, 49, 52};

    private final DynamicEconomy plugin;

    public MainMenuGui(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player player) {
        Inventory inv = Bukkit.createInventory(
                new GuiHolder(GuiHolder.GuiType.MAIN_MENU), 54, TITLE);

        GUIHelper.fill(inv, GUIHelper.filler(plugin.getConfigManager().getGuiEmpty()));
        GUIHelper.fillBorder(inv, GUIHelper.filler(plugin.getConfigManager().getGuiFiller()));

        for (MarketCategory cat : plugin.getEconomyService().getCategories().values()) {
            inv.setItem(cat.getGuiSlot(), buildCategoryIcon(cat));
        }

        placeContracts(inv);
        inv.setItem(INFO_SLOT, buildInfoItem());
        player.openInventory(inv);
    }

    private ItemStack buildCategoryIcon(@NotNull MarketCategory cat) {
        boolean hot = cat.getId().equals(plugin.getEconomyService().getHotCategoryId());
        String name = cat.getDisplayName() + (hot ? " §6🔥" : "");

        double avg = cat.getItems().stream()
                .mapToDouble(i -> i.getCurrentMultiplier()).average().orElse(1.0);

        List<String> lore = new ArrayList<>();
        lore.add(cat.getDescription());
        lore.add("");
        lore.add(GUIHelper.color("&7Предметов: &f" + cat.getItems().size()));
        if (hot) lore.add(GUIHelper.color("&6🔥 Горячая! +"
                + (int)((cat.getHotMultiplier()-1)*100) + "%"));
        lore.add("");
        lore.add(GUIHelper.color("&7Средняя цена: " + GUIHelper.priceColor(avg)
                + String.format("%.0f%%", avg * 100)));
        lore.add(GUIHelper.color(GUIHelper.bar(avg,
                plugin.getConfigManager().getMinPriceMultiplier(), 1.0)));
        lore.add("");
        lore.add(GUIHelper.color("&eНажмите для просмотра →"));
        return GUIHelper.item(cat.getIcon(), name, lore);
    }

    private void placeContracts(@NotNull Inventory inv) {
        Collection<ContractService.Contract> contracts =
                plugin.getContractService().getActiveContracts();
        if (contracts.isEmpty()) {
            inv.setItem(CONTRACT_SLOTS[1], GUIHelper.item(
                    plugin.getConfigManager().getGuiEmpty(),
                    plugin.getMessageManager().get("gui.no-contracts")));
            return;
        }
        int i = 0;
        for (ContractService.Contract c : contracts) {
            if (i >= CONTRACT_SLOTS.length) break;
            inv.setItem(CONTRACT_SLOTS[i++], buildContractItem(c));
        }
    }

    private ItemStack buildContractItem(@NotNull ContractService.Contract c) {
        int done    = plugin.getContractService().getContractProgress(c.id());
        int req     = c.requiredAmount();
        int pct     = (int)(((double)done / req) * 100);
        int bonusPct= (int)(c.bonusMultiplier() * 100);
        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.color("&7Доставить: &f" + req + "x " + c.displayName()));
        lore.add(GUIHelper.color("&7Прогресс: &e" + done + "/" + req + " (" + pct + "%)"));
        lore.add(GUIHelper.color("&7Осталось: &e" + c.getRemainingMinutes() + " мин"));
        lore.add("");
        lore.add(GUIHelper.color("&aБонус: &6+" + bonusPct + "% к цене"));
        return GUIHelper.item(org.bukkit.Material.GOLD_INGOT,
                GUIHelper.color("&6📦 Контракт: " + c.displayName()), lore);
    }

    private ItemStack buildInfoItem() {
        String hotId   = plugin.getEconomyService().getHotCategoryId();
        String hotName = hotId != null
                ? plugin.getEconomyService().getCategory(hotId).getDisplayName()
                : "§7Нет";
        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.color("&7Цены падают от продаж, растут со временем."));
        lore.add("");
        lore.add(GUIHelper.color("&6🔥 Горячая: " + hotName));
        lore.add("");
        lore.add(GUIHelper.color("&eЛКМ &7→ продать &8| &eПКМ &7→ купить"));
        return GUIHelper.item(org.bukkit.Material.NETHER_STAR,
                GUIHelper.color("&b&lDynamic Market"), lore);
    }
}
