package dev.n1xend.dynamiceconomy.commands.impl;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.gui.MainMenuGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the {@code /shop} command — opens the main market GUI.
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class ShopCommand implements CommandExecutor {

    private final DynamicEconomy plugin;
    private final MainMenuGui mainMenuGui;

    public ShopCommand(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
        this.mainMenuGui = new MainMenuGui(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("error.players-only"));
            return true;
        }

        if (!player.hasPermission("dynamiceconomy.shop")) {
            player.sendMessage(plugin.getMessageManager().get("error.no-permission"));
            return true;
        }

        mainMenuGui.open(player);
        return true;
    }
}
