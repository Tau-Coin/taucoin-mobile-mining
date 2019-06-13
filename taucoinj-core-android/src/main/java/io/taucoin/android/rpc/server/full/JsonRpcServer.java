package io.taucoin.android.rpc.server.full;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.taucoin.android.rpc.server.full.filter.FilterManager;
import io.taucoin.facade.Taucoin;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.android.rpc.server.full.method.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

import io.taucoin.android.rpc.server.*;


public final class JsonRpcServer extends io.taucoin.android.rpc.server.JsonRpcServer{

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    private Taucoin taucoin;
    private Dispatcher dispatcher;
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    private int port;

    public JsonRpcServer(Taucoin taucoin) {
        super(taucoin);
        this.taucoin = taucoin;

        this.dispatcher = new Dispatcher();

        // Custom methods to receive Address Transaction History
        this.dispatcher.register(new tau_getTransactions(this.taucoin));

        //net
        this.dispatcher.register(new net_version(this.taucoin));
        this.dispatcher.register(new net_listening(this.taucoin));
        this.dispatcher.register(new net_peerCount(this.taucoin));

        //protocol
        this.dispatcher.register(new tau_protocolVersion(this.taucoin));

        //forge
        this.dispatcher.register(new tau_forging(this.taucoin));

        //account
        this.dispatcher.register(new tau_getAccountDetails(this.taucoin));

        //transactions
		//getTransactionByHash, Not supported now for no TransactionStore
        //this.dispatcher.register(new tau_getTransactionByHash(this.taucoin));
        this.dispatcher.register(new tau_sendTransaction(this.taucoin));
        this.dispatcher.register(new tau_getTransactionDetail(this.taucoin));

        //block 
        this.dispatcher.register(new tau_blockNumber(this.taucoin));
        this.dispatcher.register(new tau_getBlockHashList(this.taucoin));
        this.dispatcher.register(new tau_getBlockByHash(this.taucoin));
        this.dispatcher.register(new tau_getBlockByNumber(this.taucoin));

        //db
        this.dispatcher.register(new db_putString(this.taucoin));
        this.dispatcher.register(new db_getString(this.taucoin));
        this.dispatcher.register(new db_putHex(this.taucoin));
        this.dispatcher.register(new db_getHex(this.taucoin));
        this.dispatcher.register(new db_getbestblock(this.taucoin));

        taucoin.addListener(FilterManager.getInstance());
    }

    public void start(int port) throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        this.port = port;
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.localAddress(port);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new JsonRpcServerInitializer());

            Channel ch = b.bind().sync().channel();

            logger.info("Full json rpc server is starting, listen port: {}", this.port);

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    class JsonRpcServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            p.addLast(new HttpServerCodec());
            p.addLast(new JsonRpcServerHandler(dispatcher));
        }
    }
}
