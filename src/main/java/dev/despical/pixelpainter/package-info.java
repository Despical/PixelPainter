/*
 * PixelPainter - Turn images into Minecraft block art.
 * Copyright (C) 2022-2026  Berke Akçen
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

/**
 * Plugin lifecycle and the service boundary for PixelPainter.
 * <p>
 * The PixelPainter entry point creates configuration and execution services before
 * registering commands. ConfigurationManager owns resource validation, TaskExecutor
 * owns bounded worker lifetimes, and PaintService owns world jobs and recovery.
 * Startup failures disable the plugin; invalid reloads retain the active resources.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
package dev.despical.pixelpainter;
