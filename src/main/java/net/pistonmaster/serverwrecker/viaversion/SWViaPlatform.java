package net.pistonmaster.serverwrecker.viaversion;

import com.viaversion.viaversion.ViaAPIBase;
import com.viaversion.viaversion.api.ViaAPI;
import com.viaversion.viaversion.api.command.ViaCommandSender;
import com.viaversion.viaversion.api.configuration.ConfigurationProvider;
import com.viaversion.viaversion.api.configuration.ViaVersionConfig;
import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.platform.ViaPlatform;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.viaversion.viaversion.libs.fastutil.ints.IntLinkedOpenHashSet;
import com.viaversion.viaversion.libs.fastutil.ints.IntSortedSet;
import com.viaversion.viaversion.libs.gson.JsonObject;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.SWConstants;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SWViaPlatform implements ViaPlatform<UUID> {
    private final Path dataFolder;
    private ViaVersionConfig config;
    private final ViaAPI<UUID> api = new ViaAPIBase<>() {
    };
    @Getter
    private final ViaInjector injector = new ViaInjector() {
        @Override
        public void inject() {
        }

        @Override
        public void uninject() {
        }

        @Override
        public IntSortedSet getServerProtocolVersions() {
            // On client-side we can connect to any server version
            IntSortedSet versions = new IntLinkedOpenHashSet();
            versions.add(ProtocolVersion.v1_8.getOriginalVersion());
            versions.add(SWConstants.getVersionsSorted()
                    .stream()
                    .mapToInt(ProtocolVersion::getOriginalVersion)
                    .max().getAsInt());
            return versions;
        }

        @Override
        public int getServerProtocolVersion() {
            return getServerProtocolVersions().firstInt();
        }

        @Override
        public String getEncoderName() {
            return getDecoderName();
        }

        @Override
        public String getDecoderName() {
            return "via-codec";
        }

        @Override
        public JsonObject getDump() {
            return new JsonObject();
        }
    };
    private final EventLoop eventLoop = new DefaultEventLoop();
    private final ExecutorService asyncService = Executors.newFixedThreadPool(4);

    public void init() {
        config = new AbstractViaConfig(dataFolder.resolve("config.yml").toFile()) {
            {
                reloadConfig();
            }

            // Based on Sponge ViaVersion
            private static final List<String> UNSUPPORTED = Arrays.asList("anti-xray-patch", "bungee-ping-interval",
                    "bungee-ping-save", "bungee-servers", "quick-move-action-fix", "nms-player-ticking",
                    "velocity-ping-interval", "velocity-ping-save", "velocity-servers",
                    "blockconnection-method", "change-1_9-hitbox", "change-1_14-hitbox");

            @Override
            protected void handleConfig(Map<String, Object> config) {
            }

            @Override
            public List<String> getUnsupportedOptions() {
                return UNSUPPORTED;
            }
        };
    }

    protected FutureTaskId runEventLoop(Runnable runnable) {
        return new FutureTaskId(eventLoop.submit(runnable).addListener(errorLogger()));
    }

    @Override
    public Logger getLogger() {
        return Logger.getLogger("ViaVersion");
    }

    @Override
    public String getPlatformName() {
        return "ServerWrecker";
    }

    @Override
    public String getPlatformVersion() {
        return "1.0.0"; // TODO
    }

    @Override
    public boolean isProxy() {
        return true;
    }

    @Override
    public String getPluginVersion() {
        return "1.0.0"; // TODO
    }

    @Override
    public FutureTaskId runAsync(Runnable runnable) {
        return new FutureTaskId(CompletableFuture.runAsync(runnable, asyncService)
                .exceptionally(throwable -> {
                    if (!(throwable instanceof CancellationException)) {
                        throwable.printStackTrace();
                    }
                    return null;
                }));
    }

    @Override
    public FutureTaskId runRepeatingAsync(Runnable runnable, long ticks) {
        return new FutureTaskId(eventLoop
                .scheduleAtFixedRate(() -> runAsync(runnable), 0, ticks * 50, TimeUnit.MILLISECONDS)
                .addListener(errorLogger())
        );
    }

    @Override
    public FutureTaskId runSync(Runnable runnable) {
        return runEventLoop(runnable);
    }

    @Override
    public FutureTaskId runSync(Runnable runnable, long ticks) {
        // ViaVersion seems to not need to run delayed tasks on main thread
        return new FutureTaskId(eventLoop
                .schedule(() -> runSync(runnable), ticks * 50, TimeUnit.MILLISECONDS)
                .addListener(errorLogger())
        );
    }

    @Override
    public FutureTaskId runRepeatingSync(Runnable runnable, long ticks) {
        // ViaVersion seems to not need to run repeating tasks on main thread
        return new FutureTaskId(eventLoop
                .scheduleAtFixedRate(() -> runSync(runnable), 0, ticks * 50, TimeUnit.MILLISECONDS)
                .addListener(errorLogger())
        );
    }

    @Override
    public ViaCommandSender[] getOnlinePlayers() {
        return new ViaCommandSender[0];
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
    }

    @Override
    public boolean kickPlayer(UUID uuid, String message) {
        return false;
    }

    @Override
    public boolean isPluginEnabled() {
        return false;
    }

    @Override
    public ViaAPI<UUID> getApi() {
        return api;
    }

    @Override
    public ViaVersionConfig getConf() {
        return config;
    }

    @Override
    public ConfigurationProvider getConfigurationProvider() {
        return null;
    }

    @Override
    public File getDataFolder() {
        return dataFolder.toFile();
    }

    @Override
    public void onReload() {
    }

    @Override
    public JsonObject getDump() {
        return injector.getDump();
    }

    @Override
    public boolean isOldClientsAllowed() {
        return true;
    }

    @Override
    public boolean hasPlugin(String name) {
        return false;
    }

    protected <T extends Future<?>> GenericFutureListener<T> errorLogger() {
        return future -> {
            if (!future.isCancelled() && future.cause() != null) {
                future.cause().printStackTrace();
            }
        };
    }
}
