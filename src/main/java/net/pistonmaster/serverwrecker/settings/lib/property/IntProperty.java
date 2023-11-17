package net.pistonmaster.serverwrecker.settings.lib.property;

public record IntProperty(String settingsId,
                          String name,
                          String uiDescription,
                          String cliDescription,
                          String fullDescription,
                          String[] cliNames,
                          int defaultValue
) {
}
