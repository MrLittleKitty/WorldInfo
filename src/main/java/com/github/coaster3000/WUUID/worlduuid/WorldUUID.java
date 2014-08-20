package com.github.coaster3000.WUUID.worlduuid;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class WorldUUID extends JavaPlugin implements PluginMessageListener {

	private String encoding;

	public void onEnable() {
		reloadConfigSettings();
	}

	public void onDisable() {
		unregister();
	}

	public void reloadConfigSettings() {
		unregister();
		FileConfiguration config = getConfig();
		config.options().copyDefaults(true);

		informPlayer = config.getBoolean("inform-player", false);
		channel = config.getString("plugin-channel", "world_uid");
		encoding = config.getString("encoding", "UTF-8");

		register();
	}

	private boolean registered = false;
	private String  channel;
	private boolean informPlayer;

	public void register() {
		registered = true;
		Bukkit.getMessenger().registerIncomingPluginChannel(this, channel, this);
	}

	public void unregister() {
		if (registered) Bukkit.getMessenger().unregisterIncomingPluginChannel(this, channel, this);
		registered = false;
	}

	public void onPluginMessageReceived(String channel, Player player,
			byte[] bytes) {
		if (!channel.equals(this.channel) && verify(bytes)) {
			return;
		}
		try {

			byte[] data = player.getWorld().getUID().toString().getBytes(encoding);
			player.sendPluginMessage(this, channel, data);
		} catch (Throwable t) {
			if (informPlayer) {
				player.sendMessage(
						ChatColor.RED + "An error occurred sending world UUID!");
				player.sendMessage(
						ChatColor.GRAY + "Please check server logs for details...");
			}
			t.printStackTrace();
		}
	}

	private boolean verify(byte[] bytes) { //Used to provide checks to protocol. However currently unimplemented.
		return true;
	}
}
