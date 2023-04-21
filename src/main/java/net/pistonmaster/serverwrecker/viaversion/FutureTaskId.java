package net.pistonmaster.serverwrecker.viaversion;

import com.viaversion.viaversion.api.platform.PlatformTask;

import java.util.concurrent.Future;

public record FutureTaskId(Future<?> object) implements PlatformTask<Future<?>> {
    @Override
    public Future<?> getObject() {
        return object;
    }

    @Override
    public void cancel() {
        object.cancel(false);
    }
}
