package net.pistonmaster.serverwrecker.viaversion.platform;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.viaversion.JLoggerToLogback;
import net.raphimc.viaaprilfools.platform.ViaAprilFoolsPlatform;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SWViaAprilFools implements ViaAprilFoolsPlatform {
    private final JLoggerToLogback logger = new JLoggerToLogback(LoggerFactory.getLogger("ViaAprilFools"));
    private final Path dataFolder;

    @Override
    public Logger getLogger() {
        return logger;
    }

    public void init() {
        init(getDataFolder());
    }

    @Override
    public File getDataFolder() {
        return dataFolder.toFile();
    }
}
