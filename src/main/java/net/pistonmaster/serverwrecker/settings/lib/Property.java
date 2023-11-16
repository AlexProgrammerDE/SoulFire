package net.pistonmaster.serverwrecker.settings.lib;

public interface Property {
    static Property of(String name, String uiDescription, String cliDescription, String fullDescription, String[] cliNames, boolean defaultValue) {
        return new BooleanProperty(name, uiDescription, cliDescription, fullDescription, cliNames, defaultValue);
    }

    static Property of(String name, String uiDescription, String cliDescription, String fullDescription, String[] cliNames, int defaultValue) {
        return new IntegerProperty(name, uiDescription, cliDescription, fullDescription, cliNames, defaultValue);
    }

    static Property of(String name, String uiDescription, String cliDescription, String fullDescription, String[] cliNames, String defaultValue) {
        return new StringProperty(name, uiDescription, cliDescription, fullDescription, cliNames, defaultValue);
    }

    static Property of(String name, String uiDescription, String cliDescription, String fullDescription, String[] cliNames, ComboOption[] values, int defaultValue) {
        return new ComboProperty(name, uiDescription, cliDescription, fullDescription, cliNames, values, defaultValue);
    }

    record BooleanProperty(
            String name,
            String uiDescription,
            String cliDescription,
            String fullDescription,
            String[] cliNames,
            boolean defaultValue
    ) implements Property {
    }

    record IntegerProperty(
            String name,
            String uiDescription,
            String cliDescription,
            String fullDescription,
            String[] cliNames,
            int defaultValue
    ) implements Property {
    }

    record StringProperty(
            String name,
            String uiDescription,
            String cliDescription,
            String fullDescription,
            String[] cliNames,
            String defaultValue
    ) implements Property {
    }

    record ComboProperty(
            String name,
            String uiDescription,
            String cliDescription,
            String fullDescription,
            String[] cliNames,
            ComboOption[] options,
            int defaultValue
    ) implements Property {
    }

    record ComboOption(
            String id,
            String displayName
    ) {
    }
}
