package net.pistonmaster.serverwrecker.viaversion.platform;

import de.gerrygames.viarewind.api.ViaRewindConfigImpl;
import de.gerrygames.viarewind.api.ViaRewindPlatform;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.viaversion.JLoggerToLogback;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SWViaRewind implements ViaRewindPlatform {
    private final JLoggerToLogback logger = new JLoggerToLogback(LoggerFactory.getLogger("ViaRewind"));
    private final Path dataFolder;

    @Override
    public Logger getLogger() {
        return logger;
    }

    public void init() {
        init(new ViaRewindConfigImpl(dataFolder.resolve("config.yml").toFile()));
    }
}
