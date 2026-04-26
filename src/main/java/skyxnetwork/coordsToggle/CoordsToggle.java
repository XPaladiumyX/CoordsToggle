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

    // Canal plugin message entre Paper et le plugin Velocity
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

        // On utilise Floodgate (présent sur Paper) pour détecter les joueurs Bedrock
        if (getServer().getPluginManager().isPluginEnabled("floodgate")) {
            floodgateHooked = true;
            getLogger().info("Floodgate detecte! Detection joueurs Bedrock active.");
        } else {
            getLogger().warning("Floodgate introuvable. La detection Bedrock ne fonctionnera pas.");
        }

        // Enregistre le canal plugin message vers Velocity (outgoing)
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        // Incoming au cas où Velocity répond (optionnel, pour debug futur)
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);

        getServer().getPluginManager().registerEvents(this, this);
        registerCommand("coordinates");
        registerCommand("coordstoggle");

        getLogger().info("CoordsToggle (Paper) active avec succes!");
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
            sendToggleToProxy(player, isCoordinateHidden(player.getUniqueId()));
        }
        getLogger().info("Configuration rechargee!");
    }

    /**
     * Envoie un plugin message à Velocity.
     * Le message contient : UUID (string) + "|" + état (true/false).
     * Velocity intercepte ce message et envoie le GameRulesChangedPacket au client Bedrock.
     */
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
            getLogger().warning("Erreur envoi plugin message: " + e.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // Réservé pour éventuels retours de Velocity (non utilisé actuellement)
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

        // Délai 40 ticks (2s) : laisser le temps à Velocity/Geyser d'établir la session
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

        // Joueur Java Edition → F3, pas besoin du plugin
        if (floodgateHooked && !FloodgateApi.getInstance().isFloodgatePlayer(uuid)) {
            player.sendMessage(prefix + "§eVous jouez en §bJava Edition§e. Utilisez §bF3§e pour vos coordonnees.");
            return true;
        }

        boolean newHidden = !isCoordinateHidden(uuid);
        playerHidden.put(uuid, newHidden);
        savePlayerData(uuid);
        sendToggleToProxy(player, newHidden);

        if (newHidden) {
            player.sendMessage(prefix + "§aCoordonnees §ccachees§a! Anti stream-snipe §aactive ✔");
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