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

package me.despical.pixelpainter.utils.undo;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class BlockTypeSnapshot {

	public Map<Location, BlockState> originalBlockStates;

	public BlockTypeSnapshot(Location start, Location stop) {
		this.originalBlockStates = new HashMap<>();

		final World world = start.getWorld();

		for (int x = Math.min(start.getBlockX(), stop.getBlockX()); x < Math.max(start.getBlockX(), stop.getBlockX()) + 1; x++) {
			for (int y = Math.min(start.getBlockY(), stop.getBlockY()); y < Math.min(256, Math.max(start.getBlockY(), stop.getBlockY())) + 1; y++) {
				for (int z = Math.min(start.getBlockZ(), stop.getBlockZ()); z < Math.max(start.getBlockZ(), stop.getBlockZ()) + 1; z++) {
					Block block = world.getBlockAt(x, y, z);

					originalBlockStates.put(block.getLocation(), block.getState());
				}
			}
		}
	}

	public void undo() {
		final Map<Location, BlockState> newOnes = new HashMap<>();

		for (Map.Entry<Location, BlockState> e : originalBlockStates.entrySet()) {
			if (!e.getKey().getBlock().getState().equals(e.getValue())) {
				newOnes.put(e.getKey(), e.getKey().getBlock().getState());

				BlockState state = e.getValue();
				state.update(true,false);
			}
		}

		this.originalBlockStates = newOnes;
	}
}