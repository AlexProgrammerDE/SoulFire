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
package net.pistonmaster.soulfire.server.protocol.netty;

import com.github.steveice10.packetlib.helper.TransportHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
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
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.incubator.channel.uring.IOUringDatagramChannel;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
import net.pistonmaster.soulfire.proxy.SWProxy;

import java.util.concurrent.ThreadFactory;

public class SWNettyHelper {
    public static final Class<? extends Channel> CHANNEL_CLASS;
    public static final Class<? extends DatagramChannel> DATAGRAM_CHANNEL_CLASS;

    static {
        var transportMethod = TransportHelper.determineTransportMethod();
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

    private SWNettyHelper() {
    }

    public static EventLoopGroup createEventLoopGroup(int threads, String name) {
        ThreadFactory threadFactory = r -> new Thread(r, "SoulFire-" + name);
        EventLoopGroup group = switch (TransportHelper.determineTransportMethod()) {
            case IO_URING -> new IOUringEventLoopGroup(threads, threadFactory);
            case EPOLL -> new EpollEventLoopGroup(threads, threadFactory);
            case KQUEUE -> new KQueueEventLoopGroup(threads, threadFactory);
            case NIO -> new NioEventLoopGroup(threads, threadFactory);
        };

        Runtime.getRuntime().addShutdownHook(new Thread(group::shutdownGracefully));

        return group;
    }

    public static void addProxy(ChannelPipeline pipeline, SWProxy proxy) {
        var address = proxy.getInetSocketAddress();
        switch (proxy.type()) {
            case HTTP -> {
                if (proxy.username() != null && proxy.password() != null) {
                    pipeline.addFirst("proxy", new HttpProxyHandler(address, proxy.username(), proxy.password()));
                } else {
                    pipeline.addFirst("proxy", new HttpProxyHandler(address));
                }
            }
            case SOCKS4 -> pipeline.addFirst("proxy", new Socks4ProxyHandler(address, proxy.username()));
            case SOCKS5 ->
                    pipeline.addFirst("proxy", new Socks5ProxyHandler(address, proxy.username(), proxy.password()));
            default -> throw new UnsupportedOperationException("Unsupported proxy type: " + proxy.type());
        }
    }
}
