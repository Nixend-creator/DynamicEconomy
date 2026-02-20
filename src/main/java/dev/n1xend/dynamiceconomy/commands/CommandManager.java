package dev.n1xend.dynamiceconomy.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.commands.impl.ShopAdminCommand;
import dev.n1xend.dynamiceconomy.commands.impl.ShopCommand;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers all plugin commands via Paper's LifecycleEventManager (Brigadier).
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
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

            // /shop
            commands.register(
                Commands.literal("shop")
                    .requires(src -> src.getSender().hasPermission("dynamiceconomy.shop"))
                    .executes(ctx -> {
                        shopCmd.execute(ctx.getSource().getSender());
                        return Command.SINGLE_SUCCESS;
                    })
                    .build(),
                "Open the dynamic market",
                List.of("market", "store")
            );

            // /shopadmin
            commands.register(
                Commands.literal("shopadmin")
                    .requires(src -> src.getSender().hasPermission("dynamiceconomy.admin"))
                    .executes(ctx -> {
                        adminCmd.execute(ctx.getSource().getSender(), new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("reload")
                        .executes(ctx -> {
                            adminCmd.execute(ctx.getSource().getSender(), new String[]{"reload"});
                            return Command.SINGLE_SUCCESS;
                        }))
                    .then(Commands.literal("info")
                        .executes(ctx -> {
                            adminCmd.execute(ctx.getSource().getSender(), new String[]{"info"});
                            return Command.SINGLE_SUCCESS;
                        }))
                    .then(Commands.literal("reset")
                        .then(Commands.argument("target", StringArgumentType.word())
                            .executes(ctx -> {
                                // ctx.getArgument вместо StringArgumentType.getWord
                                String target = ctx.getArgument("target", String.class);
                                adminCmd.execute(ctx.getSource().getSender(), new String[]{"reset", target});
                                return Command.SINGLE_SUCCESS;
                            })))
                    .then(Commands.literal("setprice")
                        .then(Commands.argument("item", StringArgumentType.word())
                            .then(Commands.argument("multiplier", FloatArgumentType.floatArg(0.01f, 10.0f))
                                .executes(ctx -> {
                                    String item = ctx.getArgument("item", String.class);
                                    float mult = ctx.getArgument("multiplier", Float.class);
                                    adminCmd.execute(ctx.getSource().getSender(), new String[]{"setprice", item, String.valueOf(mult)});
                                    return Command.SINGLE_SUCCESS;
                                }))))
                    .build(),
                "Admin commands for DynamicEconomy"
            );
        });
    }
}
