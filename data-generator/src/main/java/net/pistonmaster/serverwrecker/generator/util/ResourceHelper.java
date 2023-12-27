package net.pistonmaster.serverwrecker.generator.util;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ResourceHelper {
    public static String getResource(String path) {
        try {
            var inputStream = Objects.requireNonNull(ResourceHelper.class.getResourceAsStream(path));
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate language file", e);
        }
    }
}
