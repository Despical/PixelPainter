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

package dev.despical.pixelpainter.image;

import dev.despical.pixelpainter.core.PaintError;
import dev.despical.pixelpainter.core.PaintException;
import dev.despical.pixelpainter.config.Settings;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Owns local PNG, JPEG and WebP files and bounded downloads from trusted HTTPS hosts.
 * <p>
 * Names use a flat namespace. Resolution excludes symbolic links and requires an
 * explicit extension when a base name is ambiguous. Decode checks byte size,
 * format and source dimensions before reading pixels.
 * <p>
 * Downloads validate redirect destinations, reject local addresses, enforce byte
 * and read-time limits, and decode a temporary file before promoting it. Existing
 * files are never replaced. These blocking operations belong on image workers
 * and do not access Bukkit worlds. Trust the configured host owners.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class ImageStore {

    private final Path folder;

    /**
     * Creates the source directory and resolves its real filesystem location.
     *
     * @param folder source-image directory
     * @throws IOException if the directory cannot be created or resolved
     */
    public ImageStore(Path folder) throws IOException {
        Files.createDirectories(folder);
        this.folder = folder.toRealPath();
    }

    /**
     * Validates a flat source name with an optional PNG, JPEG or WebP extension.
     *
     * @param input letters, digits, underscores or hyphens with an optional extension
     * @return unchanged input
     * @throws IllegalArgumentException if the name violates the filename policy
     */
    public static String validName(String input) {
        if (!input.matches("[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}(?i:\\.(png|jpg|jpeg|webp))?")) {
            throw new PaintException(PaintError.FILENAME);
        }

        return input;
    }

    /**
     * Lists valid, regular source files without following symbolic links.
     *
     * @return source names sorted without regard to case
     * @throws IOException if directory scanning fails
     */
    public List<String> list() throws IOException {
        try (var paths = Files.list(folder)
        ) {
            return paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                .map(p -> p.getFileName().toString())
                .filter(ImageStore::isSourceName)
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
    }

    private static boolean isSourceName(String name) {
        if (!name.matches("(?i).+\\.(png|jpg|jpeg|webp)")) {
            return false;
        }

        try {
            validName(name);

            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Resolves a case-insensitive name or an unambiguous extensionless base name.
     *
     * @param name validated source name
     * @return path of the existing source image
     * @throws IOException if scanning fails
     * @throws IllegalArgumentException if the name is invalid, missing or ambiguous
     */
    public Path resolve(String name) throws IOException {
        validName(name);
        List<String> matches = list().stream().filter(n -> n.equalsIgnoreCase(name)
            || (!name.contains(".") && n.substring(0, n.lastIndexOf('.')).equalsIgnoreCase(name))).toList();

        if (matches.isEmpty()) {
            throw new PaintException(PaintError.MISSING_IMAGE);
        }

        if (matches.size() > 1) {
            throw new PaintException(PaintError.AMBIGUOUS_IMAGE);
        }

        return folder.resolve(matches.getFirst());
    }

    /**
     * Resolves and decodes a source image using captured limits.
     *
     * @param name source name
     * @param settings request settings
     * @return decoded image
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if name, format or size validation fails
     */
    public BufferedImage read(String name, Settings settings) throws IOException {
        return decode(resolve(name), settings);
    }

    /**
     * Validates byte size, format and source dimensions before reading pixels.
     *
     * @param file existing image file
     * @param settings source limits
     * @return decoded first image
     * @throws IOException if metadata or content cannot be read
     * @throws IllegalArgumentException if format, bytes or dimensions are unsupported
     */
    public static BufferedImage decode(Path file, Settings settings) throws IOException {
        if (Files.size(file) > settings.maxFileBytes()) {
            throw new PaintException(PaintError.FILE_SIZE);
        }

        try (var stream = ImageIO.createImageInputStream(file.toFile())
        ) {
            var readers = ImageIO.getImageReaders(stream);

            if (!readers.hasNext()) {
                throw new PaintException(PaintError.IMAGE_FORMAT);
            }

            ImageReader reader = readers.next();

            try {
                String format = reader.getFormatName();

                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")
                    && !format.equalsIgnoreCase("webp")) {
                    throw new PaintException(PaintError.IMAGE_FORMAT);
                }

                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);

                if (width <= 0 || height <= 0 || (long) width * height > settings.maxSourcePixels()) {
                    throw new PaintException(PaintError.SOURCE_SIZE);
                }

                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Deletes a resolved source without changing paintings or undo journals.
     *
     * @param name source name
     * @throws IOException if deletion or resolution fails
     * @throws IllegalArgumentException if the name is invalid, missing or ambiguous
     */
    public void delete(String name) throws IOException {
        Files.delete(resolve(name));
    }

    /**
     * Downloads and validates an image before moving it into the source directory.
     *
     * @param name destination name; an absent extension becomes .png
     * @param link trusted HTTPS source URL
     * @param settings captured download hosts and limits
     * @return saved destination filename
     * @throws Exception if network access, validation, decoding or promotion fails
     */
    public String download(String name, String link, Settings settings) throws Exception {
        validName(name);

        if (!name.contains(".")) {
            name += ".png";
        }

        Path target = folder.resolve(name);

        if (list().stream().anyMatch(n -> n.equalsIgnoreCase(target.getFileName().toString()))) {
            throw new PaintException(PaintError.IMAGE_EXISTS);
        }

        Path temporary = Files.createTempFile(folder, ".download-", ".tmp");

        try {
            URI uri = URI.create(link);
            boolean received = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);

            for (int redirects = 0; redirects <= 4; redirects++) {
                validateRemote(uri, settings);
                var connection = (HttpURLConnection) uri.toURL().openConnection();

                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "PixelPainter/2.0");

                try {
                    int status = connection.getResponseCode();

                    if (status >= 300 && status <= 399) {
                        String location = connection.getHeaderField("Location");

                        if (location == null) {
                            throw new PaintException(PaintError.DOWNLOAD_FAILED);
                        }

                        uri = uri.resolve(location);
                        continue;
                    }

                    if (status != 200 || connection.getContentLengthLong() > settings.maxFileBytes()) {
                        throw new PaintException(PaintError.DOWNLOAD_FAILED);
                    }

                    try (var input = connection.getInputStream(); var output = Files.newOutputStream(temporary)
                    ) {
                        byte[] buffer = new byte[8192];
                        int count, total = 0;

                        while ((count = input.read(buffer)) != -1) {
                            total += count;

                            if (total > settings.maxFileBytes() || System.nanoTime() > deadline
                                || Thread.currentThread().isInterrupted()) {
                                throw new PaintException(PaintError.DOWNLOAD_FAILED);
                            }

                            output.write(buffer, 0, count);
                        }
                    }

                    received = true;
                    break;
                } finally {
                    connection.disconnect();
                }
            }

            if (!received) {
                throw new PaintException(PaintError.DOWNLOAD_FAILED);
            }

            decode(temporary, settings);
            Files.move(temporary, target); // Never replace an existing image.
            return name;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void validateRemote(URI uri, Settings settings) throws Exception {
        String host = uri.getHost();

        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null
            || (uri.getPort() != -1 && uri.getPort() != 443)
            || !settings.downloadHosts().contains(host.toLowerCase(Locale.ROOT))) {
            throw new PaintException(PaintError.DOWNLOAD_HOST);
        }

        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()
                || (address.getAddress().length == 16 && (address.getAddress()[0] & 0xfe) == 0xfc)) {
                throw new PaintException(PaintError.DOWNLOAD_HOST);
            }
        }
    }
}
