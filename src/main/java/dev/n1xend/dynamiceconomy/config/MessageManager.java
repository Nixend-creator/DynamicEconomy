package dev.n1xend.dynamiceconomy.config;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages localized messages loaded from messages/messages_{lang}.yml.
 *
 * <p>Supports placeholder replacement using %key% syntax.
 * Falls back to the key itself if the message is not found.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class MessageManager {

    private static final String MESSAGES_DIR = "messages";

    private final DynamicEconomy plugin;
    private final Map<String, String> messages = new HashMap<>();

    public MessageManager(DynamicEconomy plugin) {
        this.plugin = plugin;
        saveDefaultMessages();
        loadMessages();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a formatted message with color codes translated.
     *
     * @param key the message key (e.g. "sell.success")
     * @return colored message string, falls back to key if not found
     */
    @NotNull
    public String get(@NotNull String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * Returns a formatted message with placeholder replacements.
     *
     * <p>Placeholders are passed as alternating key-value pairs:
     * {@code get("sell.success", "%amount%", "64", "%item%", "Wheat")}</p>
     *
     * @param key          message key
     * @param replacements alternating placeholder-value pairs
     * @return formatted message
     */
    @NotNull
    public String get(@NotNull String key, @NotNull Object... replacements) {
        String message = get(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(
                String.valueOf(replacements[i]),
                String.valueOf(replacements[i + 1])
            );
        }
        return message;
    }

    /**
     * Returns the prefix string.
     */
    @NotNull
    public String getPrefix() {
        return get("prefix");
    }

    /**
     * Builds a prefixed message.
     */
    @NotNull
    public String prefixed(@NotNull String key, @NotNull Object... replacements) {
        return getPrefix() + get(key, replacements);
    }

    /**
     * Reloads messages from disk after config change.
     */
    public void reload() {
        messages.clear();
        loadMessages();
    }

    // -------------------------------------------------------------------------
    // Private
    // -------------------------------------------------------------------------

    private void saveDefaultMessages() {
        String[] files = {"messages_ru.yml", "messages_en.yml"};
        for (String fileName : files) {
            String resourcePath = MESSAGES_DIR + "/" + fileName;
            File file = new File(plugin.getDataFolder(), resourcePath);
            if (!file.exists()) {
                plugin.saveResource(resourcePath, false);
            }
        }
    }

    private void loadMessages() {
        String lang = plugin.getConfigManager().getLanguage();
        String fileName = "messages_" + lang + ".yml";
        File file = new File(plugin.getDataFolder(), MESSAGES_DIR + "/" + fileName);

        FileConfiguration config;

        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            // Load from JAR as fallback
            InputStream stream = plugin.getResource(MESSAGES_DIR + "/" + fileName);
            if (stream == null) {
                plugin.getLogger().warning("Message file not found: " + fileName + ". Falling back to messages_ru.yml");
                stream = plugin.getResource(MESSAGES_DIR + "/messages_ru.yml");
            }
            if (stream == null) {
                plugin.getLogger().severe("No message files found in JAR! Messages will not work.");
                return;
            }
            config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        flattenKeys(config, "", messages);
        plugin.getLogger().info("Loaded " + messages.size() + " messages (" + lang + ").");
    }

    private void flattenKeys(FileConfiguration config, String prefix, Map<String, String> target) {
        for (String key : config.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (config.isConfigurationSection(key)) {
                var section = config.getConfigurationSection(key);
                if (section != null) {
                    for (String subKey : section.getKeys(true)) {
                        String value = section.getString(subKey);
                        if (value != null) {
                            target.put(fullKey + "." + subKey, colorize(value));
                        }
                    }
                }
            } else if (config.isString(key)) {
                String value = config.getString(key);
                if (value != null) {
                    target.put(fullKey, colorize(value));
                }
            }
        }
    }

    private String colorize(String text) {
        return text.replace("&", "§");
    }
}
