package com.hesh;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * HTTP Server ChannelInitializer.
 *
 */
public class Initializer extends ChannelInitializer<SocketChannel> {



    @Override
    protected void initChannel(SocketChannel ch) throws Exception {


        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());// Http消息编码解码,包含HttpRequestDecoder, HttpResponseEncoder

//        pipeline.addLast(new HttpObjectAggregator(64*1024)); //封装完整FullHttpRequest
//        pipeline.addLast(new ChunkedWriteHandler());
//         pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        pipeline.addLast(new SocketHandel());//自定义处理类


      }

}
