package me.despical.pixelpainter.commands;

import me.despical.commandframework.Command;
import me.despical.commandframework.CommandArguments;
import me.despical.commons.util.Strings;
import me.despical.pixelpainter.utils.SkinCreator;
import me.despical.pixelpainter.utils.data.AtomicHolder;
import me.despical.pixelpainter.utils.image.AsyncImageLoader;
import me.despical.pixelpainter.utils.Direction;
import me.despical.pixelpainter.utils.FileUtils;
import me.despical.pixelpainter.utils.RGBBlockColor;
import me.despical.pixelpainter.utils.undo.UndoUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static me.despical.commandframework.Command.SenderType.PLAYER;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class MainCommands implements CommandImpl {

	private static final String NAME_URL = "https://api.mojang.com/users/profiles/minecraft/";

	public static String getUUIDFromName(String name) {
		try {
			URL url = new URL(NAME_URL + name);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			List<String> list = new ArrayList<>();

			String input;

			while ((input = br.readLine()) != null) {
				list.add(input);
			}

			br.close();

			for (String s : list) {
				return s.split("\"id\":\"")[1].split("\"")[0];
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Command(
			name = "pp"
	)
	public void ppCommand(CommandArguments arguments) {
		arguments.sendMessage(bold + "Welcome to Pixel Painter plugin!");
		arguments.sendMessage("Use /pp help command to list commands.");
	}

	@Command(
		name = "pp.skin"
	)
	public void skinCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		String uuid = player.getUniqueId().toString();

		try {
			SkinCreator.createStatue(SkinCreator.getSkin(getUUIDFromName(arguments.getArgument(0))), player.getLocation(), Direction.UP_NORTH);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createSkin(Player player, BufferedImage bufferedImage, Direction direction, int height, boolean enableTrans, String name) {
		bufferedImage = RGBBlockColor.resize(bufferedImage, (int) (bufferedImage.getWidth() * (((double) height) * 2 / bufferedImage.getHeight())), height * 2);
		RGBBlockColor.Pixel[][] result = RGBBlockColor.convertTo2DWithoutUsingGetRGB(bufferedImage);

		new AsyncImageLoader(plugin, name, result, player, player.getLocation().clone(), direction, bufferedImage, enableTrans).loadImage(true);
	}

	@Command(
			name = "pp.create",
			permission = "pp.create",
			usage = "/pp create <image file> <direction> <height>",
			desc = "Creates a in-game image from specified image file in the specified direction and height.",
			senderType = PLAYER
	)
	public void createCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		int length = arguments.getArgumentsLength();

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

		try  {
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
			senderType = PLAYER
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
			name = "pp.download",
			permission = "pp.download",
			usage = "/pp download <image name> <image URL>",
			desc = "Download image to load.",
			senderType = PLAYER
	)
	public void downloadImageCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		int length = arguments.getArgumentsLength();

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

		final String link = arguments.getArgument(1);

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
						URL url = new URL((link));

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
			senderType = PLAYER
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
			senderType = PLAYER
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
											  "Existing image files: " + ChatColor.RESET + String.join(", ", files));
	}

	@Command(
			name = "pp.help",
			permission = "pp.help",
			usage = "/pp help",
			desc = "Get list of commands and their descriptions."
	)
	public void helpCommand(CommandArguments arguments) {
		arguments.sendMessage(bold + "*** Pixel Painter Commands ***");
		arguments.sendMessage("");

		List<Command> commands = plugin.getCommandFramework().getCommands();
		commands.removeIf(cmd -> cmd.desc().isEmpty());

		for (Command command : commands) {
			arguments.sendMessage(Strings.format("&7" + command.usage() + " &f&l- &f" + command.desc()));
		}
	}

	@Command(
			name = "pp.specs",
			permission = "pp.specs",
			usage = "/pp specs <image file>",
			desc = "Get image's width and height values.",
			senderType = PLAYER
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

	{
		register(this);

		new TabCompleter(plugin);
	}
}