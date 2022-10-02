/*
 *  Copyright (C) 2017 Zombie_Striker
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU General Public License as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program;
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307 USA
 */

package me.despical.pixelpainter.utils.image;

import me.despical.pixelpainter.Main;
import me.despical.pixelpainter.utils.*;
import me.despical.pixelpainter.utils.RGBBlockColor.Pixel;
import me.despical.pixelpainter.utils.data.AtomicHolder;
import me.despical.pixelpainter.utils.data.DataHolder;
import me.despical.pixelpainter.utils.data.MaterialData;
import me.despical.pixelpainter.utils.undo.UndoUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class AsyncImageLoader extends Image {

	private final Main plugin;
	private final String name;
	private final Pixel[][] result;
	private final BufferedImage bufferedImage;

	public AsyncImageLoader(Main plugin, String name, Pixel[][] result, Player player, Location minCorner, Direction direction, BufferedImage bufferedImage, boolean enableTrans) {
		this.plugin = plugin;
		this.name = name;
		this.result = result;
		this.bufferedImage = bufferedImage;
		this.player = player == null ? "Plugin" : player.getName();
		this.direction = direction;
		this.minCorner = minCorner;
		this.neg = isMinUpNeg(this.direction);
		this.moving = isMovingX(this.direction);
		this.enableTransparent = enableTrans;
	}

	@SuppressWarnings("deprecation")
	public void loadImage(boolean allowUndo) {
		sendMessage("Requested async image loading by " + player);

		final Location start = getBlockAt(0, bufferedImage.getWidth(), bufferedImage.getHeight());
		final Location end = getBlockAt(0, -1, 0).subtract(0, 1, 0);

		if (allowUndo) {
			UndoUtils.addNewSnapshot(name, start, end);
		}

		final int maxHeight = end.getWorld().getMaxHeight();

		new BukkitRunnable() {
			public void run() {
				final HashMap<String, List<DataHolder>> chunksorter = new HashMap<>();

				for (int width = 0; width < (bufferedImage.getWidth()); width += 2) {
					for (int height = (bufferedImage.getHeight() - 1); height >= 0; height -= 2) {
						Location b = getBlockAt(height, width, bufferedImage.getHeight());

						if (b == null || b.getBlockY() > maxHeight) {
							continue;
						}

						Color[] color = new Color[4];

						for (int i = 0; i < 4; i++) {
							int y = (height + 1 < result.length) ? height + (i % 2) : height;
							int x = (width + 1 < result[y].length) ? width + (i % 2) : width;
							color[i] = new Color(result[y][x].r, result[y][x].g, result[y][x].b, result[y][x].a);
						}

						MaterialData materialData = RGBBlockColor.getClosestBlockValue(color, (direction == Direction.FLAT_NORTHEAST || direction == Direction.FLAT_NORTHWEST || direction == Direction.FLAT_SOUTHEAST || direction == Direction.FLAT_SOUTHWEST), enableTransparent, plugin.supportedMaterials);
						String tempKey = (b.getBlockX() / 16) + "," + (b.getBlockZ() / 16);
						List<DataHolder> temp;

						if (chunksorter.containsKey(tempKey)) {
							temp = chunksorter.get(tempKey);

							if (temp == null) temp = new ArrayList<>();

							temp.add(new DataHolder(b, materialData, materialData.hasDirection()));
						} else {
							temp = new ArrayList<>();
							temp.add(new DataHolder(b, materialData));
						}

						chunksorter.put(tempKey, temp);
					}
				}

				int delayLoadingMessage = 0, timesTicked = 0;

				final int maxDelay = 7;
				final AtomicHolder blocksUpdated = new AtomicHolder();

				for (Entry<String, List<DataHolder>> ent : chunksorter.entrySet()) {
					final List<DataHolder> gg = ent.getValue();
					timesTicked++;

					final int tempDel = delayLoadingMessage++;
					delayLoadingMessage %= maxDelay;

					final int currTick = timesTicked;

					new BukkitRunnable() {

						@Override
						public void run() {
							for (final DataHolder dh : gg) {
								final BlockState bs = dh.location.getBlock().getState();
								if (dh.materialData.getMaterial() != Material.AIR) {

									byte rd = dh.materialData.getData();
									BlockFace bf = null;

									if (dh.hasFaces()) {
										bf = getBlockFace(dh, direction);
										rd = getBlockData(dh, direction);
									}

									if (dh.materialData.getMaterial() != bs.getType() || (!Main.isAbove113 && ((int) bs.getRawData()) != ((int) rd))) {
										blocksUpdated.setInteger(blocksUpdated.getInteger() + 1);
										if (Main.isAbove113) {
											bs.getBlock().setType(dh.materialData.getMaterial());
											bs.setType(dh.materialData.getMaterial());

											if (bf != null) {
												ReflectionUtil.setFacing(bs, bf);
											}

										} else {
											bs.setType(dh.materialData.getMaterial());

											if (bs.getRawData() != rd) bs.setRawData(rd);

											bs.update(true, false);
										}

										if (dh.materialData.getMaterial().hasGravity()) {
											Block below = bs.getBlock().getLocation().subtract(0, 1, 0).getBlock();

											if (below.getType() == Material.AIR)
												below.setType(Material.BARRIER); // to prevent blocks falling
										}

										final BlockFace bf2 = bf;

										new BukkitRunnable() {

											@Override
											public void run() {
												boolean tryBoolean = ReflectionUtil.isFacing(bs, bf2);

												if (bs.getBlock().getType() != dh.materialData.getMaterial() || ((bf2 != null) ? (!tryBoolean) : (!Main.isAbove113 && bs.getBlock().getData() != dh.materialData.getData())) || bs.getBlock().getType() == Material.AIR) {
													if (bs.getBlock().getType().name().equals("VOID_AIR")) return;

													BlockFace test = null;

													if (ReflectionUtil.isDirectional(bs.getBlock().getState())) {
														test = ReflectionUtil.getFacing(bs.getBlock().getState());
													}

													sendMessage(
																"Incorrect value: " + dh.materialData.getMaterial().name() + ":"
																+ ((bf2 != null) ? bf2.name() : dh.materialData.getData())
																+ " is " + bs.getBlock().getType() + ":"
																+ (test != null ? test.name() : bs.getBlock().getData())
																+ " at " + bs.getBlock().getLocation().getBlockX() + ","
																+ bs.getBlock().getLocation().getBlockY() + ","
																+ bs.getBlock().getLocation().getBlockZ());
												}

												cancel();
											}
										}.runTaskLater(plugin, 20);
									}
								}
							}

							if (tempDel == 0)
								sendMessage(" Loading: "
											+ ((int) (((double) currTick) / chunksorter.size() * 100)) + "%");

							cancel();
						}
					}.runTaskLater(plugin, 3L * timesTicked);
				}

				new BukkitRunnable() {

					@Override
					public void run() {
						sendMessage("Done! Updated " + blocksUpdated.getInteger() + " blocks!");

						cancel();
					}
				}.runTaskLater(plugin, 3L * timesTicked);

				cancel();
			}
		}.runTaskAsynchronously(plugin);
	}

	public static BlockFace getBlockFace(DataHolder dataHolder, Direction direction) {
		Material material = dataHolder.materialData.getMaterial();

		try {
			if (material == Material.FURNACE || material.name().equals("BURNING_FURNACE")) {
				if (direction == Direction.UP_EAST)
					return BlockFace.SOUTH;
				if (direction == Direction.UP_SOUTH)
					return BlockFace.WEST;
				if (direction == Direction.UP_WEST)
					return BlockFace.NORTH;
				if (direction == Direction.UP_NORTH)
					return BlockFace.EAST;
			}

			if (material == Material.DISPENSER) {
				if (direction == Direction.UP_EAST)
					return BlockFace.SOUTH;
				if (direction == Direction.UP_SOUTH)
					return BlockFace.WEST;
				if (direction == Direction.UP_WEST)
					return BlockFace.NORTH;
				if (direction == Direction.UP_NORTH)
					return BlockFace.EAST;
			}

			if (material == Material.OBSERVER) {
				if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.FRONT) {
					if (Main.isAbove113) {
						if (direction == Direction.UP_EAST)
							return BlockFace.SOUTH;
						if (direction == Direction.UP_SOUTH)
							return BlockFace.WEST;
						if (direction == Direction.UP_WEST)
							return BlockFace.NORTH;
						if (direction == Direction.UP_NORTH)
							return BlockFace.EAST;
					}

					if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.BACK) {
						if (direction == Direction.UP_EAST)
							return BlockFace.NORTH;
						if (direction == Direction.UP_SOUTH)
							return BlockFace.EAST;
						if (direction == Direction.UP_WEST)
							return BlockFace.SOUTH;
						if (direction == Direction.UP_NORTH)
							return BlockFace.WEST;
					}

					if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.SIDE) {
						if (direction == Direction.UP_EAST)
							return BlockFace.EAST;
						if (direction == Direction.UP_SOUTH)
							return BlockFace.SOUTH;
						if (direction == Direction.UP_WEST)
							return BlockFace.WEST;
						if (direction == Direction.UP_NORTH)
							return BlockFace.NORTH;
					}
				}
			}
		} catch (Error | Exception ignored) {
		}

		return null;
	}

	public static byte getBlockData(DataHolder dataHolder, Direction direction) {
		Material material = dataHolder.materialData.getMaterial();
		String name = material.name();

		try {
			if (name.endsWith("_DOOR") && !name.contains("TRAP")) {
				if (direction == Direction.UP_NORTH)
					return ((byte) 2);
				if (direction == Direction.UP_EAST)
					return ((byte) 3);
				if (direction == Direction.UP_SOUTH)
					return ((byte) 0);
				if (direction == Direction.UP_WEST)
					return ((byte) 1);
			}

			if (material == Material.FURNACE || name.equals("BURNING_FURNACE")) {
				if (direction == Direction.UP_NORTH)
					return ((byte) 5);
				if (direction == Direction.UP_EAST)
					return ((byte) 3);
				if (direction == Direction.UP_SOUTH)
					return ((byte) 4);
				if (direction == Direction.UP_WEST)
					return ((byte) 2);
			}

			if (material == Material.DISPENSER) {
				if (direction == Direction.UP_NORTH)
					return ((byte) 5);
				if (direction == Direction.UP_EAST)
					return ((byte) 3);
				if (direction == Direction.UP_SOUTH)
					return ((byte) 4);
				if (direction == Direction.UP_WEST)
					return ((byte) 2);

			}
			if (name.equals("PISTON_BASE") || name.equals("PISTON_STICKY_BASE")) {
				if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.TOP) {
					if (direction == Direction.UP_NORTH)
						return ((byte) 5);
					if (direction == Direction.UP_EAST)
						return ((byte) 3);
					if (direction == Direction.UP_SOUTH)
						return ((byte) 4);
					if (direction == Direction.UP_WEST)
						return ((byte) 2);
				}

				if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.BACK) {
					if (direction == Direction.UP_NORTH)
						return ((byte) 4);
					if (direction == Direction.UP_EAST)
						return ((byte) 2);
					if (direction == Direction.UP_SOUTH)
						return ((byte) 5);
					if (direction == Direction.UP_WEST)
						return ((byte) 3);
				}
			}

			if (material == Material.PUMPKIN || material == Material.JACK_O_LANTERN) {
				if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.FRONT) {
					if (direction == Direction.UP_NORTH)
						return ((byte) 3);
					if (direction == Direction.UP_EAST)
						return ((byte) 0);
					if (direction == Direction.UP_SOUTH)
						return ((byte) 1);
					if (direction == Direction.UP_WEST)
						return ((byte) 2);
				}

				if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.BACK) {
					if (direction == Direction.UP_NORTH)
						return ((byte) 0);
					if (direction == Direction.UP_EAST)
						return ((byte) 3);
					if (direction == Direction.UP_SOUTH)
						return ((byte) 2);
					if (direction == Direction.UP_WEST)
						return ((byte) 1);
				}

			}

			if (material == Material.OBSERVER) {
				if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.FRONT) {
					if (direction == Direction.UP_NORTH)
						return ((byte) 4);
					if (direction == Direction.UP_EAST)
						return ((byte) 2);
					if (direction == Direction.UP_SOUTH)
						return ((byte) 5);
					if (direction == Direction.UP_WEST)
						return ((byte) 3);

				}

				if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.BACK) {
					if (direction == Direction.UP_NORTH)
						return ((byte) 5);
					if (direction == Direction.UP_EAST)
						return ((byte) 3);
					if (direction == Direction.UP_SOUTH)
						return ((byte) 4);
					if (direction == Direction.UP_WEST)
						return ((byte) 2);
				}

				if (dataHolder.materialData.getDirection() == ImageRelativeBlockDirection.SIDE) {
					if (direction == Direction.UP_NORTH)
						return ((byte) 3);
					if (direction == Direction.UP_EAST)
						return ((byte) 5);
					if (direction == Direction.UP_SOUTH)
						return ((byte) 2);
					if (direction == Direction.UP_WEST)
						return ((byte) 4);
				}
			}
		} catch (Error | Exception ignored) {}

		return dataHolder.materialData.getData();
	}

	private void sendMessage(String message) {
		minCorner.getWorld().getPlayers().forEach(player -> player.sendMessage(message));
	}
}