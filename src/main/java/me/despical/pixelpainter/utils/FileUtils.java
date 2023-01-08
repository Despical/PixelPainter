/*
 * Pixel Painter - Turn your images into blocks.
 * Copyright (C) 2023 Despical
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.pixelpainter.utils;

import me.despical.pixelpainter.Main;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class FileUtils {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	public static File IMAGES;

	public static void downloadImage(URL url, String fileName, Consumer<Integer> percentage) throws MalformedURLException {
		BufferedInputStream in = null;
		FileOutputStream out = null;

		try {
			URLConnection conn = url.openConnection();
			int size = conn.getContentLength();

			in = new BufferedInputStream(url.openStream());
			out = new FileOutputStream(getImageFile(fileName));

			byte[] data = new byte[1024];
			int count, lastPerc = 0;
			double sumCount = 0.0;

			while ((count = in.read(data, 0, 1024)) != -1) {
				out.write(data, 0, count);

				sumCount += count;

				if (size > 0) {
					double perc = sumCount / size * 100.0;

					if (lastPerc != (int) perc) {
						lastPerc = (int) perc;

						percentage.accept((int) perc);
					}
				}
			}

		} catch (IOException exception) {
			exception.printStackTrace();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException exception) {
					exception.printStackTrace();
				}

			if (out != null) {
				try {
					out.close();
				} catch (IOException exception) {
					exception.printStackTrace();
				}
			}
		}
	}

	public static boolean checkIfImageExists(String imageName) {
		return getImageFile(imageName).exists();
	}

	public static File getImageFile(String imageName) {
		return new File(IMAGES + File.separator + imageName);
	}

	public static void initializeFiles() {
		plugin.saveDefaultConfig();

		IMAGES = new File(plugin.getDataFolder(), "images");

		if (IMAGES.exists()) return;

		if (IMAGES.mkdir()) {
			plugin.log("Created images folder, drop your images to this folder.");
		}
	}
}