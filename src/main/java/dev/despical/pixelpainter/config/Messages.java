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

package dev.despical.pixelpainter.config;

import dev.despical.pixelpainter.command.ChatLayout;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Loads Adventure MiniMessage templates and renders configured command output.
 * <p>
 * Bundled defaults fill absent keys; configured values must be strings with valid
 * strict MiniMessage syntax. The template map is copied before publication.
 * Templates use {@code {name}} placeholders: ordinary values are inserted as
 * literal text, while explicit Adventure components keep their formatting and
 * interactions. Players receive centered panels for keys that have a
 * {@code -title} companion; the console receives the same text unformatted.
 *
 * @param templates message keys mapped to MiniMessage templates
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public record Messages(Map<String, String> templates) {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[a-z][a-z0-9_-]*}");
    private static final Pattern PLACEHOLDER_NAME = Pattern.compile("[a-z][a-z0-9_-]*");

    /**
     * Copies templates so caller mutations cannot alter the active messages.
     */
    public Messages {
        templates = Map.copyOf(templates);
    }

    /**
     * Merges the configured file with defaults and validates every template.
     *
     * @param file message YAML file
     * @return copied and validated templates
     * @throws Exception if the file, value types or MiniMessage syntax are invalid
     */
    public static Messages load(Path file) throws Exception {
        var defaults = new YamlConfiguration();

        try (var stream = Objects.requireNonNull(Messages.class.getResourceAsStream("/messages.yml"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
        ) {
            defaults.load(reader);
        }

        var yaml = new YamlConfiguration();

        yaml.load(file.toFile());

        Map<String, String> values = new HashMap<>();

        for (String key : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(key)) {
                values.put(key, defaults.getString(key));
            }
        }

        for (String key : yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(key)) {
                continue;
            }

            if (!yaml.isString(key)) {
                throw new IllegalArgumentException("Message must be text: " + key);
            }

            values.put(key, yaml.getString(key));
        }

        var mini = MiniMessage.builder().strict(true).build();

        values.forEach((_, value) -> mini.deserialize(PLACEHOLDER.matcher(value).replaceAll("x")));
        return new Messages(values);
    }

    /**
     * Renders a template with placeholders.
     *
     * @param key message key
     * @param replacements alternating placeholder names and values; a {@link Component} keeps its
     *                     interactions, any other value is inserted as literal text
     * @return rendered Adventure component
     * @throws IllegalArgumentException if the key is unknown or the pairs are incomplete
     */
    public Component get(String key, Object... replacements) {
        String template = templates.get(key);

        if (template == null) {
            throw new IllegalArgumentException("Unknown message key: " + key);
        }

        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Message replacements need name/value pairs");
        }

        var tags = TagResolver.builder();

        for (int i = 0; i < replacements.length; i += 2) {
            String name = replacements[i].toString();

            if (!PLACEHOLDER_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Invalid message placeholder " + name);
            }

            template = template.replace("{" + name + "}", "<" + name + ">");
            tags.resolver(replacements[i + 1] instanceof Component component ? Placeholder.component(name, component)
                : Placeholder.unparsed(name, String.valueOf(replacements[i + 1])));
        }

        return MiniMessage.miniMessage().deserialize(template, tags.build());
    }

    /**
     * Renders a template as plain text.
     *
     * @param key message key
     * @param replacements alternating placeholder names and values
     * @return rendered text without formatting or interactions
     */
    public String text(String key, Object... replacements) {
        return PlainTextComponentSerializer.plainText().serialize(get(key, replacements));
    }

    /**
     * Checks whether a message exists.
     *
     * @param key message key
     * @return whether the template is defined
     */
    public boolean has(String key) {
        return templates.containsKey(key);
    }

    /**
     * Delivers a component to a player, or unformatted non-empty lines to the console.
     *
     * @param sender command audience
     * @param component rendered message
     */
    public void reply(CommandSender sender, Component component) {
        if (sender instanceof Player) {
            sender.sendMessage(component);

            return;
        }

        PlainTextComponentSerializer.plainText().serialize(component).lines().map(String::strip)
            .filter(line -> !line.isEmpty()).forEach(sender::sendMessage);
    }
}
