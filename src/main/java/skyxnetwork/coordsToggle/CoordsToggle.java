package skyxnetwork.coordsToggle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CoordsToggle extends JavaPlugin implements Listener, CommandExecutor, TabExecutor {

    private static CoordsToggle instance;
    private File playerDataDir;
    private final ConcurrentHashMap<UUID, Boolean> playerHidden = new ConcurrentHashMap<>();
    private String prefix;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadConfig();

        playerDataDir = new File(getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }

        getServer().getPluginManager().registerEvents(this, this);

        registerCommand("coordinates");
        registerCommand("coords");
        registerCommand("coordstoggle");

        getLogger().info("CoordsToggle enabled successfully!");
    }

    private void registerCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    @Override
    public void onDisable() {
        saveAllPlayerData();
        getLogger().info("CoordsToggle disabled!");
    }

    private void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        prefix = config.getString("Prefix", "§dSky X §9Network §eCoordsToggle §8●⏺ ");
    }

    public void reloadPlugin() {
        loadConfig();
        loadAllPlayerData();
        for (Player player : getServer().getOnlinePlayers()) {
            if (isCoordinateHidden(player.getUniqueId())) {
                player.addScoreboardTag("coords_hidden");
            } else {
                player.removeScoreboardTag("coords_hidden");
            }
        }
        getLogger().info("Plugin configuration reloaded!");
    }

    private File getPlayerFile(UUID uuid) {
        return new File(playerDataDir, uuid.toString() + ".yml");
    }

    private void loadAllPlayerData() {
        playerHidden.clear();
        File[] files = playerDataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                    boolean hidden = config.getBoolean("hidden", false);
                    playerHidden.put(uuid, hidden);
                } catch (Exception e) {
                    getLogger().warning("Failed to load player data: " + file.getName());
                }
            }
        }
    }

    private void savePlayerData(UUID uuid) {
        Boolean hidden = playerHidden.get(uuid);
        if (hidden == null) return;

        File file = getPlayerFile(uuid);
        YamlConfiguration config = new YamlConfiguration();
        config.set("hidden", hidden);

        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().warning("Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }

    private void saveAllPlayerData() {
        for (UUID uuid : playerHidden.keySet()) {
            savePlayerData(uuid);
        }
    }

    private void loadPlayerData(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            playerHidden.put(uuid, false);
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            boolean hidden = config.getBoolean("hidden", false);
            playerHidden.put(uuid, hidden);
        } catch (Exception e) {
            getLogger().warning("Failed to load player data for " + uuid + ": " + e.getMessage());
            playerHidden.put(uuid, false);
        }
    }

    public boolean isCoordinateHidden(UUID uuid) {
        return playerHidden.getOrDefault(uuid, false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        loadPlayerData(uuid);

        if (isCoordinateHidden(uuid)) {
            player.addScoreboardTag("coords_hidden");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        savePlayerData(uuid);
        playerHidden.remove(uuid);
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();

        if (commandName.equals("coordstoggle") || commandName.equals("ctreload")) {
            if (!sender.hasPermission("coords.toggle.reload")) {
                sender.sendMessage(prefix + "§cYou don't have permission to use this command!");
                return true;
            }
            reloadPlugin();
            sender.sendMessage(prefix + "§aPlugin configuration reloaded!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cThis command can only be used by players!");
            return true;
        }

        UUID uuid = player.getUniqueId();
        boolean currentlyHidden = isCoordinateHidden(uuid);

        boolean newState = !currentlyHidden;
        playerHidden.put(uuid, newState);
        savePlayerData(uuid);

        if (newState) {
            player.addScoreboardTag("coords_hidden");
            player.sendMessage(prefix + "§aCoordinates display has been §chidden§a!");
        } else {
            player.removeScoreboardTag("coords_hidden");
            player.sendMessage(prefix + "§aCoordinates display has been §eenabled§a!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }

    public static CoordsToggle getInstance() {
        return instance;
    }

    public String getPrefix() {
        return prefix;
    }
}