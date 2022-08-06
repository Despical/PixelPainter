package me.despical.pixelpainter.utils.data;

import me.despical.pixelpainter.Main;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class DataHolder implements ConfigurationSerializable {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	public MaterialData materialData;
	public Location location;

	private boolean hasFaces = false;

	public DataHolder(Location location, MaterialData materialData) {
		this.location = location;
		this.materialData = materialData;
	}

	public DataHolder(Location b, MaterialData md, boolean hasFaces) {
		this.location = b;
		this.materialData = md;
		this.hasFaces = hasFaces;
	}

	public DataHolder(Map<String, Object> data) {
		final Map<String, Object> tempData = data;

		new BukkitRunnable() {

			@Override
			public void run() {
				if (plugin.getServer().getWorld((String) tempData.get("b.w")) != null) {
					location = new Location(plugin.getServer().getWorld((String) tempData.get("b.w")), (int) tempData.get("b.x"), (int) tempData.get("b.y"), (int) tempData.get("b.z"));
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 20);

		this.materialData = (MaterialData) data.get("md");
	}

	@NotNull
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> data = new HashMap<>();
		data.put("b.x", this.location.getBlockX());
		data.put("b.y", this.location.getBlockY());
		data.put("b.z", this.location.getBlockZ());
		data.put("b.w", this.location.getWorld().getName());
		data.put("md", this.materialData);
		return data;
	}

	public boolean hasFaces() {
		return hasFaces;
	}
}