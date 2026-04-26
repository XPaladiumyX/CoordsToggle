package skyxnetwork.coordsToggle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

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
    private boolean geyserHooked = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfig();

        playerDataDir = new File(getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }

        if (getServer().getPluginManager().isPluginEnabled("Geyser-Spigot")) {
            geyserHooked = true;
            getLogger().info("Geyser-Spigot detecte! Masquage des coordonnees Bedrock active.");
        } else {
            getLogger().warning("Geyser-Spigot introuvable. Le masquage Bedrock ne fonctionnera pas.");
        }

        getServer().getPluginManager().registerEvents(this, this);
        registerCommand("coordinates");
        registerCommand("coordstoggle");

        getLogger().info("CoordsToggle active avec succes!");
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
        getLogger().info("CoordsToggle desactive!");
    }

    private void loadConfig() {
        reloadConfig();
        prefix = getConfig().getString("Prefix", "§dSky X §9Network §eCoordsToggle §8●⏺ ");
    }

    public void reloadPlugin() {
        loadConfig();
        loadAllPlayerData();
        for (Player player : getServer().getOnlinePlayers()) {
            sendGeyserPacketDirect(player.getUniqueId(), isCoordinateHidden(player.getUniqueId()));
        }
        getLogger().info("Configuration rechargee!");
    }

    private void sendGeyserPacketDirect(UUID uuid, boolean hidden) {
        if (!geyserHooked) return;

        try {
            GameRulesChangedPacket packet = new GameRulesChangedPacket();
            packet.getGameRules().add(new GameRuleData<>("showCoordinates", !hidden));

            GeyserConnection connection = GeyserApi.api().connectionByUuid(uuid);
            if (connection == null) return;

            var sessionField = connection.getClass().getDeclaredField("session");
            sessionField.setAccessible(true);
            Object session = sessionField.get(connection);

            if (session != null) {
                var method = session.getClass().getMethod("sendUpstreamPacket", BedrockPacket.class);
                method.invoke(session, packet);
            }
        } catch (Exception e) {
            getLogger().warning("Erreur envoi packet: " + e.getMessage());
        }
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
                getLogger().warning("Echec du chargement des donnees: " + file.getName());
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
            getLogger().warning("Echec de la sauvegarde pour " + uuid + ": " + e.getMessage());
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
            getLogger().warning("Echec du chargement pour " + uuid);
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
                sendGeyserPacketDirect(uuid, true);
            }
        }, 20L);
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
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("coordstoggle") || cmdName.equals("ctreload")) {
            if (!sender.hasPermission("coords.toggle.reload")) {
                sender.sendMessage(prefix + "§cVous n'avez pas la permission!");
                return true;
            }
            reloadPlugin();
            sender.sendMessage(prefix + "§aConfiguration rechargee!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cCette commande ne peut etre utilisee que par un joueur!");
            return true;
        }

        if (!player.hasPermission("coords.toggle.use")) {
            player.sendMessage(prefix + "§cVous n'avez pas la permission!");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (geyserHooked && GeyserApi.api().connectionByUuid(uuid) == null) {
            player.sendMessage(prefix + "§eVous jouez en §bJava§e. Appuyez sur §bF3§e pour voir vos coordonnees.");
            return true;
        }

        boolean newHidden = !isCoordinateHidden(uuid);
        playerHidden.put(uuid, newHidden);
        savePlayerData(uuid);
        sendGeyserPacketDirect(uuid, newHidden);

        if (newHidden) {
            player.sendMessage(prefix + "§aCoordonnees §ccachees§a! Anti stream-snipe §aactive");
        } else {
            player.sendMessage(prefix + "§aCoordonnees §eaffichees§a!");
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