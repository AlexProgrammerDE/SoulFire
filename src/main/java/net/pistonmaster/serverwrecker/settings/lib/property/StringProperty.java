package net.pistonmaster.serverwrecker.settings.lib.property;

public record StringProperty(
        String namespace,
        String name,
        String uiDescription,
        String cliDescription,
        String fullDescription,
        String[] cliNames,
        String defaultValue
) implements Property {
}
