package me.roboticplayer.blockplacemonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockPlacementMonitor extends JavaPlugin implements Listener {

	private FileConfiguration config;
	private int taskID;
	private Map<UUID, Integer> trackerMap;
	private Map<UUID, Integer> lavaMap;
	private List<Material> blocked;

	@Override
	public void onEnable() {
		config = this.getConfig();
		saveDefaultConfig();
		trackerMap = new HashMap<UUID, Integer>();
		lavaMap = new HashMap<UUID, Integer>();
		clearTimer();
		populateMaterials();
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("BlockPlacementMonitor has been enabled");
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTask(taskID);
		getLogger().info("BlockPlacementMonitor has been disabled");
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		int max = config.getInt("maxPlacement");
		int remind = config.getInt("notifyReminderThreshold");
		if (e.isCancelled())
			return;
		if (p.hasPermission("blockmonitor.bypass"))
			return;
		if (blocked.contains(e.getBlock().getType())) {
			if (trackerMap.containsKey(p.getUniqueId()))
				trackerMap.put(p.getUniqueId(), trackerMap.get(p.getUniqueId()) + 1);
			else
				trackerMap.put(p.getUniqueId(), 1);

			if (trackerMap.get(p.getUniqueId()) >= max) {
				if (trackerMap.get(p.getUniqueId()) == max || (trackerMap.get(p.getUniqueId()) - max) % remind == 0) {
					for (Player staff : getServer().getOnlinePlayers()) {
						if (staff.hasPermission("blockmonitor.notify")) {
							staff.sendMessage(notifyMessage(p, e.getBlock().getLocation()));
						}
					}
					getLogger().info(notifyMessage(p, e.getBlock().getLocation()));
				}
			}
		}

	}

	@EventHandler
	public void onBucketEmpty(PlayerBucketEmptyEvent e) {
		if (!config.getBoolean("monitorLava.enabled"))
			return;
		Player p = e.getPlayer();
		int max = config.getInt("monitorLava.amount");
		int remind = config.getInt("monitorLava.reminder");
		if (e.isCancelled())
			return;
		if (p.hasPermission("blockmonitor.bypass"))
			return;
		if (!e.getBucket().toString().contains("LAVA"))
			return;

		if (lavaMap.containsKey(p.getUniqueId()))
			lavaMap.put(p.getUniqueId(), lavaMap.get(p.getUniqueId()) + 1);
		else
			lavaMap.put(p.getUniqueId(), 1);
		
		if (lavaMap.get(p.getUniqueId()) >= max) {
			if (lavaMap.get(p.getUniqueId()) == max || (lavaMap.get(p.getUniqueId()) - max) % remind == 0) {
				for (Player staff : getServer().getOnlinePlayers()) {
					if (staff.hasPermission("blockmonitor.notify")) {
						staff.sendMessage(lavaMessage(p, e.getBlockClicked().getLocation()));
					}
				}
				getLogger().info(lavaMessage(p, e.getBlockClicked().getLocation()));
			}
		}
	}

	private void populateMaterials() {
		blocked = new ArrayList<Material>();
		for (String mat : config.getStringList("blocks")) {
			if (Material.getMaterial(mat) != null)
				blocked.add(Material.getMaterial(mat));
			else
				getLogger().info("Incorrect material: " + mat);
		}
	}

	private void clearTimer() {
		taskID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				trackerMap.clear();
				lavaMap.clear();
			}
		}, config.getInt("cooldownTime") * 20, config.getInt("cooldownTime") * 20);
	}

	private String notifyMessage(Player p, Location loc) {
		String message = ChatColor.translateAlternateColorCodes('&', config.getString("notifyMessage"));
		message = message.replace("%PLAYER%", p.getName());
		message = message.replace("%NUMBER%", String.valueOf(trackerMap.get(p.getUniqueId())));
		message = message.replace("%TIME%", String.valueOf(config.getInt("cooldownTime")));
		message = message.replace("%LOCATION%", formatLocation(loc));
		return message;
	}

	private String lavaMessage(Player p, Location loc) {
		String message = ChatColor.translateAlternateColorCodes('&', config.getString("monitorLava.message"));
		message = message.replace("%PLAYER%", p.getName());
		message = message.replace("%NUMBER%", String.valueOf(lavaMap.get(p.getUniqueId())));
		message = message.replace("%TIME%", String.valueOf(config.getInt("cooldownTime")));
		message = message.replace("%LOCATION%", formatLocation(loc));
		return message;
	}

	private String formatLocation(Location loc) {
		String message = config.getString("locationFormat");
		message = message.replace("%WORLD%", loc.getWorld().getName());
		message = message.replace("%XCoord%", String.valueOf(loc.getBlockX()));
		message = message.replace("%YCoord%", String.valueOf(loc.getBlockY()));
		message = message.replace("%ZCoord%", String.valueOf(loc.getBlockZ()));

		return message;
	}
}
