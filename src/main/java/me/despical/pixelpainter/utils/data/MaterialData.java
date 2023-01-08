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

package me.despical.pixelpainter.utils.data;

import me.despical.pixelpainter.utils.RGBBlockColor;
import me.despical.pixelpainter.utils.image.ImageRelativeBlockDirection;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class MaterialData implements ConfigurationSerializable, Comparable<MaterialData> {

	private Material material;
	private final byte data;

	private ImageRelativeBlockDirection direction = null;

	public MaterialData(Material m, byte data, ImageRelativeBlockDirection direction) {
		ConfigurationSerialization.registerClass(MaterialData.class);
		this.setMaterial(m);
		this.data = data;
		this.direction = direction;

	}

	public MaterialData(Material m, byte data) {
		this (m, data, null);
	}

	public MaterialData(Material m) {
		this (m, ((byte) 0), null);
	}

	public MaterialData(Map<String, Object> data) {
		this.setMaterial(Material.valueOf((String) data.get("m")));
		this.data = Byte.parseByte((String) data.get("data"));
	}

	public static MaterialData getMatDataByTypes(Material mat, byte data) {
		return getMatDataByTypes(mat, data, null);
	}

	public static MaterialData getMatDataByTypes(Material mat, byte data, ImageRelativeBlockDirection direction) {
		for (MaterialData key : RGBBlockColor.materialValue.keySet())
			if (key.getData() == data && key.getMaterial() == mat && ((direction == null && !key.hasDirection()) || direction == key.getDirection())) return key;

		return null;
	}

	public boolean hasDirection() {
		return direction != null;
	}

	public ImageRelativeBlockDirection getDirection() {
		return direction;
	}

	public BlockFace getBlockFace() {
		return direction.convertToBlockFace();
	}

	public byte getData() {
		return data;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material m) {
		this.material = m;
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("m", this.getMaterial().toString());
		data.put("data", this.data + "");
		return data;
	}

	@Override
	public int compareTo(MaterialData o) {
		if(this.material ==o.material){
			String dir1 = this.direction == null ? "" : this.direction.name();
			String dir2 = o.direction ==  null ? "" : o.direction.name();
			return dir1.compareTo(dir2);
		}

		return this.material.name().compareTo(o.material.name());
	}
}