package dev.n1xend.dynamiceconomy.services;

import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketCategory;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads market categories and items from items.yml.
 *
 * <p>Separated from EconomyService to follow single-responsibility principle.
 * This class is stateless after construction and used once at startup.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class MarketLoader {

    private final DynamicEconomy plugin;
    private final Logger logger;

    public MarketLoader(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    // -------------------------------------------------------------------------
    // Public
    // -------------------------------------------------------------------------

    /**
     * Loads all categories and their items from items.yml.
     *
     * @return ordered map of categoryId → MarketCategory
     */
    @NotNull
    public Map<String, MarketCategory> loadCategories() {
        plugin.saveResource("items.yml", false);

        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);

        Map<String, MarketCategory> categories = new LinkedHashMap<>();

        ConfigurationSection categoriesSection = itemsConfig.getConfigurationSection("categories");
        if (categoriesSection == null) {
            logger.warning("No categories section found in items.yml!");
            return categories;
        }

        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection catSection = categoriesSection.getConfigurationSection(categoryId);
            if (catSection == null) {
                continue;
            }

            MarketCategory category = parseCategory(categoryId, catSection);
            if (category == null) {
                continue;
            }

            loadItemsIntoCategory(category, catSection);
            categories.put(categoryId, category);

            logger.info("Loaded category '" + categoryId + "' with " + category.getItems().size() + " items.");
        }

        return categories;
    }

    // -------------------------------------------------------------------------
    // Private parsing
    // -------------------------------------------------------------------------

    private MarketCategory parseCategory(String categoryId, ConfigurationSection section) {
        String displayName = section.getString("display-name", categoryId);
        String description = section.getString("description", "");
        String iconName = section.getString("icon", "CHEST");
        int slot = section.getInt("slot", 10);

        Material icon = parseMaterial(iconName, "icon for category " + categoryId);
        if (icon == null) {
            return null;
        }

        // Translate color codes from display-name stored in yml
        displayName = displayName.replace("&", "§");
        description = description.replace("&", "§");

        return new MarketCategory(categoryId, displayName, description, icon, slot);
    }

    private void loadItemsIntoCategory(MarketCategory category, ConfigurationSection catSection) {
        ConfigurationSection itemsSection = catSection.getConfigurationSection("items");
        if (itemsSection == null) {
            return;
        }

        for (String materialName : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(materialName);
            if (itemSection == null) {
                continue;
            }

            Material material = parseMaterial(materialName, "item " + materialName);
            if (material == null) {
                continue;
            }

            String displayName = itemSection.getString("display-name", materialName);
            displayName = displayName.replace("&", "§");
            double basePrice = itemSection.getDouble("base-price", 1.0);

            MarketItem item = new MarketItem(materialName, category.getId(), displayName, material, basePrice);
            category.addItem(item);
        }
    }

    private Material parseMaterial(String name, String context) {
        Material mat = Material.matchMaterial(name);
        if (mat == null) {
            logger.warning("Unknown material '" + name + "' for " + context + " — skipping.");
        }
        return mat;
    }
}
