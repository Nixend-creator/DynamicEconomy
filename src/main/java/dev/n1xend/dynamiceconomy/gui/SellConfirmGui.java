package dev.n1xend.dynamiceconomy.gui;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketCategory;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import dev.n1xend.dynamiceconomy.utils.GUIHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sell confirmation dialog — shows quantity, price preview, and market impact.
 *
 * <p>Layout (3-row, 27 slots):
 * <pre>
 * [filler] [filler] [filler] [filler] [filler] [filler] [filler] [filler] [filler]
 * [filler] [item]   [filler] [filler] [confirm] [filler] [cancel] [filler] [filler]
 * [filler] [filler] [filler] [filler] [filler]  [filler] [filler] [filler] [filler]
 * </pre></p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class SellConfirmGui {

    public static final String TITLE_PREFIX = "§6Продажа: ";

    private static final int SLOT_ITEM_PREVIEW = 11;
    private static final int SLOT_CONFIRM = 13;
    private static final int SLOT_CANCEL = 15;

    private final DynamicEconomy plugin;

    public SellConfirmGui(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the sell confirmation screen for the given item and sell mode.
     *
     * <p>Automatically calculates how many items will be sold based on mode
     * and what the player has in their inventory.</p>
     *
     * @param player     the selling player
     * @param materialId material to sell
     * @param mode       sell mode (ALL / STACK / ONE)
     */
    public void open(@NotNull Player player, @NotNull String materialId, @NotNull GuiStateStore.SellMode mode) {
        MarketItem item = plugin.getEconomyService().getItem(materialId);
        if (item == null) {
            return;
        }

        int inInventory = countInInventory(player, item.getMaterial());
        if (inInventory <= 0) {
            player.sendMessage(plugin.getMessageManager().prefixed("sell.no-items",
                "%item%", item.getDisplayName()));
            return;
        }

        int toSell = switch (mode) {
            case ALL -> inInventory;
            case STACK -> Math.min(64, inInventory);
            case ONE -> 1;
        };
        toSell = Math.min(toSell, plugin.getConfigManager().getMaxSellAmount());

        MarketCategory category = plugin.getEconomyService().getCategory(item.getCategoryId());
        double seasonalMult = (category != null) ? category.getSeasonalMultiplier() : 1.0;

        double previewPayout = plugin.getEconomyService().getPriceCalculator()
            .calculatePreviewPayout(item, toSell, seasonalMult);
        double multAfter = plugin.getEconomyService().getPriceCalculator()
            .previewMultiplierAfterSale(item, toSell);

        String title = TITLE_PREFIX + toSell + "x " + item.getDisplayName();
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        ItemStack filler = GUIHelper.buildFiller(plugin.getConfigManager().getGuiFiller());
        GUIHelper.fill(inventory, filler);

        inventory.setItem(SLOT_ITEM_PREVIEW,
            buildPreviewItem(item, toSell, previewPayout, item.getCurrentMultiplier(), multAfter, seasonalMult));
        inventory.setItem(SLOT_CONFIRM, buildConfirmButton(toSell, previewPayout, item.getDisplayName()));
        inventory.setItem(SLOT_CANCEL, GUIHelper.buildItem(Material.RED_STAINED_GLASS_PANE,
            plugin.getMessageManager().get("gui.confirm.cancel-button"),
            List.of(GUIHelper.colorize("&7Вернуться в категорию"))));

        // Store pending sell data
        UUID uuid = player.getUniqueId();
        GuiStateStore.setSellItem(uuid, materialId);
        GuiStateStore.setSellAmount(uuid, toSell);
        GuiStateStore.setSellMode(uuid, mode);

        player.openInventory(inventory);
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private ItemStack buildPreviewItem(@NotNull MarketItem item, int amount, double payout,
                                        double multBefore, double multAfter, double seasonalMult) {
        double perUnit = payout / amount;
        double taxRate = plugin.getEconomyService().getPriceCalculator().getSellTaxRate();
        double taxAmount = item.getCurrentPrice() * seasonalMult * amount * taxRate;

        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.colorize("&8▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔"));
        lore.add(plugin.getMessageManager().get("gui.confirm.selling",
            "%amount%", amount, "%item%", item.getDisplayName()));
        lore.add(plugin.getMessageManager().get("gui.confirm.per-unit",
            "%price%", GUIHelper.formatPrice(perUnit)));

        if (seasonalMult > 1.0) {
            int bonusPct = (int) ((seasonalMult - 1.0) * 100);
            lore.add(GUIHelper.colorize("&6🔥 Сезонный бонус: &e+" + bonusPct + "%"));
        }

        lore.add(plugin.getMessageManager().get("gui.confirm.tax",
            "%tax%", (int) (taxRate * 100), "%amount%", GUIHelper.formatPrice(taxAmount)));
        lore.add("");
        lore.add(plugin.getMessageManager().get("gui.confirm.payout",
            "%payout%", GUIHelper.formatPrice(payout)));
        lore.add("");
        lore.add(plugin.getMessageManager().get("gui.confirm.impact-before",
            "%color%", GUIHelper.colorize(GUIHelper.priceColor(multBefore)),
            "%percent%", String.format("%.0f%%", multBefore * 100)));
        lore.add(plugin.getMessageManager().get("gui.confirm.impact-after",
            "%color%", GUIHelper.colorize(GUIHelper.priceColor(multAfter)),
            "%percent%", String.format("%.0f%%", multAfter * 100)));

        return GUIHelper.buildItem(item.getMaterial(),
            GUIHelper.colorize("&f" + item.getDisplayName() + " &7×" + amount), lore);
    }

    private ItemStack buildConfirmButton(int amount, double payout, String itemName) {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessageManager().get("gui.confirm.selling",
            "%amount%", amount, "%item%", itemName));
        lore.add(GUIHelper.colorize("&aПолучите: &6" + GUIHelper.formatPrice(payout)));
        lore.add("");
        lore.add(GUIHelper.colorize("&eНажмите для подтверждения"));

        return GUIHelper.buildItem(Material.LIME_STAINED_GLASS_PANE,
            plugin.getMessageManager().get("gui.confirm.confirm-button"), lore);
    }

    // -------------------------------------------------------------------------
    // Slot constants for listener
    // -------------------------------------------------------------------------

    public static int getConfirmSlot() {
        return SLOT_CONFIRM;
    }

    public static int getCancelSlot() {
        return SLOT_CANCEL;
    }

    // -------------------------------------------------------------------------
    // Private util
    // -------------------------------------------------------------------------

    private int countInInventory(@NotNull Player player, @NotNull Material material) {
        int count = 0;
        for (var stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }
}
