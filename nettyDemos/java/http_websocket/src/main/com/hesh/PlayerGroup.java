package com.hesh;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.concurrent.ImmediateEventExecutor;



public class PlayerGroup {
    static private ChannelGroup channelGroup
            = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    static public void addChannel(Channel channel) {
        channelGroup.add(channel);
    }

    static public void removeChannel(Channel channel) {
        channelGroup.remove(channel);
    }

    static public void broadCast(ByteBuf message) {
        if (channelGroup == null || channelGroup.isEmpty()) {
            return;
        }
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(message);
        message.retain();

        channelGroup.writeAndFlush(frame);
    }

    static public void destory() {
        if (channelGroup == null || channelGroup.isEmpty()) {
            return;
        }
        channelGroup.close();
    }
}

