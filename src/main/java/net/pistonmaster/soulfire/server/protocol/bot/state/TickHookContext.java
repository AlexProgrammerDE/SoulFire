package net.pistonmaster.soulfire.server.protocol.bot.state;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.Getter;

@Getter
public class TickHookContext {
    public static final ThreadLocal<TickHookContext> INSTANCE = ThreadLocal.withInitial(TickHookContext::new);

    private final Multimap<HookType, Runnable> hooks = MultimapBuilder.enumKeys(HookType.class)
            .arrayListValues()
            .build();

    public void addHook(HookType type, Runnable hook) {
        hooks.put(type, hook);
    }

    public void callHooks(HookType type) {
        hooks.get(type).forEach(Runnable::run);
    }

    public void clear() {
        hooks.clear();
    }

    public enum HookType {
        PRE_TICK,
        PRE_ENTITY_TICK,
        POST_ENTITY_TICK,
        POST_TICK
    }
}
