package dev.n1xend.dynamiceconomy.data;

import com.google.gson.*;
import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles JSON persistence of market item state between server restarts.
 *
 * <p>Saves and loads: current price multiplier, last sell timestamp,
 * and total sold statistics for every market item.</p>
 *
 * <p>{@link #save()} is thread-safe and can be called from async contexts.</p>
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public class DataManager {

    private static final String DATA_FILE_NAME = "market_data.json";

    private final DynamicEconomy plugin;
    private final Logger logger;
    private final Path dataFile;
    private final Gson gson;

    public DataManager(@NotNull DynamicEconomy plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataFile = plugin.getDataFolder().toPath().resolve(DATA_FILE_NAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    /**
     * Loads market item states from {@code market_data.json}.
     * Called once on startup after EconomyService has registered all items.
     * Missing items in JSON are silently skipped (new items use default multiplier).
     */
    public void load() {
        if (!Files.exists(dataFile)) {
            logger.info("No market_data.json found — starting with default prices.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(dataFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }

            int loaded = 0;
            for (var entry : root.entrySet()) {
                String materialId = entry.getKey();
                MarketItem item = plugin.getEconomyService().getItem(materialId);
                if (item == null) {
                    continue;
                }

                JsonObject data = entry.getValue().getAsJsonObject();
                if (data.has("multiplier")) {
                    item.setCurrentMultiplier(data.get("multiplier").getAsDouble());
                }
                if (data.has("lastSell")) {
                    item.setLastSellTimestamp(data.get("lastSell").getAsLong());
                }
                if (data.has("totalSold")) {
                    item.setTotalSold(data.get("totalSold").getAsLong());
                }
                loaded++;
            }

            logger.info("Loaded market data for " + loaded + " items.");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load market_data.json", e);
        } catch (JsonParseException e) {
            logger.log(Level.SEVERE, "Corrupted market_data.json — resetting prices", e);
        }
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    /**
     * Saves all market item states to {@code market_data.json}.
     *
     * <p>Thread-safe — synchronized to prevent concurrent writes during async auto-save
     * and synchronous onDisable.</p>
     */
    public synchronized void save() {
        try {
            Files.createDirectories(dataFile.getParent());

            JsonObject root = new JsonObject();

            for (var category : plugin.getEconomyService().getCategories().values()) {
                for (MarketItem item : category.getItems()) {
                    JsonObject data = new JsonObject();
                    data.addProperty("multiplier", item.getCurrentMultiplier());
                    data.addProperty("lastSell", item.getLastSellTimestamp());
                    data.addProperty("totalSold", item.getTotalSold());
                    root.add(item.getId(), data);
                }
            }

            try (Writer writer = Files.newBufferedWriter(dataFile)) {
                gson.toJson(root, writer);
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save market_data.json", e);
        }
    }
}
