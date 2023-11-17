package net.pistonmaster.serverwrecker.settings.lib.property;

public record StringProperty(
        String settingsId,
        String name,
        String uiDescription,
        String cliDescription,
        String fullDescription,
        String[] cliNames,
        String defaultValue
) {
}
