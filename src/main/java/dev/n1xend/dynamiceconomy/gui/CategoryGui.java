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
 * Category listing GUI — paginated items with sell/buy price info.
 *
 * @author n1xend
 * @version 1.2.1
 */
public final class CategoryGui {

    // Inner content slots: rows 1-4, columns 1-7 (border excluded)
    public static final int[] ITEM_SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34,
        37,38,39,40,41,42,43
    };
    public static final int ITEMS_PER_PAGE = ITEM_SLOTS.length; // 28

    public static final int SLOT_BACK   = 49;
    public static final int SLOT_PREV   = 45;
    public static final int SLOT_NEXT   = 53;
    public static final int SLOT_HEADER = 4;

    private final DynamicEconomy plugin;

    public CategoryGui(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player player, @NotNull String categoryId, int page) {
        MarketCategory cat = plugin.getEconomyService().getCategory(categoryId);
        if (cat == null) return;

        boolean hot  = categoryId.equals(plugin.getEconomyService().getHotCategoryId());
        String title = cat.getDisplayName() + (hot ? " §6🔥" : "");

        // GuiHolder carries categoryId — listener reads it instead of parsing title
        Inventory inv = Bukkit.createInventory(
                new GuiHolder(GuiHolder.GuiType.CATEGORY, categoryId), 54, title);

        GUIHelper.fill(inv, GUIHelper.filler(plugin.getConfigManager().getGuiEmpty()));
        GUIHelper.fillBorder(inv, GUIHelper.filler(plugin.getConfigManager().getGuiFiller()));

        List<MarketItem> items = new ArrayList<>(cat.getItems());
        int totalPages = Math.max(1, (int)Math.ceil((double)items.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * ITEMS_PER_PAGE;
        for (int i = start; i < Math.min(start + ITEMS_PER_PAGE, items.size()); i++) {
            inv.setItem(ITEM_SLOTS[i - start], buildItemSlot(items.get(i), cat));
        }

        // Nav
        if (page > 0) {
            inv.setItem(SLOT_PREV, GUIHelper.item(Material.ARROW,
                    plugin.getMessageManager().get("gui.prev-page"),
                    List.of(GUIHelper.color("&7Страница " + page + "/" + totalPages))));
        }
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, GUIHelper.item(Material.ARROW,
                    plugin.getMessageManager().get("gui.next-page"),
                    List.of(GUIHelper.color("&7Страница " + (page+2) + "/" + totalPages))));
        }
        inv.setItem(SLOT_BACK, GUIHelper.item(Material.BARRIER,
                plugin.getMessageManager().get("gui.back-button")));
        inv.setItem(SLOT_HEADER, buildHeader(cat, hot));

        // Save state
        UUID uuid = player.getUniqueId();
        plugin.getGuiStateStore().setCategory(uuid, categoryId);
        plugin.getGuiStateStore().setPage(uuid, page);

        player.openInventory(inv);
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private ItemStack buildItemSlot(@NotNull MarketItem item, @NotNull MarketCategory cat) {
        double mult      = item.getCurrentMultiplier();
        double seasonal  = cat.getSeasonalMultiplier();
        double tax       = plugin.getEconomyService().getPriceCalculator().getSellTaxRate();
        double sellPrice = item.getCurrentPrice() * seasonal * (1.0 - tax);
        double buyPrice  = item.getCurrentPrice()
                * plugin.getConfig().getDouble("buy-mode.spread-multiplier", 1.3);
        boolean hot      = cat.isHotCategory();
        boolean contract = plugin.getContractService().hasActiveContractFor(item.getId());

        String prefix = (contract ? "§a📦 " : "") + (hot ? "§6🔥 " : "");

        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.color("&8▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔"));
        lore.add(GUIHelper.color("&7Продать (ЛКМ): &a" + GUIHelper.formatPrice(sellPrice)));
        if (plugin.getConfig().getBoolean("buy-mode.enabled", true)) {
            lore.add(GUIHelper.color("&7Купить  (ПКМ): &e" + GUIHelper.formatPrice(buyPrice)));
        }
        lore.add("");
        lore.add(GUIHelper.color("&7Уровень цены:"));
        lore.add(GUIHelper.color(
                GUIHelper.bar(mult, plugin.getConfigManager().getMinPriceMultiplier(), 1.0)
                + " " + GUIHelper.priceColor(mult)
                + String.format("%.0f%%", mult * 100)));
        lore.add(GUIHelper.color("&7Тренд: " + GUIHelper.trendArrow(mult)
                + " " + GUIHelper.priceColor(mult)
                + String.format("%+.1f%%", (mult - 1.0) * 100)));
        lore.add(GUIHelper.color("&7Продано всего: &f" + item.getTotalSold()));
        if (hot) {
            lore.add(GUIHelper.color("&6🔥 Горячий! +"
                    + (int)((cat.getHotMultiplier()-1)*100) + "% бонус"));
        }
        if (contract) {
            lore.add(GUIHelper.color("&a📦 Контракт! +"
                    + (int)(plugin.getConfigManager().getContractBonusMultiplier()*100) + "% бонус"));
        }
        lore.add("");
        lore.add(GUIHelper.color("&eЛКМ &7→ продать всё  &8|  &eПКМ &7→ купить"));
        lore.add(GUIHelper.color("&eShift+ЛКМ &7→ продать 1 шт"));

        return GUIHelper.item(item.getMaterial(), prefix + item.getDisplayName(), lore);
    }

    private ItemStack buildHeader(@NotNull MarketCategory cat, boolean hot) {
        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.color("&7Предметов: &f" + cat.getItems().size()));
        if (hot) lore.add(GUIHelper.color("&6🔥 Горячая! +"
                + (int)((cat.getHotMultiplier()-1)*100) + "%"));
        lore.add("");
        lore.add(GUIHelper.color("&7Налог с продажи: &c"
                + (int)(plugin.getConfigManager().getSellTax()*100) + "%"));
        return GUIHelper.item(cat.getIcon(), cat.getDisplayName(), lore);
    }

    /** Returns true if slot is a valid item display slot. */
    public static boolean isItemSlot(int slot) {
        for (int s : ITEM_SLOTS) if (s == slot) return true;
        return false;
    }
}
