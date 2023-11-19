package net.pistonmaster.serverwrecker.settings.lib.property;

public record IntProperty(String namespace,
                          String name,
                          String uiDescription,
                          String cliDescription,
                          String fullDescription,
                          String[] cliNames,
                          int defaultValue
) implements Property {
}
