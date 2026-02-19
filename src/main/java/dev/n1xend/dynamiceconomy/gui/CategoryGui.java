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
 * Category item listing GUI — shows all items in a category with price info.
 *
 * <p>Supports pagination with 28 items per page (4 rows × 7 columns).
 * Left border and right border are reserved for GUI chrome.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class CategoryGui {

    /** Available item display slots (inner 7 columns, rows 1-4). */
    private static final int[] ITEM_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private static final int ITEMS_PER_PAGE = ITEM_SLOTS.length;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_HEADER = 4;

    private final DynamicEconomy plugin;

    public CategoryGui(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the category GUI at the given page for a player.
     *
     * @param player     the player
     * @param categoryId category identifier
     * @param page       page index (0-based)
     */
    public void open(@NotNull Player player, @NotNull String categoryId, int page) {
        MarketCategory category = plugin.getEconomyService().getCategory(categoryId);
        if (category == null) {
            return;
        }

        boolean isHot = categoryId.equals(plugin.getEconomyService().getHotCategoryId());
        String title = category.getDisplayName() + (isHot ? " §6🔥" : "");

        Inventory inventory = Bukkit.createInventory(null, 54, title);

        ItemStack filler = GUIHelper.buildFiller(plugin.getConfigManager().getGuiFiller());
        ItemStack empty = GUIHelper.buildFiller(plugin.getConfigManager().getGuiEmpty());
        GUIHelper.fill(inventory, empty);
        GUIHelper.fillBorder(inventory, filler);

        List<MarketItem> items = new ArrayList<>(category.getItems());
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());

        for (int i = start; i < end; i++) {
            int slot = ITEM_SLOTS[i - start];
            inventory.setItem(slot, buildItemDisplay(items.get(i), category));
        }

        // Navigation buttons
        if (page > 0) {
            inventory.setItem(SLOT_PREV, GUIHelper.buildItem(Material.ARROW,
                plugin.getMessageManager().get("gui.prev-page"),
                List.of(GUIHelper.colorize("&7Страница " + page + "/" + totalPages))));
        }
        if (page < totalPages - 1) {
            inventory.setItem(SLOT_NEXT, GUIHelper.buildItem(Material.ARROW,
                plugin.getMessageManager().get("gui.next-page"),
                List.of(GUIHelper.colorize("&7Страница " + (page + 2) + "/" + totalPages))));
        }

        inventory.setItem(SLOT_BACK, GUIHelper.buildItem(Material.BARRIER,
            plugin.getMessageManager().get("gui.back-button")));
        inventory.setItem(SLOT_HEADER, buildCategoryHeader(category, isHot));

        // Save navigation state
        UUID uuid = player.getUniqueId();
        GuiStateStore.setCategory(uuid, categoryId);
        GuiStateStore.setPage(uuid, page);

        player.openInventory(inventory);
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private ItemStack buildItemDisplay(@NotNull MarketItem item, @NotNull MarketCategory category) {
        double multiplier = item.getCurrentMultiplier();
        double seasonalMult = category.getSeasonalMultiplier();
        double taxRate = plugin.getEconomyService().getPriceCalculator().getSellTaxRate();
        double netPricePerUnit = item.getCurrentPrice() * seasonalMult * (1.0 - taxRate);

        boolean isHot = category.isHotCategory();
        boolean hasContract = plugin.getContractService().hasActiveContractFor(item.getId());

        String namePrefix = (hasContract ? "§a📦 " : "") + (isHot ? "§6🔥 " : "");

        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.colorize("&8▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔"));
        lore.add(plugin.getMessageManager().get("gui.item-lore.base-price",
            "%price%", GUIHelper.formatPrice(item.getBasePrice())));
        lore.add(plugin.getMessageManager().get("gui.item-lore.current-price",
            "%color%", GUIHelper.colorize(GUIHelper.priceColor(multiplier)),
            "%price%", GUIHelper.formatPrice(netPricePerUnit)));
        lore.add("");
        lore.add(plugin.getMessageManager().get("gui.item-lore.price-level"));
        lore.add(GUIHelper.colorize(
            GUIHelper.multiplierBar(multiplier, plugin.getConfigManager().getMinPriceMultiplier(), 1.0)
            + " " + GUIHelper.priceColor(multiplier) + String.format("%.0f%%", multiplier * 100)
        ));
        lore.add("");
        lore.add(plugin.getMessageManager().get("gui.item-lore.trend",
            "%arrow%", GUIHelper.colorize(GUIHelper.trendArrow(multiplier)),
            "%color%", GUIHelper.colorize(GUIHelper.priceColor(multiplier)),
            "%percent%", String.format("%+.1f%%", (multiplier - 1.0) * 100)));
        lore.add(plugin.getMessageManager().get("gui.item-lore.total-sold",
            "%amount%", item.getTotalSold()));

        if (isHot) {
            int bonusPct = (int) ((category.getHotMultiplier() - 1.0) * 100);
            lore.add(plugin.getMessageManager().get("gui.item-lore.hot-bonus", "%bonus%", bonusPct));
        }
        if (hasContract) {
            int bonusPct = (int) (plugin.getConfigManager().getContractBonusMultiplier() * 100);
            lore.add(plugin.getMessageManager().get("gui.item-lore.contract-bonus", "%bonus%", bonusPct));
        }

        lore.add("");
        lore.add(plugin.getMessageManager().get("gui.click-hint"));

        return GUIHelper.buildItem(item.getMaterial(), namePrefix + item.getDisplayName(), lore);
    }

    private ItemStack buildCategoryHeader(@NotNull MarketCategory category, boolean isHot) {
        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.colorize("&7Предметов: &f" + category.getItems().size()));
        if (isHot) {
            int bonusPct = (int) ((category.getHotMultiplier() - 1.0) * 100);
            lore.add(GUIHelper.colorize("&6🔥 Горячая! Бонус: &e+" + bonusPct + "% к цене"));
        }
        lore.add("");
        lore.add(GUIHelper.colorize("&7Цены указаны за 1 шт. после налога "
            + (int) (plugin.getConfigManager().getSellTax() * 100) + "%."));

        return GUIHelper.buildItem(category.getIcon(), category.getDisplayName(), lore);
    }

    // -------------------------------------------------------------------------
    // Slot check utility
    // -------------------------------------------------------------------------

    /**
     * Returns true if the given slot is a valid item display slot in the category GUI.
     *
     * @param slot inventory slot index
     * @return whether this slot contains an item
     */
    public static boolean isItemSlot(int slot) {
        for (int itemSlot : ITEM_SLOTS) {
            if (itemSlot == slot) {
                return true;
            }
        }
        return false;
    }
}
