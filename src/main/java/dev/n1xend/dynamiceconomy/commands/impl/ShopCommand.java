package dev.n1xend.dynamiceconomy.commands.impl;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.gui.MainMenuGui;
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
public class ShopCommand {

    private final DynamicEconomy plugin;
    private final MainMenuGui mainMenuGui;

    public ShopCommand(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
        this.mainMenuGui = new MainMenuGui(plugin);
    }

    public void execute(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("error.players-only"));
            return;
        }
        mainMenuGui.open(player);
    }
}
