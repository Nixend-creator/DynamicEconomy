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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Loads market categories and items from {@code items.yml}.
 *
 * <p>On every startup the bundled {@code items.yml} version is compared to the
 * on-disk version. If the bundled version is higher (or the file is missing),
 * the file is overwritten so new items always appear after a plugin update.</p>
 *
 * @author n1xend
 * @version 1.2.1
 */
public final class MarketLoader {

    private static final String ITEMS_FILE    = "items.yml";
    private static final String VERSION_KEY   = "config-version";

    private final DynamicEconomy plugin;
    private final Logger         logger;

    public MarketLoader(@NotNull DynamicEconomy plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.logger = plugin.getLogger();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    @NotNull
    public Map<String, MarketCategory> loadCategories() {
        ensureItemsFileUpToDate();

        File itemsFile = new File(plugin.getDataFolder(), ITEMS_FILE);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(itemsFile);

        Map<String, MarketCategory> categories = new LinkedHashMap<>();
        ConfigurationSection catSection = cfg.getConfigurationSection("categories");
        if (catSection == null) {
            logger.warning("No 'categories' section found in items.yml — shop will be empty!");
            return categories;
        }

        int loaded  = 0;
        int skipped = 0;
        for (String catId : catSection.getKeys(false)) {
            ConfigurationSection sec = catSection.getConfigurationSection(catId);
            if (sec == null) continue;

            if (!sec.getBoolean("enabled", true)) {
                skipped++;
                logger.info("Category '" + catId + "' disabled — skipping.");
                continue;
            }

            MarketCategory cat = parseCategory(catId, sec);
            if (cat == null) continue;

            loadItems(cat, sec);
            categories.put(catId, cat);
            loaded++;
            logger.info("Loaded category '" + catId + "' — " + cat.getItems().size() + " items.");
        }

        if (skipped > 0)
            logger.info(skipped + " categor" + (skipped == 1 ? "y" : "ies") + " disabled.");
        logger.info("Total categories loaded: " + loaded);
        return categories;
    }

    // ── File versioning ───────────────────────────────────────────────────────

    /**
     * Copies bundled items.yml to disk if missing or outdated.
     * Version is read from the {@code config-version} key.
     */
    private void ensureItemsFileUpToDate() {
        File diskFile = new File(plugin.getDataFolder(), ITEMS_FILE);

        int bundledVersion = getBundledVersion();
        int diskVersion    = getDiskVersion(diskFile);

        if (!diskFile.exists() || diskVersion < bundledVersion) {
            logger.info("Updating " + ITEMS_FILE
                    + " (disk v" + diskVersion + " → bundled v" + bundledVersion + ")");
            plugin.saveResource(ITEMS_FILE, /* replace= */ true);
        }
    }

    private int getBundledVersion() {
        InputStream is = plugin.getResource(ITEMS_FILE);
        if (is == null) return 0;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(
                new InputStreamReader(is, StandardCharsets.UTF_8));
        return cfg.getInt(VERSION_KEY, 0);
    }

    private int getDiskVersion(@NotNull File file) {
        if (!file.exists()) return -1;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return cfg.getInt(VERSION_KEY, 0);
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private MarketCategory parseCategory(@NotNull String id,
                                          @NotNull ConfigurationSection sec) {
        String rawName = sec.getString("display-name", id);
        String desc    = sec.getString("description", "");
        String iconStr = sec.getString("icon", "CHEST");
        int    slot    = sec.getInt("slot", 10);

        Material icon = parseMaterial(iconStr, "icon for category " + id);
        if (icon == null) return null;

        return new MarketCategory(id,
                rawName.replace("&", "§"),
                desc.replace("&", "§"),
                icon, slot, true);
    }

    private void loadItems(@NotNull MarketCategory cat,
                            @NotNull ConfigurationSection catSec) {
        ConfigurationSection items = catSec.getConfigurationSection("items");
        if (items == null) return;

        for (String matName : items.getKeys(false)) {
            ConfigurationSection iSec = items.getConfigurationSection(matName);
            if (iSec == null) continue;

            Material mat = parseMaterial(matName, "item " + matName + " in " + cat.getId());
            if (mat == null) continue;

            String name  = iSec.getString("display-name", matName).replace("&", "§");
            double price = iSec.getDouble("base-price", 1.0);
            cat.addItem(new MarketItem(matName, cat.getId(), name, mat, price));
        }
    }

    private Material parseMaterial(@NotNull String name, @NotNull String ctx) {
        Material m = Material.matchMaterial(name);
        if (m == null) logger.warning("Unknown material '" + name + "' for " + ctx + " — skipping.");
        return m;
    }
}
