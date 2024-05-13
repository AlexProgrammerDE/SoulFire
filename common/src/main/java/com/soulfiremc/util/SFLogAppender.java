/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Getter
public class SFLogAppender extends AbstractAppender {
  public static final SFLogAppender INSTANCE = new SFLogAppender();

  private static final AbstractStringLayout.Serializer formatter =
    new PatternLayout.SerializerBuilder()
      .setAlwaysWriteExceptions(true)
      .setDisableAnsi(false)
      .setNoConsoleNoAnsi(false)
      .setDefaultPattern(
        "%highlight{[%d{HH:mm:ss} %level] [%logger{1.*}]: %minecraftFormatting{%msg}%xEx}{FATAL=red, ERROR=red, WARN=yellow, INFO=normal, DEBUG=cyan, TRACE=black}")
      .build();
  private static final AbstractStringLayout.Serializer builtInFormatter =
    new PatternLayout.SerializerBuilder()
      .setAlwaysWriteExceptions(true)
      .setDisableAnsi(false)
      .setNoConsoleNoAnsi(false)
      .setDefaultPattern(
        "%highlight{[%d{HH:mm:ss} %level] [%logger{1}]: %minecraftFormatting{%msg}%xEx}{FATAL=red, ERROR=red, WARN=yellow, INFO=normal, DEBUG=cyan, TRACE=black}")
      .build();
  private final List<Consumer<String>> logConsumers = new CopyOnWriteArrayList<>();
  private final QueueWithMaxSize<String> logs = new QueueWithMaxSize<>(300); // Keep max 300 logs

  private SFLogAppender() {
    super("LogPanelAppender", null, null, false, Property.EMPTY_ARRAY);

    ((Logger) LogManager.getRootLogger()).addAppender(this);
  }

  @Override
  public void append(LogEvent event) {
    String formatted;
    if (event.getLoggerName().startsWith("com.soulfiremc")) {
      formatted = builtInFormatter.toSerializable(event);
    } else {
      formatted = formatter.toSerializable(event);
    }

    if (formatted.isBlank()) {
      return;
    }

    for (var consumer : logConsumers) {
      consumer.accept(formatted);
    }

    logs.add(formatted);
  }

  public static class QueueWithMaxSize<E> {
    private final int maxSize;
    private final Queue<E> queue;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public QueueWithMaxSize(int maxSize) {
      this.maxSize = maxSize;
      this.queue = new ArrayBlockingQueue<>(maxSize);
    }

    public boolean add(E element) {
      lock.writeLock().lock();
      try {
        if (queue.size() >= maxSize) {
          queue.poll(); // Remove the oldest element if max size is reached
        }

        return queue.add(element);
      } finally {
        lock.writeLock().unlock();
      }
    }

    public List<E> getNewest(int amount) {
      if (amount > maxSize) {
        throw new IllegalArgumentException("Amount is bigger than max size!");
      }

      lock.readLock().lock();
      try {
        var list = new ArrayList<>(queue);
        var size = list.size();
        var start = size - amount;

        if (start < 0) {
          start = 0;
        }

        return list.subList(start, size);
      } finally {
        lock.readLock().unlock();
      }
    }
  }
}
