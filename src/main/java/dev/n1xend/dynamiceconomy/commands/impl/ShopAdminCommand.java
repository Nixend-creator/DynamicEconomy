package dev.n1xend.dynamiceconomy.commands.impl;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import dev.n1xend.dynamiceconomy.utils.GUIHelper;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the {@code /shopadmin} command — admin utilities for the market.
 *
 * <p>Subcommands: reload, reset, setprice, info</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class ShopAdminCommand {

    private final DynamicEconomy plugin;

    public ShopAdminCommand(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dynamiceconomy.admin")) {
            sender.sendMessage(plugin.getMessageManager().get("admin.no-permission"));
            return;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "reset"  -> handleReset(sender, args);
            case "setprice" -> handleSetPrice(sender, args);
            case "info"   -> handleInfo(sender);
            default       -> sendHelp(sender);
        }
    }

    // -------------------------------------------------------------------------

    private void handleReload(@NotNull CommandSender sender) {
        plugin.reloadConfig();
        plugin.getDataManager().load();
        sender.sendMessage(plugin.getMessageManager().prefixed("admin.reloaded"));
    }

    private void handleReset(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.help.reset"));
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            for (var cat : plugin.getEconomyService().getCategories().values()) {
                cat.getItems().forEach(i -> i.setCurrentMultiplier(1.0));
            }
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.reset-all"));
            return;
        }

        MarketItem item = plugin.getEconomyService().getItem(args[1].toUpperCase());
        if (item == null) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.item-not-found", "%item%", args[1]));
            return;
        }
        item.setCurrentMultiplier(1.0);
        sender.sendMessage(plugin.getMessageManager().prefixed("admin.reset-item", "%item%", item.getDisplayName()));
    }

    private void handleSetPrice(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.help.setprice"));
            return;
        }

        MarketItem item = plugin.getEconomyService().getItem(args[1].toUpperCase());
        if (item == null) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.item-not-found", "%item%", args[1]));
            return;
        }

        try {
            double multiplier = Math.max(0.01, Math.min(10.0, Double.parseDouble(args[2])));
            item.setCurrentMultiplier(multiplier);
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.price-set",
                "%item%", item.getDisplayName(),
                "%value%", String.format("%.2f", multiplier)));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.invalid-number", "%value%", args[2]));
        }
    }

    private void handleInfo(@NotNull CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().get("admin.info.header"));
        for (var cat : plugin.getEconomyService().getCategories().values()) {
            boolean isHot = cat.getId().equals(plugin.getEconomyService().getHotCategoryId());
            sender.sendMessage(plugin.getMessageManager().get("admin.info.category-line",
                "%category%", cat.getDisplayName() + (isHot ? " 🔥" : "")));
            for (MarketItem item : cat.getItems()) {
                sender.sendMessage(plugin.getMessageManager().get("admin.info.item-line",
                    "%id%", item.getId(),
                    "%percent%", String.format("%.0f", item.getCurrentMultiplier() * 100),
                    "%price%", GUIHelper.formatPrice(item.getCurrentPrice())));
            }
        }
        sender.sendMessage(plugin.getMessageManager().get("admin.info.contracts",
            "%count%", plugin.getContractService().getActiveContracts().size()));
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().get("admin.help.header"));
        sender.sendMessage(plugin.getMessageManager().get("admin.help.reload"));
        sender.sendMessage(plugin.getMessageManager().get("admin.help.reset"));
        sender.sendMessage(plugin.getMessageManager().get("admin.help.setprice"));
        sender.sendMessage(plugin.getMessageManager().get("admin.help.info"));
    }
}
