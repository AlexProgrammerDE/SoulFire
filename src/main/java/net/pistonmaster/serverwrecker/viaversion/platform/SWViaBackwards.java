package net.pistonmaster.serverwrecker.viaversion.platform;

import com.viaversion.viabackwards.api.ViaBackwardsPlatform;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.viaversion.JLoggerToLogback;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SWViaBackwards implements ViaBackwardsPlatform {
    private final JLoggerToLogback logger = new JLoggerToLogback(LoggerFactory.getLogger("ViaBackwards"));
    private final Path dataFolder;

    @Override
    public Logger getLogger() {
        return logger;
    }

    public void init() {
        init(getDataFolder());
    }

    @Override
    public void disable() {
    }

    @Override
    public File getDataFolder() {
        return dataFolder.toFile();
    }
}
