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

package dev.despical.pixelpainter;

import dev.despical.pixelpainter.config.Messages;
import dev.despical.pixelpainter.config.Settings;
import dev.despical.pixelpainter.core.Bounds;
import dev.despical.pixelpainter.core.Position;
import dev.despical.pixelpainter.image.ImageStore;
import dev.despical.pixelpainter.storage.PaintRecord;
import dev.despical.pixelpainter.storage.UndoStore;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies source boundaries, journal restart recovery and configuration/message validation.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
class StorageAndConfigurationTest {

    @TempDir
    Path temporary;

    private Settings settings() throws Exception {
        Path config = temporary.resolve("config.yml");

        if (!Files.exists(config)) {
            Files.writeString(config, "{}");
        }

        return Settings.load(config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secret", "..\\secret", "C:\\secret", "/tmp/test", ".hidden", "a.gif", "a.png.exe", "a/b", "<red>"})
    void rejectsUnsafeImageNames(String name) {
        assertThrows(IllegalArgumentException.class, () -> ImageStore.validName(name));
    }

    @Test
    void imageResolutionRejectsAmbiguousNamesAndCorruptImages() throws Exception {
        Path folder = temporary.resolve("images");
        var store = new ImageStore(folder);
        var image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);

        ImageIO.write(image, "png", folder.resolve("art.png").toFile());
        assertEquals(3, store.read("ART", settings()).getHeight());
        ImageIO.write(image, "jpeg", folder.resolve("art.jpg").toFile());
        assertThrows(IllegalArgumentException.class, () -> store.resolve("art"));
        Files.writeString(folder.resolve("broken.png"), "not an image");
        assertThrows(IllegalArgumentException.class, () -> store.read("broken.png", settings()));
        store.delete("art.jpg");
        assertTrue(Files.exists(folder.resolve("art.png")));
    }
}
