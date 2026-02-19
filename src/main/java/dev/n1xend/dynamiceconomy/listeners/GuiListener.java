package dev.n1xend.dynamiceconomy.listeners;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketCategory;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import dev.n1xend.dynamiceconomy.gui.*;
import dev.n1xend.dynamiceconomy.services.EconomyService;
import dev.n1xend.dynamiceconomy.utils.GUIHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handles all inventory click events for DynamicEconomy GUIs.
 *
 * <p>Routes click events to the appropriate GUI handler based on inventory title.
 * All GUI inventories are identified by their title string prefix.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class GuiListener implements Listener {

    private final DynamicEconomy plugin;
    private final MainMenuGui mainMenuGui;
    private final CategoryGui categoryGui;
    private final SellConfirmGui sellConfirmGui;

    public GuiListener(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
        this.mainMenuGui = new MainMenuGui(plugin);
        this.categoryGui = new CategoryGui(plugin);
        this.sellConfirmGui = new SellConfirmGui(plugin);
    }

    // -------------------------------------------------------------------------
    // Click events
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!isOurGui(title)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        if (title.equals(MainMenuGui.TITLE)) {
            handleMainMenu(player, slot);
        } else if (title.startsWith("§6§l") && !title.startsWith(SellConfirmGui.TITLE_PREFIX)) {
            handleCategoryMenu(player, slot, clicked, click);
        } else if (title.startsWith(SellConfirmGui.TITLE_PREFIX)) {
            handleConfirmMenu(player, slot);
        }
    }

    // -------------------------------------------------------------------------
    // Close events
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Delay cleanup by 1 tick — navigation clicks need state in the same tick
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            String newTitle = player.getOpenInventory().getTitle();
            if (!isOurGui(newTitle)) {
                GuiStateStore.cleanup(player.getUniqueId());
            }
        }, 1L);
    }

    // -------------------------------------------------------------------------
    // Main menu handler
    // -------------------------------------------------------------------------

    private void handleMainMenu(@NotNull Player player, int slot) {
        for (MarketCategory category : plugin.getEconomyService().getCategories().values()) {
            if (category.getGuiSlot() == slot) {
                categoryGui.open(player, category.getId(), 0);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Category menu handler
    // -------------------------------------------------------------------------

    private void handleCategoryMenu(@NotNull Player player, int slot,
                                     @NotNull ItemStack clicked, @NotNull ClickType click) {
        UUID uuid = player.getUniqueId();
        String categoryId = GuiStateStore.getCategory(uuid);
        int page = GuiStateStore.getPage(uuid);

        // Back button
        if (slot == 49) {
            mainMenuGui.open(player);
            return;
        }

        // Pagination
        if (slot == 45 && page > 0) {
            categoryGui.open(player, categoryId, page - 1);
            return;
        }
        if (slot == 53) {
            categoryGui.open(player, categoryId, page + 1);
            return;
        }

        // Item click — only handle inner item slots
        if (!CategoryGui.isItemSlot(slot)) {
            return;
        }

        MarketItem item = plugin.getEconomyService().getItem(clicked.getType().name());
        if (item == null) {
            return;
        }

        GuiStateStore.SellMode mode = switch (click) {
            case RIGHT -> GuiStateStore.SellMode.STACK;
            case SHIFT_LEFT, SHIFT_RIGHT -> GuiStateStore.SellMode.ONE;
            default -> GuiStateStore.SellMode.ALL;
        };

        sellConfirmGui.open(player, item.getId(), mode);
    }

    // -------------------------------------------------------------------------
    // Confirm menu handler
    // -------------------------------------------------------------------------

    private void handleConfirmMenu(@NotNull Player player, int slot) {
        UUID uuid = player.getUniqueId();
        String categoryId = GuiStateStore.getCategory(uuid);
        String materialId = GuiStateStore.getSellItem(uuid);

        // Cancel
        if (slot == SellConfirmGui.getCancelSlot()) {
            if (categoryId != null) {
                categoryGui.open(player, categoryId, GuiStateStore.getPage(uuid));
            } else {
                mainMenuGui.open(player);
            }
            return;
        }

        // Confirm sell
        if (slot == SellConfirmGui.getConfirmSlot()) {
            if (materialId == null) {
                return;
            }

            int amount = GuiStateStore.getSellAmount(uuid);
            EconomyService.SellData result = plugin.getEconomyService().trySell(player, materialId, amount);
            sendSellFeedback(player, result);

            // Re-open category on next tick after sell
            String finalCategoryId = categoryId;
            int finalPage = GuiStateStore.getPage(uuid);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (finalCategoryId != null) {
                    categoryGui.open(player, finalCategoryId, finalPage);
                } else {
                    mainMenuGui.open(player);
                }
            }, 1L);
        }
    }

    // -------------------------------------------------------------------------
    // Sell result feedback
    // -------------------------------------------------------------------------

    private void sendSellFeedback(@NotNull Player player, @NotNull EconomyService.SellData data) {
        switch (data.result()) {
            case SUCCESS -> {
                String msg = plugin.getMessageManager().prefixed("sell.success",
                    "%amount%", GuiStateStore.getSellAmount(player.getUniqueId()),
                    "%item%", data.itemDisplayName(),
                    "%payout%", GUIHelper.formatPrice(data.payout()));
                player.sendMessage(msg);

                String priceMsg = plugin.getMessageManager().prefixed("sell.price-now",
                    "%percent%", String.format("%.0f", data.multiplierAfter() * 100));
                player.sendMessage(priceMsg);

                if (data.hadDiversityBonus()) {
                    int bonusPct = (int) (plugin.getConfigManager().getDiversityBonusMultiplier() * 100);
                    player.sendMessage(plugin.getMessageManager().get("sell.bonus-diversity", "%bonus%", bonusPct));
                }
                if (data.hadContractBonus()) {
                    int bonusPct = (int) (plugin.getConfigManager().getContractBonusMultiplier() * 100);
                    player.sendMessage(plugin.getMessageManager().get("sell.bonus-contract", "%bonus%", bonusPct));
                }
            }
            case COOLDOWN ->
                player.sendMessage(plugin.getMessageManager().prefixed("sell.cooldown"));
            case NOT_ENOUGH_ITEMS ->
                player.sendMessage(plugin.getMessageManager().prefixed("sell.no-items",
                    "%item%", data.itemDisplayName()));
            case ITEM_NOT_SOLD ->
                player.sendMessage(plugin.getMessageManager().prefixed("sell.unknown-item"));
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private boolean isOurGui(@NotNull String title) {
        return title.equals(MainMenuGui.TITLE)
            || title.startsWith("§6§l")
            || title.startsWith(SellConfirmGui.TITLE_PREFIX);
    }
}
