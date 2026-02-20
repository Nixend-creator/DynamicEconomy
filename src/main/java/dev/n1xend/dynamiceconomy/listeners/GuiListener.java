package dev.n1xend.dynamiceconomy.listeners;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.auction.AuctionService;
import dev.n1xend.dynamiceconomy.data.models.AuctionListing;
import dev.n1xend.dynamiceconomy.data.models.MarketCategory;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import dev.n1xend.dynamiceconomy.gui.*;
import dev.n1xend.dynamiceconomy.services.BuyService;
import dev.n1xend.dynamiceconomy.services.EconomyService;
import dev.n1xend.dynamiceconomy.utils.GUIHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Handles all GUI inventory events for DynamicEconomy.
 *
 * <p><b>Critical Paper 1.21 note:</b> always use
 * {@code event.getView().getTopInventory()} to get the plugin-owned
 * inventory. {@code event.getInventory()} returns whatever inventory
 * the click physically landed in — this can be the player's own
 * inventory (bottom half), whose holder is a {@code HumanEntity},
 * not our {@link GuiHolder}, causing all routing to silently fail.</p>
 *
 * @author n1xend
 * @version 1.2.2
 */
public final class GuiListener implements Listener {

    private final DynamicEconomy plugin;
    private final MainMenuGui    mainMenuGui;
    private final CategoryGui    categoryGui;
    private final SellConfirmGui sellConfirmGui;
    private final BuyConfirmGui  buyConfirmGui;
    private final AuctionGui     auctionGui;

    public GuiListener(@NotNull DynamicEconomy plugin) {
        this.plugin         = plugin;
        this.mainMenuGui    = new MainMenuGui(plugin);
        this.categoryGui    = new CategoryGui(plugin);
        this.sellConfirmGui = new SellConfirmGui(plugin);
        this.buyConfirmGui  = new BuyConfirmGui(plugin);
        this.auctionGui     = new AuctionGui(plugin);
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(@NotNull InventoryDragEvent event) {
        if (holderOf(event.getView().getTopInventory()) != null) {
            event.setCancelled(true);
        }
    }

    // ── Click ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // IMPORTANT: read holder from TOP inventory, not event.getInventory()
        Inventory topInv = event.getView().getTopInventory();
        GuiHolder holder = holderOf(topInv);
        if (holder == null) return;

        event.setCancelled(true);

        // Ignore clicks in the player's own inventory (bottom half)
        if (event.getRawSlot() >= topInv.getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        int       slot  = event.getRawSlot();
        ClickType click = event.getClick();

        switch (holder.getType()) {
            case MAIN_MENU    -> handleMainMenu(player, slot);
            case CATEGORY     -> handleCategory(player, holder.getMeta(), slot, clicked, click);
            case SELL_CONFIRM -> handleSellConfirm(player, holder.getMeta(), slot);
            case BUY_CONFIRM  -> handleBuyConfirm(player, holder.getMeta(), slot);
            case AUCTION      -> handleAuction(player, holder.getMeta(), slot);
        }
    }

    // ── Close ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        GuiHolder holder = holderOf(event.getView().getTopInventory());
        if (holder != null && holder.getType() == GuiHolder.GuiType.MAIN_MENU) {
            plugin.getGuiStateStore().cleanup(player.getUniqueId());
        }
    }

    // ── Main Menu ─────────────────────────────────────────────────────────────

    private void handleMainMenu(@NotNull Player player, int slot) {
        for (MarketCategory cat : plugin.getEconomyService().getCategories().values()) {
            if (cat.getGuiSlot() == slot) {
                categoryGui.open(player, cat.getId(), 0);
                return;
            }
        }
    }

    // ── Category ──────────────────────────────────────────────────────────────

