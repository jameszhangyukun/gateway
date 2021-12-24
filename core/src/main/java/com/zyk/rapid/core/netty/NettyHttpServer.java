package com.zyk.rapid.core.netty;

import com.zyk.gateway.common.util.RemotingHelper;
import com.zyk.gateway.common.util.RemotingUtil;
import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.LifeCycle;
import com.zyk.rapid.core.netty.processor.NettyProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * 接收所有的网络请求
 */
@Slf4j
@Data
public class NettyHttpServer implements LifeCycle {

    private GatewayConfig gatewayConfig;

    private int port = 8888;

    private ServerBootstrap serverBootstrap;

    private EventLoopGroup eventLoopGroupBoss;

    private EventLoopGroup eventLoopGroupWork;

    private NettyProcessor nettyProcessor;

    public NettyHttpServer(GatewayConfig gatewayConfig, NettyProcessor nettyProcessor) {
        this.gatewayConfig = gatewayConfig;
        this.nettyProcessor = nettyProcessor;
        if (gatewayConfig.getPort() > 0 && gatewayConfig.getPort() < 65535) {
            this.port = gatewayConfig.getPort();
        }
        // 初始化nettyServer
        init();
    }

    @Override
    public void init() {
        this.serverBootstrap = new ServerBootstrap();
        if (useEPoll()) {
            this.eventLoopGroupBoss = new EpollEventLoopGroup(gatewayConfig.getEventLLoopGroupBossNum(), new DefaultThreadFactory("NettyBossEPoll"));
            this.eventLoopGroupWork = new EpollEventLoopGroup(gatewayConfig.getEventLoopGroupWorkNum(), new DefaultThreadFactory("NettyWorkEPoll"));
        } else {
            this.eventLoopGroupBoss = new NioEventLoopGroup(gatewayConfig.getEventLLoopGroupBossNum(), new DefaultThreadFactory("NettyBossNio"));
            this.eventLoopGroupWork = new NioEventLoopGroup(gatewayConfig.getEventLoopGroupWorkNum(), new DefaultThreadFactory("NettyWorkNio"));
        }
    }

    private boolean useEPoll() {
        return gatewayConfig.isUseEPoll() && RemotingUtil.isLinuxPlatform() && Epoll.isAvailable();
    }

    @Override
    public void start() {
        ServerBootstrap handler = this.serverBootstrap
                .group(eventLoopGroupBoss, eventLoopGroupBoss)
                .channel(useEPoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, 65535)
                .childOption(ChannelOption.SO_RCVBUF, 65535)
                .localAddress(new InetSocketAddress(this.port))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new HttpServerCodec(),
                                new HttpObjectAggregator(gatewayConfig.getMaxContentLength()),
                                new HttpServerExpectContinueHandler(),
                                new NettyServerConnectManagerHandler(),
                                new NettyHttpServerHandler(nettyProcessor));
                    }
                });
        if (gatewayConfig.isNettyAllocator()) {
            handler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
        try {
            this.serverBootstrap.bind().sync();
            log.info("< ============= Rapid Server StartUp On Port: " + this.port + "================ >");
        } catch (Exception e) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() fail!", e);
        }
    }

    @Override
    public void shutdown() {
        if (eventLoopGroupWork != null) eventLoopGroupWork.shutdownGracefully();
        if (eventLoopGroupBoss != null) eventLoopGroupBoss.shutdownGracefully();
    }

    static class NettyServerConnectManagerHandler extends ChannelDuplexHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.debug("NETTY SERVER PIPLINE: channelRegistered {}", remoteAddr);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.debug("NETTY SERVER PIPLINE: channelUnregistered {}", remoteAddr);
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.debug("NETTY SERVER PIPLINE: channelActive {}", remoteAddr);
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.debug("NETTY SERVER PIPLINE: channelInactive {}", remoteAddr);
            super.channelInactive(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                    log.warn("NETTY SERVER PIPLINE: userEventTriggered: IDLE {}", remoteAddr);
                    ctx.channel().close();
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.warn("NETTY SERVER PIPLINE: remoteAddr： {}, exceptionCaught {}", remoteAddr, cause);
            ctx.channel().close();
        }

    }
}
