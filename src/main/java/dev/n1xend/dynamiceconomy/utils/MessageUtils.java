package dev.n1xend.dynamiceconomy.utils;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods for player messaging.
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MessageUtils {

    private MessageUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sends a message to a player only if they are online.
     *
     * @param player  target player, may be null
     * @param message message to send (color codes pre-translated)
     */
    public static void sendMessage(@Nullable Player player, @NotNull String message) {
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }

    /**
     * Translates {@code &} color codes to {@code §}.
     *
     * @param text input text
     * @return colorized string
     */
    @NotNull
    public static String colorize(@NotNull String text) {
        return text.replace("&", "§");
    }

    /**
     * Replaces placeholders in a message string.
     *
     * <p>Placeholders are passed as alternating key-value pairs:
     * {@code replace("Hello %name%!", "%name%", "Alex")}</p>
     *
     * @param message      template message
     * @param replacements alternating placeholder-value pairs
     * @return processed message
     * @throws IllegalArgumentException if message is null
     */
    @NotNull
    public static String replacePlaceholders(@NotNull String message, @NotNull Object... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be key-value pairs (even count)");
        }
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        }
        return message;
    }
}
