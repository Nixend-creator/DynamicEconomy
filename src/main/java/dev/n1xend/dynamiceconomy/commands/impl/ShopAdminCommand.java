package dev.n1xend.dynamiceconomy.commands.impl;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import dev.n1xend.dynamiceconomy.utils.GUIHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the {@code /shopadmin} command — admin utilities for the market.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code reload} — reloads config and market data</li>
 *   <li>{@code reset <item|all>} — resets price multiplier(s) to 1.0</li>
 *   <li>{@code setprice <item> <multiplier>} — sets a specific multiplier</li>
 *   <li>{@code info} — shows all current prices in chat</li>
 * </ul></p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class ShopAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "reset", "setprice", "info");

    private final DynamicEconomy plugin;

    public ShopAdminCommand(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("dynamiceconomy.admin")) {
            sender.sendMessage(plugin.getMessageManager().get("admin.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "setprice" -> handleSetPrice(sender, args);
            case "info" -> handleInfo(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    // -------------------------------------------------------------------------
    // Subcommand handlers
    // -------------------------------------------------------------------------

    private boolean handleReload(@NotNull CommandSender sender) {
        plugin.reloadConfig();
        plugin.getDataManager().load();
        sender.sendMessage(plugin.getMessageManager().prefixed("admin.reloaded"));
        return true;
    }

    private boolean handleReset(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.help.reset"));
            return true;
        }

        if (args[1].equalsIgnoreCase("all")) {
            for (var cat : plugin.getEconomyService().getCategories().values()) {
                cat.getItems().forEach(i -> i.setCurrentMultiplier(1.0));
            }
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.reset-all"));
            return true;
        }

        MarketItem item = plugin.getEconomyService().getItem(args[1].toUpperCase());
        if (item == null) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.item-not-found", "%item%", args[1]));
            return true;
        }

        item.setCurrentMultiplier(1.0);
        sender.sendMessage(plugin.getMessageManager().prefixed("admin.reset-item", "%item%", item.getDisplayName()));
        return true;
    }

    private boolean handleSetPrice(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.help.setprice"));
            return true;
        }

        MarketItem item = plugin.getEconomyService().getItem(args[1].toUpperCase());
        if (item == null) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.item-not-found", "%item%", args[1]));
            return true;
        }

        try {
            double multiplier = Double.parseDouble(args[2]);
            multiplier = Math.max(0.01, Math.min(10.0, multiplier));
            item.setCurrentMultiplier(multiplier);
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.price-set",
                "%item%", item.getDisplayName(),
                "%value%", String.format("%.2f", multiplier)));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().prefixed("admin.invalid-number", "%value%", args[2]));
        }

        return true;
    }

    private boolean handleInfo(@NotNull CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().get("admin.info.header"));

        for (var cat : plugin.getEconomyService().getCategories().values()) {
            boolean isHot = cat.getId().equals(plugin.getEconomyService().getHotCategoryId());
            String catLine = plugin.getMessageManager().get("admin.info.category-line",
                "%category%", cat.getDisplayName() + (isHot ? " 🔥" : ""));
            sender.sendMessage(catLine);

            for (MarketItem item : cat.getItems()) {
                String itemLine = plugin.getMessageManager().get("admin.info.item-line",
                    "%id%", item.getId(),
                    "%percent%", String.format("%.0f", item.getCurrentMultiplier() * 100),
                    "%price%", GUIHelper.formatPrice(item.getCurrentPrice()));
                sender.sendMessage(itemLine);
            }
        }

        sender.sendMessage(plugin.getMessageManager().get("admin.info.contracts",
            "%count%", plugin.getContractService().getActiveContracts().size()));

        return true;
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().get("admin.help.header"));
        sender.sendMessage(plugin.getMessageManager().get("admin.help.reload"));
        sender.sendMessage(plugin.getMessageManager().get("admin.help.reset"));
        sender.sendMessage(plugin.getMessageManager().get("admin.help.setprice"));
        sender.sendMessage(plugin.getMessageManager().get("admin.help.info"));
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("dynamiceconomy.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("reset") || sub.equals("setprice")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("all");
                for (var cat : plugin.getEconomyService().getCategories().values()) {
                    cat.getItems().forEach(i -> {
                        if (i.getId().toLowerCase().startsWith(args[1].toLowerCase())) {
                            suggestions.add(i.getId());
                        }
                    });
                }
                return suggestions;
            }
        }

        return List.of();
    }
}
