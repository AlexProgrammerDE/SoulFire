/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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
package net.pistonmaster.serverwrecker.protocol.netty;

import com.github.steveice10.packetlib.helper.TransportHelper;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.incubator.channel.uring.IOUringDatagramChannel;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringSocketChannel;

public class SWNettyHelper {
    public static final Class<? extends Channel> CHANNEL_CLASS;
    public static final Class<? extends DatagramChannel> DATAGRAM_CHANNEL_CLASS;

    static {
        TransportHelper.TransportMethod transportMethod = TransportHelper.determineTransportMethod();
        switch (transportMethod) {
            case IO_URING -> {
                CHANNEL_CLASS = IOUringSocketChannel.class;
                DATAGRAM_CHANNEL_CLASS = IOUringDatagramChannel.class;
            }
            case EPOLL -> {
                CHANNEL_CLASS = EpollSocketChannel.class;
                DATAGRAM_CHANNEL_CLASS = EpollDatagramChannel.class;
            }
            case KQUEUE -> {
                CHANNEL_CLASS = KQueueSocketChannel.class;
                DATAGRAM_CHANNEL_CLASS = KQueueDatagramChannel.class;
            }
            case NIO -> {
                CHANNEL_CLASS = NioSocketChannel.class;
                DATAGRAM_CHANNEL_CLASS = NioDatagramChannel.class;
            }
            default -> throw new IllegalStateException("Unexpected value: " + transportMethod);
        }
    }

    public static EventLoopGroup createEventLoopGroup() {
        EventLoopGroup group = switch (TransportHelper.determineTransportMethod()) {
            case IO_URING -> new IOUringEventLoopGroup();
            case EPOLL -> new EpollEventLoopGroup();
            case KQUEUE -> new KQueueEventLoopGroup();
            case NIO -> new NioEventLoopGroup();
        };

        Runtime.getRuntime().addShutdownHook(new Thread(group::shutdownGracefully));

        return group;
    }
}
