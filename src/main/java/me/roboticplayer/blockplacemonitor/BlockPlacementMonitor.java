package me.roboticplayer.blockplacemonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class BlockPlacementMonitor extends JavaPlugin implements Listener {

	private FileConfiguration config;
	private int taskID;
	private Map<UUID, Integer> trackerMap;
	private List<Material> blocked;

	@Override
	public void onEnable() {
		config = this.getConfig();
		saveDefaultConfig();
		trackerMap = new HashMap<UUID, Integer>();
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
		if (e.isCancelled())
			return;
		if (p.hasPermission("blockmonitor.bypass"))
			return;
		for (Material mat : blocked) {
			if (e.getBlock().getType() == mat) {
				if (trackerMap.containsKey(p.getUniqueId()))
					trackerMap.put(p.getUniqueId(), trackerMap.get(p.getUniqueId()) + 1);
				else
					trackerMap.put(p.getUniqueId(), 1);

				if (trackerMap.get(p.getUniqueId()) != null
						&& trackerMap.get(p.getUniqueId()) > config.getInt("maxPlacement")) {
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

	private String formatLocation(Location loc) {
		String message = "World: " + loc.getWorld().getName() + ", X: " + loc.getBlockX() + ", Y: " + loc.getBlockY()
				+ ", Z: " + loc.getBlockZ();

		return message;
	}
}
