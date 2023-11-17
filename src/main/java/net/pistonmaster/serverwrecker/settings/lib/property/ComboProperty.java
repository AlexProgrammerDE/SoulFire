package net.pistonmaster.serverwrecker.settings.lib.property;

public record ComboProperty(
        String settingsId,
        String name,
        String uiDescription,
        String cliDescription,
        String fullDescription,
        String[] cliNames,
        ComboOption[] options,
        int defaultValue
) {
    public record ComboOption(
            String id,
            String displayName
    ) {
    }
}
