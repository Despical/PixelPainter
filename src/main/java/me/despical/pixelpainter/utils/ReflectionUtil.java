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

import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class ReflectionUtil {


	@SuppressWarnings("rawtypes")
	public static Object invokeMethod(Object handle, String methodName, Class[] parameterClasses, Object... args) {
		return invokeMethod(handle.getClass(), handle, methodName, parameterClasses, args);
	}

	@SuppressWarnings("rawtypes")
	public static Object invokeMethod(Class<?> clazz, Object handle, String methodName, Class[] parameterClasses, Object... args) {
		Optional<Method> methodOptional = getMethod(clazz, methodName, parameterClasses);

		if (!methodOptional.isPresent()) {
			return null;
		}

		Method method = methodOptional.get();

		try {
			return method.invoke(handle, args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Optional<Method> getMethod(Class<?> clazz, String name, Class<?>... params) {
		try {
			return Optional.of(clazz.getMethod(name, params));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		try {
			return Optional.of(clazz.getDeclaredMethod(name, params));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		return Optional.empty();
	}

	public static boolean isFacing(BlockState bs, BlockFace bf2) {
		try {
			return (((org.bukkit.block.data.Directional) bs.getBlock().getBlockData()).getFacing() == bf2);
		} catch (Error | Exception ignored) {

		}

		return false;
	}

	public static BlockFace getFacing(BlockState bs) {
		try {
			return (((org.bukkit.block.data.Directional) bs.getBlock().getBlockData()).getFacing());
		} catch (Error | Exception ignored) {

		}
		return null;
	}

	public static boolean isDirectional(BlockState bs) {
		try {
			return (bs.getBlock().getBlockData() instanceof org.bukkit.block.data.Directional);
		} catch (Error | Exception ignored) {

		}

		return false;
	}

	public static void setFacing(BlockState bs, BlockFace bf) {
		try {
			org.bukkit.block.data.Directional d = ((org.bukkit.block.data.Directional) bs.getBlockData());
			d.setFacing(bf);
			bs.setBlockData(d);
			bs.update(true, false);
		} catch (Error | Exception ignored) {

		}
	}
}