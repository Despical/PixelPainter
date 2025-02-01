package me.despical.pixelpainter;

import me.despical.commons.reflection.XReflection;
import me.despical.commons.util.UpdateChecker;
import me.despical.pixelpainter.commands.MainCommands;
import me.despical.pixelpainter.utils.FileUtils;
import me.despical.pixelpainter.utils.RGBBlockColor;
import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class Main extends JavaPlugin {

	public static boolean isAbove113 = XReflection.supports(13);

	private Material[] supportedMaterials;

	@Override
	public void onEnable() {
		FileUtils.initializeFiles();

		new MainCommands(this);
		new Metrics(this, 16044);

		loadSupportedMaterials();
		checkForUpdates();

		getLogger().info("Initialization finished.");
		getLogger().info("Join our Discord server: https://discord.gg/uXVU8jmtpU");
	}

	private void loadSupportedMaterials() {
		RGBBlockColor.activateBlocks();

		if (!getConfig().isSet("Custom-Materials-Enabled") || !getConfig().isSet("Custom-Materials")) {
			supportedMaterials = null;

			getConfig().set("Custom-Materials-Enabled", false);
			getConfig().set("Custom-Materials", Arrays.asList("STONE", "NETHERRACK"));

			List<String> fullNames = new ArrayList<>();

			for (Material material : Material.values()) {
				if (material.isBlock()) fullNames.add(material.name());
			}

			getConfig().set("FULL_MATERIAL_LIST", fullNames);

			saveConfig();
		} else if (getConfig().getBoolean("Custom-Materials-Enabled")) {
			List<String> whitelistedMaterials = getConfig().getStringList("Custom-Materials");
			supportedMaterials = new Material[whitelistedMaterials.size()];

			int i = 0;

			for (String material : whitelistedMaterials) {
				try {
					supportedMaterials[i++] = Material.matchMaterial(material);
				} catch (Throwable exception) {
					exception.printStackTrace();
				}
			}
		}
	}

	private void checkForUpdates() {
		UpdateChecker.init(this, 104770).onNewUpdate(result -> {
			log("An update is released, update plugin to " + result.getNewestVersion() + " (current: " + getDescription().getVersion() + ")");
			log("New version: https://spigotmc.org/resources/104770");
		});
	}

	public void log(String message) {
		getLogger().info(message);
	}

	public Material[] getSupportedMaterials() {
		return supportedMaterials;
	}
}