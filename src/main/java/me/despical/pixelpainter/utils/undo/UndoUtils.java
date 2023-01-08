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

import java.util.HashMap;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class UndoUtils {

	static HashMap<String, BlockTypeSnapshot> savedSnapshots = new HashMap<>();

	public static String verifyNewName(String start) {
		String test = start;
		int id = 0;

		while (savedSnapshots.containsKey(test)) {
			test += "(" + ++id + ")";
		}

		return test;
	}

	public static void addNewSnapshot(String name, Location start, Location end) {
		savedSnapshots.put(verifyNewName(name), new BlockTypeSnapshot(start, end));
	}

	public static void undo(String name) {
		BlockTypeSnapshot snapshot = savedSnapshots.get(name);

		if (snapshot != null) snapshot.undo();
	}

	public static void remove(String name) {
		savedSnapshots.remove(name);
	}

	public static Set<String> getSnapshots() {
		return savedSnapshots.keySet();
	}

	public static boolean snapshotExists(String name) {
		for (String snapshotName : savedSnapshots.keySet()) {
			if (snapshotName.equals(name)) return true;
		}

		return false;
	}
}