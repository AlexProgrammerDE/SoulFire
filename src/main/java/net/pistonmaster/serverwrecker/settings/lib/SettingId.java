package net.pistonmaster.serverwrecker.settings.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SettingId {
    /**
     * The id of the setting.
     *
     * @return The id of the setting.
     * This must be unique for each setting.
     */
    String id();
}
