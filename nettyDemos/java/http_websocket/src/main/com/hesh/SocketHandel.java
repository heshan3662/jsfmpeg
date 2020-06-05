package com.hesh;

 import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.HeadersUtils;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
 import io.netty.util.ReferenceCountUtil;

 import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SocketHandel extends BaseHttpHandler {
    private WebSocketServerHandshaker handshaker;
    private final String wsUri = "/ws";
    /*
     * channelAction
     *
     * channel 通道 action 活跃的
     *
     * 当客户端主动链接服务端的链接后，这个通道就是活跃的了。也就是客户端与服务端建立了通信通道并且可以传输数据
     *
     */
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().localAddress().toString() + " 通道已激活！");
    }
    /*
     * channelInactive
     *
     */
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().localAddress().toString() + " 通道不活跃！");
        // 关闭流
    }
    private String getMessage(ByteBuf buf) {
        byte[] con = new byte[buf.readableBytes()];
        buf.readBytes(con);
        try {
            return new String(con, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 功能：读取服务器发送过来的信息
     * pipeline添加了HttpObjectAggregator后可直接区分msg类型，但是会请求体整体打包。对视频流chunked传输数据无法接受！
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //netty以整体接收的话
//        if (msg instanceof FullHttpRequest) {// 如果是HTTP请求，进行HTTP操作
//            System.out.println("into hettpHandle");
//            handleHttpRequest(ctx, (FullHttpRequest) msg);
//        } else if (msg instanceof WebSocketFrame) {// 如果是Websocket请求，则进行websocket操作
//            System.out.println("into websockethandel");
//            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
//        }
        //netty分块接受参数
        if (msg instanceof  DefaultHttpRequest){
//            System.out.println("into DefaultHttpRequest");
             handleDefaultHttpRequest(ctx, (DefaultHttpRequest) msg);
        }else  if(msg instanceof  DefaultHttpContent){
//            System.out.println("into DefaultHttpContent");
            handleDefaultHttpContent(ctx, (DefaultHttpContent) msg);
        }else if(msg instanceof   LastHttpContent ){
//             System.out.println("into LastHttpContent");
             handleLastHttpContent(ctx, (LastHttpContent) msg);
        }
        ReferenceCountUtil.release(msg);
    }

    // 处理HTTP Header的代码
    private void handleDefaultHttpRequest(ChannelHandlerContext ctx, DefaultHttpRequest req) throws UnsupportedEncodingException {
        System.out.println("DefaultHttpRequest :" + req.toString());
        List  entries = req.headers().entries();
        Iterator iterator = HeadersUtils.iteratorAsString(entries) ;
        //websocket protocol
        String SecWebSocketProtocol  = "";
        boolean isWebsocket = false ;
        while(iterator.hasNext()) {
            //iterator.next()返回迭代的下一个元素
            Map.Entry entry = (Map.Entry) iterator.next();
            if(entry.getKey().equals("Upgrade")){
                if(entry.getValue().equals("websocket")){
                    isWebsocket = true;
                }
            }
            if(entry.getKey().equals("Sec-WebSocket-Protocol")){
                SecWebSocketProtocol=   entry.getValue().toString();
            }
        }
        // 如果是websocket请求就握手升级
        if(isWebsocket){
            PlayerGroup.addChannel(ctx.channel());
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    "/ws", SecWebSocketProtocol, true);
            handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req);
            }
        }
    }

    // 处理HTTP content的代码
    private void handleDefaultHttpContent(ChannelHandlerContext ctx, DefaultHttpContent req) throws UnsupportedEncodingException {
       if(req.content()!= null ) {
           PlayerGroup.broadCast(req.content());
       }
     }

    // 处理HTTP 结尾的代码，返回response ，http 和websocket 怎么区分
    private void handleLastHttpContent(ChannelHandlerContext ctx, LastHttpContent req) throws UnsupportedEncodingException {
        System.out.println("LastHttpContent get :"+  req.toString());
        ByteBuf bf = Unpooled.copiedBuffer("", CharsetUtil.UTF_8);
        FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, bf);
        res.headers().set(HttpHeaderNames.CONTENT_LENGTH, "".length());
        ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

    }

    // 处理HTTP  请求体FullHttpRequest的代码
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws UnsupportedEncodingException {
        System.out.println("httprequest get");
        System.out.println("handleHttpRequest method==========" + req.getMethod());
        System.out.println("handleHttpRequest uri==========" + req.getUri());
        // 如果HTTP解码失败，返回HHTP异常
        Map<String, String> parmMap = new HashMap<>();
        if (req instanceof HttpRequest) {
            HttpMethod method = req.getMethod();
            System.out.println("this is httpconnect");
            // 如果是websocket请求就握手升级
            if (wsUri.equalsIgnoreCase(req.getUri())) {
                System.out.println("websocket 请求接入");
                WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                        "/ws", "live3", true);
                handshaker = wsFactory.newHandshaker(req);
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    handshaker.handshake(ctx.channel(), req);
                }
            }
            if (HttpMethod.POST == method) {
                // 是POST请求
                HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
                decoder.offer(req);
                System.out.println(decoder.getBodyHttpDatas());
            }
            if (HttpMethod.GET == method) {
                // 是GET请求
                System.out.println(req.content());
                // 编码解码
                ByteBuf in = (ByteBuf) req.content();
                byte[] byt = new byte[in.readableBytes()];
                in.readBytes(byt);
                String body = new String(byt, "UTF-8")+"welcome";
                System.out.println("server channelRead...; received收到客户端消息:" + body);
                QueryStringDecoder decoder = new QueryStringDecoder(req.getUri());
                System.out.println(decoder.toString());
                /*
                 * ctx.channel().writeAndFlush(new
                 * TextWebSocketFrame("服务端数据"+body));
                 */

                ByteBuf bf = Unpooled.copiedBuffer(body, CharsetUtil.UTF_8);

                FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, bf);
                res.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length());
                ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                // 将数据写入通道
//                channels.writeAndFlush(new TextWebSocketFrame(body));
             }
        }
    }

    // 握手请求不成功时返回的应答
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // 返回应答给客户端
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
    }

    // 处理Websocket的代码
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否是关闭链路的指令
        System.out.println("websocket get");
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否是Ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 文本消息，不支持二进制消息
        if (frame instanceof TextWebSocketFrame) {
            // 返回应答消息
            String request = ((TextWebSocketFrame) frame).text();
            ctx.channel().writeAndFlush(new TextWebSocketFrame(
                    request + " , 欢迎使用Netty WebSocket服务，现在时刻：" + new java.util.Date().toString()));
        }
    }
    /**
     * 功能：服务端发生异常的操作
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        System.out.println("异常信息：\r\n" + cause.getMessage());
    }
}
