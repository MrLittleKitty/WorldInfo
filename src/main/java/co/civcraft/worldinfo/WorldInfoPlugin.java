package co.civcraft.worldinfo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.io.LineReader;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class WorldInfoPlugin extends JavaPlugin implements PluginMessageListener, Listener {

	public static final byte[] ZERO_BYTES = new byte[0];

	private String encoding;

	private List<String> channels;

	private boolean informPlayer;

	private ID_MODE idMode;

	private Logger log;

	private boolean registered = false;

	public void onEnable() {
		log = getLogger();
		reloadConfigSettings();
		getCommand("reloadWorldInfo").setExecutor(this);
	}

	public void onDisable() {
		unregister();
	}

	public void reloadConfigSettings() {
		unregister();
		FileConfiguration config = getConfig();

		File configFile = new File(getDataFolder(), "config.yml");

		if (!configFile.exists())
			try {
				config.save(configFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		else {
			try {
				config.load(configFile);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
		}
		config.options().copyDefaults(true);

		informPlayer = config.getBoolean("inform-player", false);
		channels = config.getStringList("plugin-channels");
		if (channels.size() == 0) {
			channels.add("world_id"); // Add default
		}

		encoding = config.getString("encoding", "UTF-8");
		idMode = ID_MODE.valueOf(config.getString("mode", ID_MODE.NAME.name()).toUpperCase(Locale.ENGLISH));

		register();
	}

	public void register() {
		registered = true;
		Bukkit.getPluginManager().registerEvents(this, this);

		for (String channel : channels) {
			getServer().getMessenger().registerOutgoingPluginChannel(this, channel);
			getServer().getMessenger().registerIncomingPluginChannel(this, channel, this);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("wuuid.reload")) {
			sender.sendMessage(ChatColor.RED + "You do not have permission!");
			return true;
		}

		reloadConfigSettings();
		sender.sendMessage(ChatColor.GOLD + "Reloaded settings.");
		return true;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldLoad(WorldLoadEvent event) {
		checkWorldFolder(event.getWorld());
	}

	private void checkWorldFolder(World world) {
		File idFile = new File(world.getWorldFolder(), "world.id");
		if (!idFile.exists()) genID(idFile);
	}

	private void genID(File idFile) {
		FileWriter fw = null;
		try {
			boolean newFile = idFile.createNewFile();
			if (newFile) {
				fw = new FileWriter(idFile);
				fw.write(UUID.randomUUID().toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					log.warning("Failed to close file '" + idFile.getPath() + "' from writing...");
					e.printStackTrace();
				}
			}
		}
	}

	public void unregister() {
		if (registered) {
			for (String channel : channels) {
				getServer().getMessenger().unregisterIncomingPluginChannel(this, channel, this);
				getServer().getMessenger().unregisterOutgoingPluginChannel(this, channel);
			}
			HandlerList.unregisterAll((Listener) this);
		}
		registered = false;
	}

	private void sendData(Player player, String channel, byte packetID, byte[] data) {
		ByteBuffer buffer = ByteBuffer.allocate(2 + data.length).put(packetID).put((byte) data.length).put
				(data);
		player.sendPluginMessage(this, channel, buffer.array());
	}

	private String getFileID(World w) {
		String ret = null;
		FileReader fr = null;

		File file = new File(w.getWorldFolder(), "world.id");
		try {
			fr = new FileReader(file);
			LineReader lr = new LineReader(fr);
			ret = lr.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					log.warning("Attempted to close file '" + file.getPath() + "' but failed.");
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	public void onPluginMessageReceived(String channel, Player player,
			byte[] bytes) {
		log.info("Message received from " + player.getName());
		try {
			if (informPlayer) {
				player.sendMessage("Message recieved and sending data on channel " + channel);
			}

			World w = player.getWorld();

			ID_MODE mode = idMode;
			if(bytes.length != 0) {
				try {
					mode = ID_MODE.valueOf(new String(bytes));
				} catch (IllegalArgumentException e) {
					mode = idMode;
				}
			}
			byte[] data = ZERO_BYTES;
			switch (mode) {
				case NAME:
					data = w.getName().getBytes(encoding);
					break;
				case UUID:
					data = w.getUID().toString().getBytes(encoding);
					break;
				case FILE:
					data = getFileID(w).getBytes(encoding);
					break;
			}

			sendData(player, channel, (byte) 0, data);
		} catch (Throwable t) {
			t.printStackTrace();
			if (informPlayer && player.isOnline()) player.sendMessage(new String[]{
					ChatColor.RED + "An error occurred sending world UUID!",
					ChatColor.GRAY + "Please check server logs for details..."
			});
		}
	}

	enum ID_MODE {
		UUID, NAME, FILE
	}

}
