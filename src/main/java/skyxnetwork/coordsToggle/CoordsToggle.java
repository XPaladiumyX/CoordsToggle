package skyxnetwork.coordsToggle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CoordsToggle extends JavaPlugin implements Listener, CommandExecutor, TabExecutor, PluginMessageListener {

    private static final String CHANNEL = "coordstoggle:coords";

    private static CoordsToggle instance;
    private File playerDataDir;
    private final ConcurrentHashMap<UUID, Boolean> playerHidden = new ConcurrentHashMap<>();
    private String prefix;
    private boolean floodgateHooked = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfig();

        playerDataDir = new File(getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }

        if (getServer().getPluginManager().isPluginEnabled("floodgate")) {
            floodgateHooked = true;
            getLogger().info("Floodgate detected! Bedrock player detection enabled.");
        } else {
            getLogger().warning("Floodgate not found. Bedrock detection will not work.");
        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);

        getServer().getPluginManager().registerEvents(this, this);
        registerCommand("coordinates");
        registerCommand("coordstoggle");

        getLogger().info("CoordsToggle (Paper) enabled successfully!");
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
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getLogger().info("CoordsToggle disabled!");
    }

    private void loadConfig() {
        reloadConfig();
        prefix = getConfig().getString("Prefix", "§dSky X §9Network §eCoordsToggle §8●⏺ ");
    }

    public void reloadPlugin() {
        loadConfig();
        loadAllPlayerData();
        for (Player player : getServer().getOnlinePlayers()) {
            sendToggleToProxy(player, isCoordinateHidden(player.getUniqueId()));
        }
        getLogger().info("Configuration reloaded!");
    }

    private void sendToggleToProxy(Player player, boolean hidden) {
        if (!floodgateHooked) return;
        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) return;

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(player.getUniqueId().toString());
            dos.writeBoolean(hidden);
            player.sendPluginMessage(this, CHANNEL, bos.toByteArray());
        } catch (IOException e) {
            getLogger().warning("Failed to send plugin message: " + e.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
    }

    private File getPlayerFile(UUID uuid) {
        return new File(playerDataDir, uuid.toString() + ".yml");
    }

    private void loadAllPlayerData() {
        playerHidden.clear();
        File[] files = playerDataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                playerHidden.put(uuid, config.getBoolean("hidden", false));
            } catch (Exception e) {
                getLogger().warning("Failed to load player data: " + file.getName());
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
        playerHidden.keySet().forEach(this::savePlayerData);
    }

    private void loadPlayerData(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            playerHidden.put(uuid, false);
            return;
        }
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            playerHidden.put(uuid, config.getBoolean("hidden", false));
        } catch (Exception e) {
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

        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline() && isCoordinateHidden(uuid)) {
                sendToggleToProxy(player, true);
            }
        }, 40L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        savePlayerData(uuid);
        playerHidden.remove(uuid);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!isCoordinateHidden(uuid)) return;

        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                sendToggleToProxy(player, true);
            }
        }, 10L);
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("coordstoggle") || cmdName.equals("ctreload")) {
            if (!sender.hasPermission("coords.toggle.reload")) {
                sender.sendMessage(prefix + "§cYou don't have permission!");
                return true;
            }
            reloadPlugin();
            sender.sendMessage(prefix + "§aConfiguration reloaded!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cThis command can only be used by a player!");
            return true;
        }

        if (!player.hasPermission("coords.toggle.use")) {
            player.sendMessage(prefix + "§cYou don't have permission!");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (floodgateHooked && !FloodgateApi.getInstance().isFloodgatePlayer(uuid)) {
            player.sendMessage(prefix + "§eYou are playing on §bJava Edition§e. Press §bF3§e to see your coordinates.");
            return true;
        }

        boolean newHidden = !isCoordinateHidden(uuid);
        playerHidden.put(uuid, newHidden);
        savePlayerData(uuid);
        sendToggleToProxy(player, newHidden);

        if (newHidden) {
            player.sendMessage(prefix + "§aCoordinates §chidden§a! Anti stream-snipe §aenabled");
        } else {
            player.sendMessage(prefix + "§aCoordinates §edisplayed§a!");
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