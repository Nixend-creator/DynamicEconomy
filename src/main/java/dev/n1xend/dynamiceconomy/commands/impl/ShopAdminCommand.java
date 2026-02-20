package dev.n1xend.dynamiceconomy.commands.impl;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import dev.n1xend.dynamiceconomy.market.MarketEventService;
import dev.n1xend.dynamiceconomy.utils.GUIHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /shopadmin — admin utilities.
 * Subcommands: reload, reset, setprice, info, event, treasury
 *
 * @author n1xend
 * @version 1.2.0
 */
public class ShopAdminCommand {

    private final DynamicEconomy plugin;

    public ShopAdminCommand(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dynamiceconomy.admin")) {
            sender.sendMessage("§cНет прав."); return;
        }
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "reload"   -> handleReload(sender);
            case "reset"    -> handleReset(sender, args);
            case "setprice" -> handleSetPrice(sender, args);
            case "info"     -> handleInfo(sender);
            case "event"    -> handleEvent(sender, args);
            case "treasury" -> handleTreasury(sender, args);
            default         -> sendHelp(sender);
        }
    }

    // ── reload ────────────────────────────────────────────────────────────────
    private void handleReload(@NotNull CommandSender sender) {
        plugin.reloadConfig();
        plugin.getDataManager().load();
        sender.sendMessage("§a[DynamicEconomy] Конфигурация перезагружена.");
    }

    // ── reset ─────────────────────────────────────────────────────────────────
    private void handleReset(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) { sender.sendMessage("§7Использование: /shopadmin reset <item|all>"); return; }
        if (args[1].equalsIgnoreCase("all")) {
            plugin.getEconomyService().getCategories().values()
                .forEach(cat -> cat.getItems().forEach(i -> i.setCurrentMultiplier(1.0)));
            sender.sendMessage("§aВсе цены сброшены до базовых.");
            return;
        }
        MarketItem item = plugin.getEconomyService().getItem(args[1].toUpperCase());
        if (item == null) { sender.sendMessage("§cПредмет не найден: §e" + args[1]); return; }
        item.setCurrentMultiplier(1.0);
        sender.sendMessage("§aЦена §e" + item.getDisplayName() + " §aсброшена.");
    }

    // ── setprice ──────────────────────────────────────────────────────────────
    private void handleSetPrice(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) { sender.sendMessage("§7Использование: /shopadmin setprice <item> <multiplier>"); return; }
        MarketItem item = plugin.getEconomyService().getItem(args[1].toUpperCase());
        if (item == null) { sender.sendMessage("§cПредмет не найден: §e" + args[1]); return; }
        try {
            double m = Math.max(0.01, Math.min(10.0, Double.parseDouble(args[2])));
            item.setCurrentMultiplier(m);
            sender.sendMessage("§aМультипликатор §e" + item.getDisplayName()
                + " §aустановлен: §e" + String.format("%.2f", m)
                + " §7(цена: " + GUIHelper.formatPrice(item.getCurrentPrice()) + ")");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное число: §e" + args[2]);
        }
    }

    // ── info ──────────────────────────────────────────────────────────────────
    private void handleInfo(@NotNull CommandSender sender) {
        sender.sendMessage("§6§l══ DynamicEconomy ══");
        sender.sendMessage("§7Категорий: §e" + plugin.getEconomyService().getCategories().size());
        sender.sendMessage("§7Предметов: §e" + plugin.getEconomyService().getItemIndex().size());
        sender.sendMessage("§7Контрактов активных: §e" + plugin.getContractService().getActiveContracts().size());
        sender.sendMessage("§7Событий рынка: §e" + plugin.getMarketEventService().getActiveEvents().size());
        sender.sendMessage("§7Лотов аукциона: §e" + plugin.getAuctionService().getActiveListings().size());
        sender.sendMessage("§7Казна: §e" + GUIHelper.formatPrice(plugin.getTreasuryService().getBalance()));
        String hot = plugin.getEconomyService().getHotCategoryId();
        if (hot != null) {
            var cat = plugin.getEconomyService().getCategory(hot);
            sender.sendMessage("§7Горячая категория: §c🔥 " + (cat != null ? cat.getDisplayName() : hot));
        }
        sender.sendMessage("§6§l══ Активные события ══");
        if (plugin.getMarketEventService().getActiveEvents().isEmpty()) {
            sender.sendMessage("§7  Нет активных событий");
        } else {
            for (var ev : plugin.getMarketEventService().getActiveEvents().values()) {
                long secsLeft = (ev.expiresAt() - System.currentTimeMillis()) / 1000;
                sender.sendMessage("§e  " + ev.itemId() + " §8— §c" + ev.type().getDisplayName()
                    + " §7(" + secsLeft + "с осталось)");
            }
        }
    }

    // ── event ─────────────────────────────────────────────────────────────────
    private void handleEvent(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§7Использование: /shopadmin event <BOOM|SHORTAGE|CRASH|PANIC> <item> <минуты>");
            sender.sendMessage("§7Типы: §aBOOM §e(x2) §eSHORTAGE §e(x1.5) §cCRASH §e(x0.4) §4PANIC §e(x0.2)");
            return;
        }
        MarketEventService.EventType type;
        try {
            type = MarketEventService.EventType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cНеверный тип события. Доступно: BOOM, SHORTAGE, CRASH, PANIC"); return;
        }
        MarketItem item = plugin.getEconomyService().getItem(args[2].toUpperCase());
        if (item == null) { sender.sendMessage("§cПредмет не найден: §e" + args[2]); return; }
        try {
            int duration = Integer.parseInt(args[3]);
            plugin.getMarketEventService().fireEvent(item.getId(), type, duration);
            sender.sendMessage("§aСобытие §e" + type.getDisplayName() + " §aзапущено для §e"
                + item.getDisplayName() + " §aна §e" + duration + " §aмин.");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное число минут: §e" + args[3]);
        }
    }

    // ── treasury ──────────────────────────────────────────────────────────────
    private void handleTreasury(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            // info
            sender.sendMessage("§6§l══ Казна сервера ══");
            sender.sendMessage("§7Баланс:      §e" + GUIHelper.formatPrice(plugin.getTreasuryService().getBalance()));
            sender.sendMessage("§7Собрано всего: §a" + GUIHelper.formatPrice(plugin.getTreasuryService().getTotalCollected()));
            sender.sendMessage("§7Выдано всего:  §c" + GUIHelper.formatPrice(plugin.getTreasuryService().getTotalDistributed()));
            sender.sendMessage("§7/shopadmin treasury give <игрок> <сумма>");
            sender.sendMessage("§7/shopadmin treasury giveall <сумма>");
            return;
        }
        if (args[1].equalsIgnoreCase("give") && args.length >= 4) {
            Player target = plugin.getServer().getPlayer(args[2]);
            if (target == null) { sender.sendMessage("§cИгрок не найден или не в сети: §e" + args[2]); return; }
            try {
                double amount = Double.parseDouble(args[3]);
                if (plugin.getTreasuryService().distribute(target, amount)) {
                    sender.sendMessage("§aВыдано §e" + GUIHelper.formatPrice(amount) + " §aигроку §e" + target.getName());
                    target.sendMessage("§6[Казна] §aВам выдано §e" + GUIHelper.formatPrice(amount) + " §aиз казны сервера!");
                } else {
                    sender.sendMessage("§cНедостаточно средств в казне (баланс: "
                        + GUIHelper.formatPrice(plugin.getTreasuryService().getBalance()) + ")");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cНеверная сумма: §e" + args[3]);
            }
            return;
        }
        if (args[1].equalsIgnoreCase("giveall") && args.length >= 3) {
            try {
                double amount = Double.parseDouble(args[2]);
                int count = plugin.getTreasuryService().distributeToAll(amount);
                if (count > 0) {
                    sender.sendMessage("§aРаздано §e" + GUIHelper.formatPrice(amount)
                        + " §aмежду §e" + count + " §aигроками онлайн.");
                    double share = amount / count;
                    plugin.getServer().broadcastMessage(
                            "§6[Казна] §eСервер раздаёт §a" + GUIHelper.formatPrice(share)
                            + " §eкаждому онлайн-игроку!");
                } else {
                    sender.sendMessage("§cНедостаточно средств или нет игроков онлайн.");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cНеверная сумма: §e" + args[2]);
            }
            return;
        }
        handleTreasury(sender, new String[]{"treasury"});
    }

    // ── help ──────────────────────────────────────────────────────────────────
    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage("§6§l══ /shopadmin ══");
        sender.sendMessage("§e/shopadmin reload §7— перезагрузить конфиг");
        sender.sendMessage("§e/shopadmin info §7— статистика сервера");
        sender.sendMessage("§e/shopadmin reset <item|all> §7— сброс цен");
        sender.sendMessage("§e/shopadmin setprice <item> <mult> §7— установить мультипликатор");
        sender.sendMessage("§e/shopadmin event <тип> <item> <мин> §7— запустить событие");
        sender.sendMessage("§e/shopadmin treasury §7— информация о казне");
        sender.sendMessage("§e/shopadmin treasury give <игрок> <сумма> §7— выдать из казны");
        sender.sendMessage("§e/shopadmin treasury giveall <сумма> §7— раздать всем онлайн");
    }
}
