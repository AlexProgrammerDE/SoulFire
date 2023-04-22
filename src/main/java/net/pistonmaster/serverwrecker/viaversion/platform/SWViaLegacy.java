package net.pistonmaster.serverwrecker.viaversion.platform;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.viaversion.JLoggerToLogback;
import net.raphimc.vialegacy.platform.ViaLegacyPlatform;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SWViaLegacy implements ViaLegacyPlatform {
    private final JLoggerToLogback logger = new JLoggerToLogback(LoggerFactory.getLogger("ViaLegacy"));
    private final Path dataFolder;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public File getDataFolder() {
        return dataFolder.toFile();
    }

    public void init() {
        init(getDataFolder());
    }
}
