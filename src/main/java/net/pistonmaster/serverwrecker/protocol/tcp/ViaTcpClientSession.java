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
package net.pistonmaster.serverwrecker.protocol.tcp;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.codec.PacketCodecHelper;
import com.github.steveice10.packetlib.crypt.PacketEncryption;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.helper.TransportHelper;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpPacketCodec;
import com.github.steveice10.packetlib.tcp.TcpPacketCompression;
import com.github.steveice10.packetlib.tcp.TcpSession;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.exception.CancelCodecException;
import com.viaversion.viaversion.exception.CancelException;
import com.viaversion.viaversion.exception.InformativeException;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import com.viaversion.viaversion.util.PipelineUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
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
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.dns.*;
import io.netty.handler.codec.haproxy.*;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.incubator.channel.uring.IOUringDatagramChannel;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import lombok.Getter;
import net.pistonmaster.serverwrecker.SWConstants;
import net.pistonmaster.serverwrecker.common.SWOptions;
import net.pistonmaster.serverwrecker.viaversion.FrameCodec;
import net.pistonmaster.serverwrecker.viaversion.StorableOptions;
import net.pistonmaster.serverwrecker.viaversion.StorableSession;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.netty.PreNettyLengthCodec;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.baseprotocols.PreNettyBaseProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class ViaTcpClientSession extends TcpSession {
    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";
    private static Class<? extends Channel> CHANNEL_CLASS;
    private static Class<? extends DatagramChannel> DATAGRAM_CHANNEL_CLASS;
    private static EventLoopGroup EVENT_LOOP_GROUP;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String bindAddress;
    private final int bindPort;
    private final ProxyInfo proxy;
    private final PacketCodecHelper codecHelper;
    @Getter
    private final SWOptions options;

    public ViaTcpClientSession(String host, int port, PacketProtocol protocol, ProxyInfo proxy, SWOptions options) {
        this(host, port, "0.0.0.0", 0, protocol, proxy, options);
    }

    public ViaTcpClientSession(String host, int port, String bindAddress, int bindPort, PacketProtocol protocol, ProxyInfo proxy, SWOptions options) {
        super(host, port, protocol);
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
        this.proxy = proxy;
        this.codecHelper = protocol.createHelper();
        this.options = options;
    }

    @Override
    public void connect(boolean wait) {
        if (this.disconnected) {
            throw new IllegalStateException("Session has already been disconnected.");
        }

        boolean debug = getFlag(BuiltinFlags.PRINT_DEBUG, false);

        if (CHANNEL_CLASS == null) {
            createTcpEventLoopGroup();
        }

        try {
            final Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(CHANNEL_CLASS);
            bootstrap.handler(new ChannelInitializer<>() {
                @Override
                public void initChannel(Channel channel) {
                    PacketProtocol protocol = getPacketProtocol();
                    protocol.newClientSession(ViaTcpClientSession.this);

                    channel.config().setOption(ChannelOption.IP_TOS, 0x18);
                    try {
                        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                    } catch (ChannelException e) {
                        if (debug) {
                            System.out.println("Exception while trying to set TCP_NODELAY");
                            e.printStackTrace();
                        }
                    }
                    channel.config().setOption(ChannelOption.TCP_FASTOPEN_CONNECT, true);

                    ChannelPipeline pipeline = channel.pipeline();

                    refreshReadTimeoutHandler(channel);
                    refreshWriteTimeoutHandler(channel);

                    addProxy(pipeline);

                    // This does the extra magic
                    UserConnectionImpl userConnection = new UserConnectionImpl(channel, true);
                    userConnection.put(new StorableOptions(options));
                    userConnection.put(new StorableSession(ViaTcpClientSession.this));

                    ProtocolPipelineImpl protocolPipeline = new ProtocolPipelineImpl(userConnection);

                    ProtocolVersion version = options.protocolVersion();
                    boolean isLegacy = SWConstants.isLegacy(version);

                    if (isLegacy) {
                        protocolPipeline.add(PreNettyBaseProtocol.INSTANCE);
                        pipeline.addLast("vl-prenetty", new PreNettyLengthCodec(userConnection));
                    }

                    pipeline.addLast("sizer", new FrameCodec());

                    // Inject Via codec
                    pipeline.addLast("via-codec", new ViaCodec(userConnection));

                    pipeline.addLast("codec", new TcpPacketCodec(ViaTcpClientSession.this, true));
                    pipeline.addLast("manager", ViaTcpClientSession.this);

                    addHAProxySupport(pipeline);
                }
            }).group(EVENT_LOOP_GROUP).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeout() * 1000);

            InetSocketAddress remoteAddress = resolveAddress();
            bootstrap.remoteAddress(remoteAddress);
            bootstrap.localAddress(bindAddress, bindPort);

            ChannelFuture future = bootstrap.connect();
            if (wait) {
                future.sync();
            }

            future.addListener((futureListener) -> {
                if (!futureListener.isSuccess()) {
                    exceptionCaught(null, futureListener.cause());
                }
            });
        } catch (Throwable t) {
            exceptionCaught(null, t);
        }
    }

    @Override
    public int getCompressionThreshold() {
        throw new UnsupportedOperationException("Not supported method.");
    }

    @Override
    public void setCompressionThreshold(int threshold, boolean validateDecompression) {
        throw new UnsupportedOperationException("Not supported method.");
    }

    @Override
    public void enableEncryption(PacketEncryption encryption) {
        throw new UnsupportedOperationException("Not supported method.");
    }

    @Override
    public PacketCodecHelper getCodecHelper() {
        return this.codecHelper;
    }

    private InetSocketAddress resolveAddress() {
        boolean debug = getFlag(BuiltinFlags.PRINT_DEBUG, false);

        String name = this.getPacketProtocol().getSRVRecordPrefix() + "._tcp." + this.getHost();
        if (debug) {
            System.out.println("[PacketLib] Attempting SRV lookup for \"" + name + "\".");
        }

        if (getFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, true) && (!this.host.matches(IP_REGEX) && !this.host.equalsIgnoreCase("localhost"))) {
            AddressedEnvelope<DnsResponse, InetSocketAddress> envelope = null;
            try (DnsNameResolver resolver = new DnsNameResolverBuilder(EVENT_LOOP_GROUP.next())
                    .channelType(DATAGRAM_CHANNEL_CLASS)
                    .build()) {
                envelope = resolver.query(new DefaultDnsQuestion(name, DnsRecordType.SRV)).get();

                DnsResponse response = envelope.content();
                if (response.count(DnsSection.ANSWER) > 0) {
                    DefaultDnsRawRecord record = response.recordAt(DnsSection.ANSWER, 0);
                    if (record.type() == DnsRecordType.SRV) {
                        ByteBuf buf = record.content();
                        buf.skipBytes(4); // Skip priority and weight.

                        int port = buf.readUnsignedShort();
                        String host = DefaultDnsRecordDecoder.decodeName(buf);
                        if (host.endsWith(".")) {
                            host = host.substring(0, host.length() - 1);
                        }

                        if (debug) {
                            System.out.println("[PacketLib] Found SRV record containing \"" + host + ":" + port + "\".");
                        }

                        this.host = host;
                        this.port = port;
                    } else if (debug) {
                        System.out.println("[PacketLib] Received non-SRV record in response.");
                    }
                } else if (debug) {
                    System.out.println("[PacketLib] No SRV record found.");
                }
            } catch (Exception e) {
                if (debug) {
                    System.out.println("[PacketLib] Failed to resolve SRV record.");
                    e.printStackTrace();
                }
            } finally {
                if (envelope != null) {
                    envelope.release();
                }

            }
        } else if (debug) {
            System.out.println("[PacketLib] Not resolving SRV record for " + this.host);
        }

        // Resolve host here
        try {
            InetAddress resolved = InetAddress.getByName(getHost());
            if (debug) {
                System.out.printf("[PacketLib] Resolved %s -> %s%n", getHost(), resolved.getHostAddress());
            }
            return new InetSocketAddress(resolved, getPort());
        } catch (UnknownHostException e) {
            if (debug) {
                System.out.println("[PacketLib] Failed to resolve host, letting Netty do it instead.");
                e.printStackTrace();
            }
            return InetSocketAddress.createUnresolved(getHost(), getPort());
        }
    }

    private void addProxy(ChannelPipeline pipeline) {
        if (proxy != null) {
            switch (proxy.getType()) {
                case HTTP -> {
                    if (proxy.isAuthenticated()) {
                        pipeline.addFirst("proxy", new HttpProxyHandler(proxy.getAddress(), proxy.getUsername(), proxy.getPassword()));
                    } else {
                        pipeline.addFirst("proxy", new HttpProxyHandler(proxy.getAddress()));
                    }
                }
                case SOCKS4 -> {
                    if (proxy.isAuthenticated()) {
                        pipeline.addFirst("proxy", new Socks4ProxyHandler(proxy.getAddress(), proxy.getUsername()));
                    } else {
                        pipeline.addFirst("proxy", new Socks4ProxyHandler(proxy.getAddress()));
                    }
                }
                case SOCKS5 -> {
                    if (proxy.isAuthenticated()) {
                        pipeline.addFirst("proxy", new Socks5ProxyHandler(proxy.getAddress(), proxy.getUsername(), proxy.getPassword()));
                    } else {
                        pipeline.addFirst("proxy", new Socks5ProxyHandler(proxy.getAddress()));
                    }
                }
                default -> throw new UnsupportedOperationException("Unsupported proxy type: " + proxy.getType());
            }
        }
    }

    private void addHAProxySupport(ChannelPipeline pipeline) {
        InetSocketAddress clientAddress = getFlag(BuiltinFlags.CLIENT_PROXIED_ADDRESS);
        if (getFlag(BuiltinFlags.ENABLE_CLIENT_PROXY_PROTOCOL, false) && clientAddress != null) {
            pipeline.addFirst("proxy-protocol-packet-sender", new ChannelInboundHandlerAdapter() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                    HAProxyProxiedProtocol proxiedProtocol = clientAddress.getAddress() instanceof Inet4Address ? HAProxyProxiedProtocol.TCP4 : HAProxyProxiedProtocol.TCP6;
                    InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    ctx.channel().writeAndFlush(new HAProxyMessage(
                            HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, proxiedProtocol,
                            clientAddress.getAddress().getHostAddress(), remoteAddress.getAddress().getHostAddress(),
                            clientAddress.getPort(), remoteAddress.getPort()
                    ));
                    ctx.pipeline().remove(this);
                    ctx.pipeline().remove("proxy-protocol-encoder");
                    super.channelActive(ctx);
                }
            });
            pipeline.addFirst("proxy-protocol-encoder", HAProxyMessageEncoder.INSTANCE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof CancelCodecException) {
            return;
        }

        super.exceptionCaught(ctx, cause);

        logger.debug("Exception caught in Netty session.", cause);
    }

    @Override
    public void send(Packet packet) {
        Channel channel = getChannel();
        if(channel == null) {
            return;
        }

        PacketSendingEvent sendingEvent = new PacketSendingEvent(this, packet);
        this.callEvent(sendingEvent);

        if (!sendingEvent.isCancelled()) {
            final Packet toSend = sendingEvent.getPacket();
            channel.writeAndFlush(toSend).addListener((ChannelFutureListener) future -> {
                if(future.isSuccess()) {
                    callPacketSent(toSend);
                } else {
                    packetExceptionCaught(null, future.cause(), packet);
                }
            });
        }
    }

    public void packetExceptionCaught(ChannelHandlerContext ctx, Throwable cause, Packet packet) {
        if (cause instanceof CancelCodecException) {
            callPacketSent(packet);
            return;
        }

        super.exceptionCaught(ctx, cause);

        logger.debug("Exception caught in Netty session.", cause);
    }

    public void setCompressionThreshold(int threshold) {
        Channel channel = getChannel();
        if (channel != null) {
            if (threshold >= 0) {
                ChannelHandler handler = channel.pipeline().get("compression");
                if (handler == null) {
                    channel.pipeline().addBefore("via-codec", "compression", new CompressionCodec(threshold));
                } else {
                    ((CompressionCodec) handler).setThreshold(threshold);
                }
            } else if (channel.pipeline().get("compression") != null) {
                channel.pipeline().remove("compression");
            }
        }
    }

    public void enableEncryption(SecretKey key) {
        getChannel().pipeline().addBefore("sizer", "encryption", new CryptoCodec(key, key));
    }

    private static void createTcpEventLoopGroup() {
        if (CHANNEL_CLASS != null) {
            return;
        }

        switch (TransportHelper.determineTransportMethod()) {
            case IO_URING -> {
                EVENT_LOOP_GROUP = new IOUringEventLoopGroup();
                CHANNEL_CLASS = IOUringSocketChannel.class;
                DATAGRAM_CHANNEL_CLASS = IOUringDatagramChannel.class;
            }
            case EPOLL -> {
                EVENT_LOOP_GROUP = new EpollEventLoopGroup();
                CHANNEL_CLASS = EpollSocketChannel.class;
                DATAGRAM_CHANNEL_CLASS = EpollDatagramChannel.class;
            }
            case KQUEUE -> {
                EVENT_LOOP_GROUP = new KQueueEventLoopGroup();
                CHANNEL_CLASS = KQueueSocketChannel.class;
                DATAGRAM_CHANNEL_CLASS = KQueueDatagramChannel.class;
            }
            case NIO -> {
                EVENT_LOOP_GROUP = new NioEventLoopGroup();
                CHANNEL_CLASS = NioSocketChannel.class;
                DATAGRAM_CHANNEL_CLASS = NioDatagramChannel.class;
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> EVENT_LOOP_GROUP.shutdownGracefully()));
    }
}
