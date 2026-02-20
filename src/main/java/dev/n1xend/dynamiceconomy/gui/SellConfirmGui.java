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
 * Sell confirmation screen (3-row, 27 slots).
 *
 * <pre>
 * [f][f][f][f][f][f][f][f][f]   row 0
 * [f][item][f][OK][f][X][f][f][f]  row 1  (slots 9-17)
 * [f][f][f][f][f][f][f][f][f]   row 2
 * </pre>
 *
 * @author n1xend
 * @version 1.2.1
 */
public final class SellConfirmGui {

    public static final String TITLE_PREFIX = "§6Продажа: ";

    static final int SLOT_PREVIEW = 11;
    static final int SLOT_CONFIRM = 13;
    static final int SLOT_CANCEL  = 15;

    private final DynamicEconomy plugin;

    public SellConfirmGui(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player player, @NotNull String materialId,
                     @NotNull GuiStateStore.SellMode mode) {
        MarketItem item = plugin.getEconomyService().getItem(materialId);
        if (item == null) return;

        int inInv  = countInInventory(player, item.getMaterial());
        if (inInv <= 0) {
            player.sendMessage(plugin.getMessageManager().prefixed("sell.no-items",
                    "%item%", item.getDisplayName()));
            return;
        }

        int maxSell = plugin.getLicenseService().getMaxSellAmount(player);
        int toSell  = switch (mode) {
            case ALL   -> Math.min(inInv, maxSell);
            case STACK -> Math.min(64,   inInv);
            case ONE   -> 1;
        };

        MarketCategory cat = plugin.getEconomyService().getCategory(item.getCategoryId());
        double seasonal    = cat != null ? cat.getSeasonalMultiplier() : 1.0;
        double preview     = plugin.getEconomyService().getPriceCalculator()
                .calculatePreviewPayout(item, toSell, seasonal);
        double multAfter   = plugin.getEconomyService().getPriceCalculator()
                .previewMultiplierAfterSale(item, toSell);

        // Strip colour codes from item name for title (avoids Minecraft title glitches)
        String cleanName   = item.getDisplayName().replaceAll("§.", "");
        String title       = TITLE_PREFIX + toSell + "x " + cleanName;

        Inventory inv = Bukkit.createInventory(
                new GuiHolder(GuiHolder.GuiType.SELL_CONFIRM, materialId), 27, title);
        GUIHelper.fill(inv, GUIHelper.filler(plugin.getConfigManager().getGuiFiller()));

        inv.setItem(SLOT_PREVIEW, buildPreview(item, toSell, preview,
                item.getCurrentMultiplier(), multAfter, seasonal));
        inv.setItem(SLOT_CONFIRM, buildConfirm(toSell, preview, item.getDisplayName()));
        inv.setItem(SLOT_CANCEL,  GUIHelper.item(Material.RED_STAINED_GLASS_PANE,
                plugin.getMessageManager().get("gui.confirm.cancel-button"),
                List.of(GUIHelper.color("&7Вернуться в категорию"))));

        // Store state for listener
        UUID uuid = player.getUniqueId();
        plugin.getGuiStateStore().setSellItem(uuid, materialId);
        plugin.getGuiStateStore().setSellAmount(uuid, toSell);
        plugin.getGuiStateStore().setSellMode(uuid, mode);

        player.openInventory(inv);
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private ItemStack buildPreview(@NotNull MarketItem item, int amount, double payout,
                                    double multBefore, double multAfter, double seasonal) {
        double perUnit   = payout / amount;
        double tax       = plugin.getEconomyService().getPriceCalculator().getSellTaxRate();
        double taxAmount = item.getCurrentPrice() * seasonal * amount * tax;

        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.color("&8▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔"));
        lore.add(GUIHelper.color("&7Продаёте: &f" + amount + "x " + item.getDisplayName()));
        lore.add(GUIHelper.color("&7Цена/шт:  &a" + GUIHelper.formatPrice(perUnit)));
        if (seasonal > 1.0)
            lore.add(GUIHelper.color("&6🔥 Сезонный бонус: +"
                    + (int)((seasonal-1)*100) + "%"));
        lore.add(GUIHelper.color("&7Налог (" + (int)(tax*100) + "%): &c-"
                + GUIHelper.formatPrice(taxAmount)));
        lore.add("");
        lore.add(GUIHelper.color("&7Получите: &a&l" + GUIHelper.formatPrice(payout)));
        lore.add("");
        lore.add(GUIHelper.color("&7Цена до:    " + GUIHelper.priceColor(multBefore)
                + String.format("%.0f%%", multBefore*100)));
        lore.add(GUIHelper.color("&7Цена после: " + GUIHelper.priceColor(multAfter)
                + String.format("%.0f%%", multAfter*100)));

        return GUIHelper.item(item.getMaterial(),
                GUIHelper.color("&f" + item.getDisplayName() + " &7×" + amount), lore);
    }

    private ItemStack buildConfirm(int amount, double payout, String name) {
        List<String> lore = new ArrayList<>();
        lore.add(GUIHelper.color("&7" + amount + "x " + name));
        lore.add(GUIHelper.color("&aПолучите: &6" + GUIHelper.formatPrice(payout)));
        lore.add("");
        lore.add(GUIHelper.color("&eНажмите для подтверждения"));
        return GUIHelper.item(Material.LIME_STAINED_GLASS_PANE,
                plugin.getMessageManager().get("gui.confirm.confirm-button"), lore);
    }

    // ── Static accessors ──────────────────────────────────────────────────────

    public static int getConfirmSlot() { return SLOT_CONFIRM; }
    public static int getCancelSlot()  { return SLOT_CANCEL;  }

    // ── Utility ───────────────────────────────────────────────────────────────

    private int countInInventory(@NotNull Player p, @NotNull Material mat) {
        int n = 0;
        for (var s : p.getInventory().getContents())
            if (s != null && s.getType() == mat) n += s.getAmount();
        return n;
    }
}
