package me.despical.pixelpainter.commands;

import me.despical.commandframework.CommandArguments;
import me.despical.commandframework.CommandFramework;
import me.despical.commandframework.annotations.Command;
import me.despical.commandframework.annotations.Completer;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.util.Strings;
import me.despical.pixelpainter.Main;
import me.despical.pixelpainter.utils.data.AtomicHolder;
import me.despical.pixelpainter.utils.image.AsyncImageLoader;
import me.despical.pixelpainter.utils.Direction;
import me.despical.pixelpainter.utils.FileUtils;
import me.despical.pixelpainter.utils.RGBBlockColor;
import me.despical.pixelpainter.utils.undo.UndoUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class MainCommands implements CommandImpl {

	private final Main plugin;
	private final CommandFramework commandFramework;

	public MainCommands(Main plugin) {
		this.plugin = plugin;
		this.commandFramework = new CommandFramework(plugin);

		commandFramework.registerCommands(this);
	}

	@Command(
		name = "pp",
		fallbackPrefix = "pixelpainter",
		usage = "/pp",
		desc = "Main command of the plugin."
	)
	public void ppCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage("&3This server is running &bPixel Painter v{0} &3by &bDespical&3.", plugin.getDescription().getVersion());

			if (arguments.hasPermission("pp.help")) {
				arguments.sendMessage("&3Commands: &b/{0} help", arguments.getLabel());
			}

			return;
		}

		arguments.sendMessage("&cUnrecognized arguments: /{0} {1}", arguments.getLabel(), arguments.concatArguments());
	}

	@Command(
		name = "pp.create",
		permission = "pp.create",
		usage = "/pp create <image file> <direction> <height>",
		desc = "Creates a in-game image from specified image file in the specified direction and height.",
		senderType = Command.SenderType.PLAYER
	)
	public void createCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		int length = arguments.getLength();

		if (length == 0) {
			sendMessage(player, "Please provide a image file!");
			return;
		}

		String imageName = arguments.getArgument(0);

		if (!FileUtils.checkIfImageExists(imageName)) {
			sendMessage(player, "&cNo image found called %s in the images folder.", imageName);
			return;
		}

		if (length == 1) {
			sendMessage(player, "Please provide a direction!");
			return;
		}

		String directionName = arguments.getArgument(1);
		Direction direction = Direction.getDirection(directionName);

		if (direction == null) {
			sendMessage(player, "Please provide a valid direction!");
			return;
		}

		if (length == 2) {
			sendMessage(player, "Please provide a height!");
			return;
		}

		int height = arguments.getArgumentAsInt(2);

		if (height > player.getWorld().getMaxHeight() && !directionName.contains("Flat")) {
			sendMessage(player, "Specified height (%d) is greater than world's maximum height value, this can cause image can not be rendered at all.", height);
		}

		boolean enableTrans = length > 3 && Boolean.parseBoolean(arguments.getArgument(3));
		File loadedImage = FileUtils.getImageFile(imageName);

		try {
			createImage(player, ImageIO.read(loadedImage), direction, height, enableTrans, loadedImage.getName());
		} catch (Exception exception) {
			sendMessage(player, "&cSomething gone wrong while reading the image file! Please check console logs for details.");
		}
	}

	@Command(
		name = "pp.undo",
		permission = "pp.undo",
		usage = "/pp undo <loaded image>",
		desc = "Undo the loaded image.",
		senderType = Command.SenderType.PLAYER
	)
	public void undoImageCommand(CommandArguments arguments) {
		Player player = arguments.getSender();

		if (arguments.isArgumentsEmpty()) {
			sendMessage(player, "Please provide a image name to undo!");
			return;
		}

		String imageName = arguments.getArgument(0);

		if (!UndoUtils.snapshotExists(imageName)) {
			sendMessage(player, "&cNo loaded image found called %s!", imageName);
			return;
		}

		UndoUtils.undo(imageName);

		sendMessage(player, "Image has been undone. If this was a mistake, or wish to have that image again, resend the command");
	}

	@Command(
		name = "pp.howmanyblocks",
		permission = "pp.howmanyblocks",
		usage = "/pp howmanyblocks <image name> <height>",
		desc = "Show how many blocks needed for this image to be generated.",
		senderType = Command.SenderType.PLAYER
	)
	public void howManyBlocksCommand(CommandArguments arguments) {
		Player player = arguments.getSender();

		if (arguments.isArgumentsEmpty()) {
			sendMessage(player, "Please provide a image name to undo!");
			return;
		}

		String imageName = arguments.getArgument(0);

		if (!FileUtils.checkIfImageExists(imageName)) {
			sendMessage(player, "&cNo image found called %s in the images folder.", imageName);
			return;
		}

		if (arguments.getLength() == 1) {
			sendMessage(player, "Please provide a height!");
			return;
		}

		int height = arguments.getArgumentAsInt(1);

		if (height > player.getWorld().getMaxHeight()) {
			sendMessage(player, "Specified height (%d) is greater than world's maximum height value, this can cause image can not be rendered at all.", height);
		}

		try {
			File loadedImage = FileUtils.getImageFile(imageName);

			BufferedImage bufferedImage = ImageIO.read(loadedImage);
			bufferedImage = RGBBlockColor.resize(bufferedImage, (int) (bufferedImage.getWidth() * (((double) height) * 2 / bufferedImage.getHeight())), height * 2);

			int imageWidth = bufferedImage.getWidth(), imageHeight = bufferedImage.getHeight();

			sendMessage(player, "This image needs %dx%d (%d) blocks to be generated.", imageWidth, imageHeight, imageWidth * imageHeight);
		} catch (Exception exception) {
			sendMessage(player, "&cSomething went wrong while we were calculating size of the blocks!");
		}
	}

	@Command(
		name = "pp.download",
		permission = "pp.download",
		usage = "/pp download <image name> <image URL>",
		desc = "Download image to load.",
		senderType = Command.SenderType.PLAYER
	)
	public void downloadImageCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		int length = arguments.getLength();

		if (length == 0) {
			sendMessage(player, "Please provide a image name to save the file.");
			return;
		}

		String fileName = arguments.getArgument(0);

		if (FileUtils.checkIfImageExists(fileName)) {
			sendMessage(player, "&cThere is already an image file called %s in the directory!", fileName);
			return;
		}

		if (length == 1) {
			sendMessage(player, "Please provide a download link!");
			return;
		}

		String link = arguments.getArgument(1);

		if (fileName != null && (!fileName.endsWith("png") || !fileName.endsWith("jpg"))) {
			fileName += ".jpg";
		}

		AtomicHolder holder = new AtomicHolder();
		holder.setString(fileName);

		new BukkitRunnable() {

			@Override
			public void run() {
				try {
					String fileName = holder.getString();

					try {
						URL url = new URL(link);

						sendMessage(player, "Downloading image (%s) requested by %s...", fileName, player.getName());

						FileUtils.downloadImage(url, fileName, percentage -> {
							if (percentage % 20 == 0) sendMessage(player, "Downloading " + percentage + "%");
						});
					} catch (MalformedURLException exception) {
						sendMessage(player, "Please provide a valid link!");
						return;
					}

					sendMessage(player, "Downloaded image file to %s file in the directory.", fileName);
				} catch (Exception exception) {
					exception.printStackTrace();

					sendMessage(player, "Something gone wrong! Check console logs for details!");
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	@Command(
		name = "pp.delete",
		permission = "pp.delete",
		usage = "/pp delete <image file>",
		desc = "Delete the specified image file from images directory.",
		senderType = Command.SenderType.PLAYER
	)
	public void deleteImageFileCommand(CommandArguments arguments) {
		Player player = arguments.getSender();

		if (arguments.isArgumentsEmpty()) {
			sendMessage(player, "Please provide a image name to delete!");
			return;
		}

		String fileName = arguments.getArgument(0);

		if (!FileUtils.checkIfImageExists(fileName)) {
			sendMessage(player, "&cThere is no image file called %s in the directory!", fileName);
			return;
		}

		File file = FileUtils.getImageFile(fileName);
		sendMessage(player, file.delete() ? "The image file called %s is successfully deleted." :
			"&cSomething gone wrong while deleting the image file!", fileName);
	}

	@Command(
		name = "pp.list",
		permission = "pp.list",
		usage = "/pp list",
		desc = "Get a list of existing image files in the directory.",
		senderType = Command.SenderType.PLAYER
	)
	public void listFilesCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		List<String> files = new ArrayList<>();

		for (File file : FileUtils.IMAGES.listFiles()) {
			if (file.isFile()) {
				files.add(file.getName());
			}
		}

		sendMessage(player, files.isEmpty() ? "The images folder is empty!" :
			"Existing image files: &r" + String.join(", ", files));
	}

	@Command(
		name = "pp.help",
		usage = "/pp help",
		desc = "Displays a list of available commands along with their descriptions.",
		permission = "pp.help"
	)
	public void helpCommand(CommandArguments arguments) {
		arguments.sendMessage("");
		MiscUtils.sendCenteredMessage(arguments.getSender(), "&3&lPixel Painter");
		MiscUtils.sendCenteredMessage(arguments.getSender(), "&3[&boptional argument&3] &b- &3<&brequired argument&3>");
		arguments.sendMessage("");

		CommandSender sender = arguments.getSender();
		boolean isPlayer = arguments.isSenderPlayer();

		for (Command command : commandFramework.getAllCommands()) {
			String usage = command.usage(), desc = command.desc();

			if (desc.isEmpty()) continue;

			if (isPlayer) {
				((Player) sender).spigot().sendMessage(new ComponentBuilder(ChatColor.DARK_GRAY + " • ")
					.append(formatCommandUsage("&3" + usage))
					.color(ChatColor.DARK_AQUA)
					.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, usage))
					.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(desc)))
					.create());
			} else {
				arguments.sendMessage(" &8• &3" + formatCommandUsage("&3" + usage) + " &3- &b" + desc);
			}
		}

		if (isPlayer) {
			Player player = arguments.getSender();

			player.sendMessage("");
			player.spigot().sendMessage(new ComponentBuilder("TIP:").color(ChatColor.YELLOW).bold(true)
				.append(" Try to ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GRAY)
				.append("hover").color(ChatColor.WHITE).underlined(true)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "Hover on the commands to get info about them.")))
				.append(" or ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GRAY)
				.append("click").color(ChatColor.WHITE).underlined(true)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "Click on the commands to insert them in the chat.")))
				.append(" on the commands!", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GRAY)
				.create());
		}
	}

	@Command(
		name = "pp.specs",
		permission = "pp.specs",
		usage = "/pp specs <image file>",
		desc = "Get image's width and height values.",
		senderType = Command.SenderType.PLAYER
	)
	public void imageSpecsCommand(CommandArguments arguments) {
		Player player = arguments.getSender();

		if (arguments.isArgumentsEmpty()) {
			sendMessage(player, "Please provide a image name to save the file.");
			return;
		}

		String fileName = arguments.getArgument(0);

		if (!FileUtils.checkIfImageExists(fileName)) {
			sendMessage(player, "&cThere is no image file called %s in the directory!", fileName);
			return;
		}

		File image = FileUtils.getImageFile(fileName);

		int width, height;
		BufferedImage bufferedImage;

		try {
			bufferedImage = ImageIO.read(image);
			width = bufferedImage.getWidth();
			height = bufferedImage.getHeight();

			sendMessage(player, "Specs for the image (%s):", fileName);
			sendMessage(player, "  Width: %d", width);
			sendMessage(player, "  Height: %d", height);
		} catch (IOException exception) {
			sendMessage(player, "Something gone wrong while checking for the image file called %s!", fileName);
		}
	}

	private void createImage(Player player, BufferedImage bufferedImage, Direction direction, int height, boolean enableTrans, String name) {
		bufferedImage = RGBBlockColor.resize(bufferedImage, (int) (bufferedImage.getWidth() * (((double) height) * 2 / bufferedImage.getHeight())), height * 2);
		RGBBlockColor.Pixel[][] result = RGBBlockColor.convertTo2DWithoutUsingGetRGB(bufferedImage);

		new AsyncImageLoader(plugin, name, result, player, player.getLocation().clone(), direction, bufferedImage, enableTrans).loadImage(true);
	}

	@Completer(
		name = "pp"
	)
	public List<String> onTabComplete(CommandArguments arguments) {
		if (!arguments.hasPermission("pp.admin")) return null;

		List<String> completions = new ArrayList<>(), commands = commandFramework.getSubCommands().stream().map(cmd -> cmd.name().replace(arguments.getLabel() + '.', "")).collect(Collectors.toList());
		String args[] = arguments.getArguments(), arg = args[0];

		if (args.length == 1) {
			StringUtil.copyPartialMatches(arg, commands, completions);
			return completions;
		}

		List<String> results = Arrays.stream(FileUtils.IMAGES.listFiles()).filter(File::isFile).map(File::getName).collect(Collectors.toList());

		if (args.length == 2) {
			if (arg.equalsIgnoreCase("delete") || arg.equalsIgnoreCase("specs")) {
				StringUtil.copyPartialMatches(args[1], results, completions);
				return completions;
			}

			if (arg.equalsIgnoreCase("howmanyblocks")) {
				return results;
			}

			if (arg.equalsIgnoreCase("undo")) {
				List<String> snapshots = new ArrayList<>(UndoUtils.getSnapshots());
				snapshots.sort(null);

				StringUtil.copyPartialMatches(args[1], snapshots, completions);
				return completions;
			}
		}

		if (arg.equalsIgnoreCase("create")) {
			if (args.length == 2) {
				StringUtil.copyPartialMatches(args[1], results, completions);
				return completions;
			}

			if (args.length == 3) {
				StringUtil.copyPartialMatches(args[2], Arrays.stream(Direction.values()).map(Direction::getName).collect(Collectors.toList()), completions);
				return completions;
			}

			if (args.length == 5) {
				StringUtil.copyPartialMatches(args[4], Arrays.asList("true", "false"), completions);
				return completions;
			}
		}

		return completions;
	}

	private String formatCommandUsage(String usage) {
		usage = "&3" + usage;

		char[] array = usage.toCharArray();
		StringBuilder buffer = new StringBuilder(usage);

		for (int i = 0; i < array.length; i++) {
			if (array[i] == '[' || array[i] == '<') {
				buffer.insert(i, "&b");
				return Strings.format(buffer.toString());
			}
		}

		return Strings.format(usage);
	}
}