package dev.n1xend.dynamiceconomy.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.commands.impl.*;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers all plugin commands via Paper's LifecycleEventManager (Brigadier).
 *
 * @author n1xend
 * @version 1.2.0
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandManager {

    private final DynamicEconomy plugin;

    public CommandManager(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            ShopCommand shopCmd = new ShopCommand(plugin);
            ShopAdminCommand adminCmd = new ShopAdminCommand(plugin);
            ShopHistoryCommand historyCmd = new ShopHistoryCommand(plugin);
            AuctionCommand auctionCmd = new AuctionCommand(plugin);
            LicenseCommand licenseCmd = new LicenseCommand(plugin);

            // /shop
            commands.register(
                Commands.literal("shop")
                    .requires(src -> src.getSender().hasPermission("dynamiceconomy.shop"))
                    .executes(ctx -> { shopCmd.execute(ctx.getSource().getSender()); return Command.SINGLE_SUCCESS; })
                    .build(),
                "Open the dynamic market", List.of("market", "store")
            );

            // /shophistory <item>
            commands.register(
                Commands.literal("shophistory")
                    .requires(src -> src.getSender().hasPermission("dynamiceconomy.shop"))
                    .then(Commands.argument("item", StringArgumentType.word())
                        .executes(ctx -> {
                            historyCmd.execute(ctx.getSource().getSender(),
                                ctx.getArgument("item", String.class));
                            return Command.SINGLE_SUCCESS;
                        }))
                    .build(),
                "View 24h price history chart", List.of("pricehistory", "ph")
            );

            // /auction list | sell <price> | buy <id> | cancel <id> | search <item>
            commands.register(
                Commands.literal("auction")
                    .requires(src -> src.getSender().hasPermission("dynamiceconomy.shop"))
                    .executes(ctx -> { auctionCmd.list(ctx.getSource().getSender()); return Command.SINGLE_SUCCESS; })
                    .then(Commands.literal("list")
                        .executes(ctx -> { auctionCmd.list(ctx.getSource().getSender()); return Command.SINGLE_SUCCESS; }))
                    .then(Commands.literal("sell")
                        .then(Commands.argument("price", FloatArgumentType.floatArg(0.01f))
                            .executes(ctx -> {
                                auctionCmd.sell(ctx.getSource().getSender(), ctx.getArgument("price", Float.class));
                                return Command.SINGLE_SUCCESS;
                            })))
                    .then(Commands.literal("buy")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> {
                                auctionCmd.buy(ctx.getSource().getSender(), ctx.getArgument("id", String.class));
                                return Command.SINGLE_SUCCESS;
                            })))
                    .then(Commands.literal("cancel")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> {
                                auctionCmd.cancel(ctx.getSource().getSender(), ctx.getArgument("id", String.class));
                                return Command.SINGLE_SUCCESS;
                            })))
                    .then(Commands.literal("search")
                        .then(Commands.argument("item", StringArgumentType.word())
                            .executes(ctx -> {
                                auctionCmd.search(ctx.getSource().getSender(), ctx.getArgument("item", String.class));
                                return Command.SINGLE_SUCCESS;
                            })))
                    .build(),
                "Auction house", List.of("ah", "auctionhouse")
            );

            // /license
            commands.register(
                Commands.literal("license")
                    .requires(src -> src.getSender().hasPermission("dynamiceconomy.shop"))
                    .executes(ctx -> { licenseCmd.execute(ctx.getSource().getSender()); return Command.SINGLE_SUCCESS; })
                    .build(),
                "View your trade license and stats", List.of("tradelicense", "rank")
            );

            // /shopadmin
            commands.register(
                Commands.literal("shopadmin")
                    .requires(src -> src.getSender().hasPermission("dynamiceconomy.admin"))
                    .executes(ctx -> { adminCmd.execute(ctx.getSource().getSender(), new String[0]); return Command.SINGLE_SUCCESS; })
                    .then(Commands.literal("reload")
                        .executes(ctx -> { adminCmd.execute(ctx.getSource().getSender(), new String[]{"reload"}); return Command.SINGLE_SUCCESS; }))
                    .then(Commands.literal("info")
                        .executes(ctx -> { adminCmd.execute(ctx.getSource().getSender(), new String[]{"info"}); return Command.SINGLE_SUCCESS; }))
                    .then(Commands.literal("reset")
                        .then(Commands.argument("target", StringArgumentType.word())
                            .executes(ctx -> {
                                adminCmd.execute(ctx.getSource().getSender(), new String[]{"reset", ctx.getArgument("target", String.class)});
                                return Command.SINGLE_SUCCESS;
                            })))
                    .then(Commands.literal("setprice")
                        .then(Commands.argument("item", StringArgumentType.word())
                            .then(Commands.argument("multiplier", FloatArgumentType.floatArg(0.01f, 10.0f))
                                .executes(ctx -> {
                                    adminCmd.execute(ctx.getSource().getSender(), new String[]{"setprice",
                                        ctx.getArgument("item", String.class),
                                        String.valueOf(ctx.getArgument("multiplier", Float.class))});
                                    return Command.SINGLE_SUCCESS;
                                }))))
                    .then(Commands.literal("event")
                        .then(Commands.argument("type", StringArgumentType.word())
                            .then(Commands.argument("item", StringArgumentType.word())
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 120))
                                    .executes(ctx -> {
                                        adminCmd.execute(ctx.getSource().getSender(), new String[]{"event",
                                            ctx.getArgument("type", String.class),
                                            ctx.getArgument("item", String.class),
                                            String.valueOf(ctx.getArgument("duration", Integer.class))});
                                        return Command.SINGLE_SUCCESS;
                                    })))))
                    .then(Commands.literal("treasury")
                        .executes(ctx -> { adminCmd.execute(ctx.getSource().getSender(), new String[]{"treasury"}); return Command.SINGLE_SUCCESS; })
                        .then(Commands.literal("give")
                            .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.01f))
                                    .executes(ctx -> {
                                        adminCmd.execute(ctx.getSource().getSender(), new String[]{"treasury", "give",
                                            ctx.getArgument("player", String.class),
                                            String.valueOf(ctx.getArgument("amount", Float.class))});
                                        return Command.SINGLE_SUCCESS;
                                    }))))
                        .then(Commands.literal("giveall")
                            .then(Commands.argument("amount", FloatArgumentType.floatArg(0.01f))
                                .executes(ctx -> {
                                    adminCmd.execute(ctx.getSource().getSender(), new String[]{"treasury", "giveall",
                                        String.valueOf(ctx.getArgument("amount", Float.class))});
                                    return Command.SINGLE_SUCCESS;
                                }))))
                    .build(),
                "Admin commands for DynamicEconomy"
            );
        });
    }
}
