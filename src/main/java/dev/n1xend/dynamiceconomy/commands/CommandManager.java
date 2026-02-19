package dev.n1xend.dynamiceconomy.commands;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.commands.impl.ShopAdminCommand;
import dev.n1xend.dynamiceconomy.commands.impl.ShopCommand;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all plugin commands with their executors.
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class CommandManager {

    private final DynamicEconomy plugin;

    public CommandManager(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all commands declared in paper-plugin.yml.
     */
    public void register() {
        ShopAdminCommand shopAdminCommand = new ShopAdminCommand(plugin);

        plugin.getCommand("shop").setExecutor(new ShopCommand(plugin));
        plugin.getCommand("shopadmin").setExecutor(shopAdminCommand);
        plugin.getCommand("shopadmin").setTabCompleter(shopAdminCommand);
    }
}
