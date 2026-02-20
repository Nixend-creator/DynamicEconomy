package dev.n1xend.dynamiceconomy.data;

import com.google.gson.*;
import dev.n1xend.dynamiceconomy.DynamicEconomy;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import dev.n1xend.dynamiceconomy.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists market item state between server restarts.
 *
 * <p>Uses {@link DatabaseManager} (MySQL or SQLite) when
 * {@code database.enabled: true} in config, otherwise falls back to
 * {@code market_data.json}.</p>
 *
 * @author n1xend
 * @version 1.2.1
 */
public final class DataManager {

    private static final String JSON_FILE = "market_data.json";

    private final DynamicEconomy   plugin;
    private final Logger           logger;
    private final Path             jsonFile;
    private final Gson             gson;
    private final DatabaseManager  db;
    private final boolean          useDb;

    public DataManager(@NotNull DynamicEconomy plugin) {
        this.plugin   = Objects.requireNonNull(plugin);
        this.logger   = plugin.getLogger();
        this.jsonFile = plugin.getDataFolder().toPath().resolve(JSON_FILE);
        this.gson     = new GsonBuilder().setPrettyPrinting().create();

        boolean dbEnabled = plugin.getConfig().getBoolean("database.enabled", false);
        if (dbEnabled) {
            this.db    = new DatabaseManager(plugin);
            this.useDb = db.connect();
            if (!useDb) logger.warning("[DB] Connection failed — using JSON fallback.");
        } else {
            this.db    = null;
            this.useDb = false;
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    public void load() {
        if (useDb) {
            int n = db.loadAll(plugin.getEconomyService().getItemIndex());
            logger.info("[DB] Loaded " + n + " item states from database.");
        } else {
            loadJson();
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    public synchronized void save() {
        if (useDb) {
            db.saveAll(plugin.getEconomyService().getItemIndex().values());
        } else {
            saveJson();
        }
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public void shutdown() {
        save();
        if (db != null) db.disconnect();
    }

    // ── JSON fallback ─────────────────────────────────────────────────────────

    private void loadJson() {
        if (!Files.exists(jsonFile)) {
            logger.info("No market_data.json found — starting with default prices.");
            return;
        }
        try (Reader r = Files.newBufferedReader(jsonFile)) {
            JsonObject root = gson.fromJson(r, JsonObject.class);
            if (root == null) return;
            int loaded = 0;
            for (var entry : root.entrySet()) {
                MarketItem item = plugin.getEconomyService().getItem(entry.getKey());
                if (item == null) continue;
                JsonObject d = entry.getValue().getAsJsonObject();
                if (d.has("multiplier")) item.setCurrentMultiplier(d.get("multiplier").getAsDouble());
                if (d.has("lastSell"))   item.setLastSellTimestamp(d.get("lastSell").getAsLong());
                if (d.has("totalSold"))  item.setTotalSold(d.get("totalSold").getAsLong());
                loaded++;
            }
            logger.info("Loaded " + loaded + " item states from market_data.json.");
        } catch (IOException | JsonParseException e) {
            logger.log(Level.SEVERE, "Failed to load market_data.json", e);
        }
    }

    private void saveJson() {
        try {
            Files.createDirectories(jsonFile.getParent());
            JsonObject root = new JsonObject();
            for (var cat : plugin.getEconomyService().getCategories().values()) {
                for (MarketItem item : cat.getItems()) {
                    JsonObject d = new JsonObject();
                    d.addProperty("multiplier", item.getCurrentMultiplier());
                    d.addProperty("lastSell",   item.getLastSellTimestamp());
                    d.addProperty("totalSold",  item.getTotalSold());
                    root.add(item.getId(), d);
                }
            }
            try (Writer w = Files.newBufferedWriter(jsonFile)) {
                gson.toJson(root, w);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save market_data.json", e);
        }
    }
}
