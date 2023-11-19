package net.pistonmaster.serverwrecker.settings.lib.property;

public record ComboProperty(
        String namespace,
        String name,
        String uiDescription,
        String cliDescription,
        String fullDescription,
        String[] cliNames,
        ComboOption[] options,
        int defaultValue
) implements Property {
    public record ComboOption(
            String id,
            String displayName
    ) {
    }
}
