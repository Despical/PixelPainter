package me.despical.pixelpainter.commands;

import me.despical.commandframework.CommandArguments;
import me.despical.commandframework.Completer;
import me.despical.pixelpainter.Main;
import me.despical.pixelpainter.utils.Direction;
import me.despical.pixelpainter.utils.FileUtils;
import me.despical.pixelpainter.utils.undo.UndoUtils;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class TabCompleter {

	private final Main plugin;

	public TabCompleter(Main plugin) {
		this.plugin = plugin;
		this.plugin.getCommandFramework().registerCommands(this);
	}

	@Completer(
			name = "pp"
	)
	public List<String> onTabComplete(CommandArguments arguments) {
		final List<String> completions = new ArrayList<>(), commands = plugin.getCommandFramework().getCommands().stream().map(cmd -> cmd.name().replace(arguments.getLabel() + '.', "")).collect(Collectors.toList());
		final String args[] = arguments.getArguments(), arg = args[0];

		commands.remove("pp");

		if (args.length == 1) {
			StringUtil.copyPartialMatches(arg, commands, completions);
		}

		List<String> results = Arrays.stream(FileUtils.IMAGES.listFiles()).filter(File::isFile).map(File::getName).collect(Collectors.toList());

		if (arg.equalsIgnoreCase("delete") || arg.equalsIgnoreCase("specs")) {
			return results;
		}

		if (arg.equalsIgnoreCase("create")) {
			if (args.length == 2) {
				return results;
			}

			if (args.length == 3) {
				return Arrays.stream(Direction.values()).map(Direction::getName).collect(Collectors.toList());
			}

			if (args.length == 5) {
				return Arrays.asList("true", "false");
			}

			return null;
		}

		if (arg.equalsIgnoreCase("undo")) {
			List<String> snapshots = new ArrayList<>(UndoUtils.getSnapshots());
			StringUtil.copyPartialMatches(args[0], snapshots, completions);

			snapshots.sort(null);
			return snapshots;
		}

		completions.sort(null);
		return completions;
	}
}