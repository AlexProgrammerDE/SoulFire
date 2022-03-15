/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.List;

@RequiredArgsConstructor
public class LogAppender implements Appender<ILoggingEvent> {
    private final JTextArea logArea;
    private final LogFormatter formatter = new LogFormatter();

    @Override
    public void doAppend(ILoggingEvent iLoggingEvent) throws LogbackException {
        String formatted = formatter.format(iLoggingEvent);

        if (formatted.isEmpty())
            return;

        SwingUtilities.invokeLater(() -> logArea.append(formatted));
    }

    @Override
    public String getName() {
        return "LogPanelAppender";
    }

    @Override
    public void setName(String s) {
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public void setContext(Context context) {
    }

    @Override
    public void addStatus(Status status) {
    }

    @Override
    public void addInfo(String msg) {
    }

    @Override
    public void addInfo(String msg, Throwable ex) {
    }

    @Override
    public void addWarn(String msg) {
    }

    @Override
    public void addWarn(String msg, Throwable ex) {
    }

    @Override
    public void addError(String msg) {
    }

    @Override
    public void addError(String msg, Throwable ex) {
    }

    @Override
    public void addFilter(Filter<ILoggingEvent> newFilter) {
    }

    @Override
    public void clearAllFilters() {
    }

    @Override
    public List<Filter<ILoggingEvent>> getCopyOfAttachedFiltersList() {
        return null;
    }

    @Override
    public FilterReply getFilterChainDecision(ILoggingEvent event) {
        return null;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isStarted() {
        return false;
    }
}
