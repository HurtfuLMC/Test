package test.test;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Test extends JavaPlugin implements Listener {
    private static final String REPO_OWNER = "HurtfuLMC";
    private static final String REPO_NAME = "Test";
    private static final String ACCESS_TOKEN = "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";
    private static final String FILENAME_REGEX = "Test-.*\\.jar"; // replace with your plugin filename pattern

    private static final String API_ENDPOINT = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String AUTHORIZATION = "Bearer " + ACCESS_TOKEN;
    private static final String PLUGIN_FILENAME = "MendingXP.jar";

    private boolean updateAvailable;

    private int xpCost;

    @Override
    public void onEnable() {
        checkForUpdates();
        getServer().getPluginManager().registerEvents(this, this);
        loadConfig();
        this.saveDefaultConfig();
    }

    private void checkForUpdates() {
        try {
            URL url = new URL(API_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Authorization", AUTHORIZATION);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            JSONObject json = new JSONObject(response.toString());
            String downloadUrl = "";
            JSONArray assets = json.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String filename = asset.getString("name");
                if (filename.matches(FILENAME_REGEX)) {
                    downloadUrl = asset.getString("browser_download_url");
                    break;
                }
            }

            if (!downloadUrl.isEmpty()) {
                URL download = new URL(downloadUrl);
                Path pluginPath = Paths.get(getDataFolder().getParentFile().getPath(), PLUGIN_FILENAME);
                Files.copy(download.openStream(), pluginPath, StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("Plugin updated successfully.");
            } else {
                getLogger().info("Plugin is up to date.");
            }
        } catch (IOException | JSONException e) {
            getLogger().warning("Failed to check for updates: " + e.getMessage());
        }
    }


    @Override
    public void onDisable() {
        getLogger().info("MendingXP disabled.");
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().contains(Material.ENCHANTED_BOOK)) {
            ItemStack[] inventory = player.getInventory().getContents();
            for (ItemStack item : inventory) {
                if (item != null && item.getEnchantments().containsKey(Enchantment.MENDING)) {
                    if (item.getDurability() > 0 && player.getTotalExperience() >= xpCost) {
                        item.setDurability((short) (item.getDurability() - 1));
                        player.giveExp(-xpCost);
                    }
                }
            }
        }
    }


    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        xpCost = config.getInt("xpCost", 3);
    }
}