    private void handleCategory(@NotNull Player player, @NotNull String catId,
                                  int slot, @NotNull ItemStack clicked,
                                  @NotNull ClickType click) {
        UUID          uuid  = player.getUniqueId();
        GuiStateStore store = plugin.getGuiStateStore();
        int           pg    = store.getPage(uuid);

        if (slot == CategoryGui.SLOT_BACK)   { mainMenuGui.open(player); return; }
        if (slot == CategoryGui.SLOT_PREV)   { categoryGui.open(player, catId, Math.max(0, pg - 1)); return; }
        if (slot == CategoryGui.SLOT_NEXT)   { categoryGui.open(player, catId, pg + 1); return; }
        if (slot == CategoryGui.SLOT_HEADER) return;
        if (!CategoryGui.isItemSlot(slot))   return;

        MarketItem item = plugin.getEconomyService().getItemByMaterial(clicked.getType());
        if (item == null) return;

        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            if (plugin.getConfig().getBoolean("buy-mode.enabled", true)) {
                buyConfirmGui.open(player, item.getId());
            }
        } else {
            GuiStateStore.SellMode mode = (click == ClickType.SHIFT_LEFT)
                    ? GuiStateStore.SellMode.ONE
                    : GuiStateStore.SellMode.ALL;
            sellConfirmGui.open(player, item.getId(), mode);
        }
    }

    // ── Sell Confirm ──────────────────────────────────────────────────────────

    private void handleSellConfirm(@NotNull Player player, @NotNull String materialId, int slot) {
        UUID          uuid  = player.getUniqueId();
        GuiStateStore store = plugin.getGuiStateStore();

        if (slot == SellConfirmGui.getCancelSlot()) {
            String catId = store.getCategory(uuid);
            if (catId != null) categoryGui.open(player, catId, store.getPage(uuid));
            else               mainMenuGui.open(player);
            return;
        }

        if (slot == SellConfirmGui.getConfirmSlot()) {
            String resolvedId = store.getSellItem(uuid);
            if (resolvedId == null) resolvedId = materialId;
            int amount = store.getSellAmount(uuid);

            EconomyService.SellData result =
                    plugin.getEconomyService().trySell(player, resolvedId, amount);

            if (result.result() == EconomyService.SellResult.SUCCESS) {
                plugin.getLicenseService().recordSell(player, amount, result.payout());
                plugin.getTreasuryService().collectTax(
                        result.payout() * plugin.getLicenseService().getTaxRate(player));
            }

            sendSellFeedback(player, result, amount);

            String fCat  = store.getCategory(uuid);
            int    fPage = store.getPage(uuid);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (fCat != null) categoryGui.open(player, fCat, fPage);
                else              mainMenuGui.open(player);
            }, 1L);
        }
    }

    // ── Buy Confirm ───────────────────────────────────────────────────────────

    private void handleBuyConfirm(@NotNull Player player, @NotNull String materialId, int slot) {
        UUID          uuid  = player.getUniqueId();
        GuiStateStore store = plugin.getGuiStateStore();

        if (slot == BuyConfirmGui.getBackSlot()) {
            String catId = store.getCategory(uuid);
            if (catId != null) categoryGui.open(player, catId, store.getPage(uuid));
            else               mainMenuGui.open(player);
            return;
        }

        int amount = 0;
        if (slot == BuyConfirmGui.getBuy1Slot())  amount = 1;
        if (slot == BuyConfirmGui.getBuy8Slot())  amount = 8;
        if (slot == BuyConfirmGui.getBuy64Slot()) amount = 64;
        if (amount == 0) return;

        BuyService.BuyData result = plugin.getBuyService().tryBuy(player, materialId, amount);
        sendBuyFeedback(player, result);

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> buyConfirmGui.open(player, materialId), 1L);
    }

    // ── Auction ───────────────────────────────────────────────────────────────

    private void handleAuction(@NotNull Player player, @NotNull String meta, int slot) {
        UUID          uuid  = player.getUniqueId();
        GuiStateStore store = plugin.getGuiStateStore();

        // meta format: "MODE:page"  e.g. "ALL:0" or "MY:2"
        String[] parts = meta.split(":", 2);
        String   mode  = parts.length > 0 ? parts[0] : "ALL";
        int      pg    = store.getAuctionPage(uuid);

        if (slot == AuctionGui.SLOT_PREV) {
            if (mode.equals("MY")) auctionGui.openMyLots(player, Math.max(0, pg - 1));
            else                   auctionGui.open(player, Math.max(0, pg - 1));
            return;
        }
        if (slot == AuctionGui.SLOT_NEXT) {
            if (mode.equals("MY")) auctionGui.openMyLots(player, pg + 1);
            else                   auctionGui.open(player, pg + 1);
            return;
        }
        if (slot == AuctionGui.SLOT_MY_LOTS) {
            if (mode.equals("MY")) auctionGui.open(player, 0);
            else                   auctionGui.openMyLots(player, 0);
            return;
        }
        if (slot == AuctionGui.SLOT_INFO || slot == AuctionGui.SLOT_HEADER) return;

        if (!AuctionGui.isItemSlot(slot)) return;

        // Find which listing this slot corresponds to
        java.util.List<AuctionListing> listings = mode.equals("MY")
                ? plugin.getAuctionService().getActiveListings().stream()
                        .filter(l -> l.getSellerUuid().equals(uuid)).toList()
                : plugin.getAuctionService().getActiveListings();

        int idx = pg * AuctionGui.ITEMS_PER_PAGE + slotToIndex(slot);
        if (idx < 0 || idx >= listings.size()) return;

        AuctionListing listing = listings.get(idx);
        boolean own = listing.getSellerUuid().equals(uuid);

        if (own) {
            // Cancel own listing
            boolean ok = plugin.getAuctionService().cancelListing(player, listing.getId());
            if (!ok) player.sendMessage("§cНе удалось отменить лот.");
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> { if (mode.equals("MY")) auctionGui.openMyLots(player, pg);
                            else                   auctionGui.open(player, pg); }, 1L);
        } else {
            // Buy listing
            AuctionService.BuyResult result =
                    plugin.getAuctionService().buyListing(player, listing.getId());
            switch (result) {
                case NOT_FOUND          -> player.sendMessage("§cЛот уже продан или истёк.");
                case OWN_LISTING        -> player.sendMessage("§cВы не можете купить собственный лот.");
                case INSUFFICIENT_FUNDS -> player.sendMessage("§cНедостаточно монет для покупки.");
                case SUCCESS            -> {} // message in AuctionService
            }
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> auctionGui.open(player, pg), 1L);
        }
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    private void sendSellFeedback(@NotNull Player p,
                                   @NotNull EconomyService.SellData d, int amount) {
        switch (d.result()) {
            case SUCCESS -> {
                p.sendMessage("§a✓ Продано §e" + amount + "x " + d.itemDisplayName()
                        + " §aза §6" + GUIHelper.formatPrice(d.payout()));
                p.sendMessage("§7Цена теперь: §e"
                        + String.format("%.0f%%", d.multiplierAfter() * 100));
                if (d.hadDiversityBonus()) p.sendMessage("§a+ Бонус разнообразия!");
                if (d.hadContractBonus())  p.sendMessage("§a+ Бонус контракта!");
            }
            case COOLDOWN         -> p.sendMessage("§cПодождите перед следующей продажей.");
            case NOT_ENOUGH_ITEMS -> p.sendMessage("§cНедостаточно §e" + d.itemDisplayName());
            case ITEM_NOT_SOLD    -> p.sendMessage("§cЭтот предмет не принимается.");
        }
    }

    private void sendBuyFeedback(@NotNull Player p, @NotNull BuyService.BuyData d) {
        switch (d.result()) {
            case SUCCESS            -> p.sendMessage("§a✓ Куплено §e" + d.itemDisplayName()
                    + " §aза §6" + GUIHelper.formatPrice(d.totalCost()));
            case INSUFFICIENT_FUNDS -> p.sendMessage("§cНедостаточно монет. Нужно: §e"
                    + GUIHelper.formatPrice(d.totalCost()));
            case INVENTORY_FULL     -> p.sendMessage("§cНет места в инвентаре.");
            case ITEM_NOT_FOUND     -> p.sendMessage("§cПредмет не найден.");
            case BUY_MODE_DISABLED  -> p.sendMessage("§cРежим покупки отключён.");
        }
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    /** Converts a raw slot index to a 0-based listing index within the current page. */
    private int slotToIndex(int slot) {
        for (int i = 0; i < AuctionGui.ITEM_SLOTS.length; i++) {
            if (AuctionGui.ITEM_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    @Nullable
    private GuiHolder holderOf(@Nullable Inventory inv) {
        if (inv == null) return null;
        InventoryHolder h = inv.getHolder();
        return (h instanceof GuiHolder gh) ? gh : null;
    }
}
