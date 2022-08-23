package me.despical.pixelpainter;

import me.despical.commandframework.CommandFramework;
import me.despical.commons.compat.VersionResolver;
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

	public static boolean isAbove113 = VersionResolver.isCurrentEqualOrHigher(VersionResolver.ServerVersion.v1_13_R1);
	public Material[] supportedMaterials;

	private CommandFramework commandFramework;

	@Override
	public void onEnable() {
		FileUtils.initializeFiles();

		this.commandFramework = new CommandFramework(this);

		new MainCommands();
		new Metrics(this, 16044);

		loadSupportedMaterials();
		checkForUpdates();
	}

	private void loadSupportedMaterials() {
		RGBBlockColor.activateBlocks();

		if (!getConfig().contains("Custom-Materials-Enabled") || !getConfig().contains("Custom-Materials")) {
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
				} catch (Error | Exception exception) {
					exception.printStackTrace();
				}
			}
		}
	}

	private void checkForUpdates() {
		UpdateChecker.init(this, 104770).requestUpdateCheck().whenComplete((result, exception) -> {
			if (result.requiresUpdate()) {
				log("An update is released, update plugin to " + result.getNewestVersion() + " (current: " + getDescription().getVersion() + ")");
				log("New version: https://www.spigotmc.org/resources/pixel-painter-1-8-1-19.104770/");
			}
		});
	}

	public void log(String message) {
		getLogger().info(message);
	}

	public CommandFramework getCommandFramework() {
		return commandFramework;
	}
}