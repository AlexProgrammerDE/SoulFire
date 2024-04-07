package com.soulfiremc.server.brigadier;

import com.soulfiremc.server.protocol.bot.state.TagsState;
import java.util.function.Predicate;

public interface TagResolvable<T> {
  Predicate<T> resolve(TagsState tagsState);
}
