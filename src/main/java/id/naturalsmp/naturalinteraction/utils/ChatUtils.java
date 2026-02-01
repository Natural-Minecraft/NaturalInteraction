package id.naturalsmp.naturalinteraction.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null || message.isEmpty())
            return "";

        String result = message;

        if (result.contains("<")) {
            try {
                result = SECTION_SERIALIZER.serialize(MINI_MESSAGE.deserialize(result));
            } catch (Exception ignored) {
            }
        }

        Matcher matcher = HEX_PATTERN.matcher(result);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            try {
                matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString());
            } catch (Exception e) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()));
            }
        }
        result = matcher.appendTail(buffer).toString();

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    public static Component toComponent(String message) {
        if (message == null || message.isEmpty())
            return Component.empty();
        return MINI_MESSAGE.deserialize(message);
    }

    public static String serialize(Component component) {
        if (component == null)
            return "";
        return SECTION_SERIALIZER.serialize(component);
    }
}