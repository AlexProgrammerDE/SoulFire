package net.pistonmaster.serverwrecker.settings.lib.property;

public record BooleanProperty(String settingsId,
                              String name,
                              String uiDescription,
                              String cliDescription,
                              String fullDescription,
                              String[] cliNames,
                              boolean defaultValue
) {
}
