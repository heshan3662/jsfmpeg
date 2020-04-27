# netty_demos.  

参考 [jsmpeg-html5播放rtsp方案](https://my.oschina.net/chengpengvb/blog/1832469?tdsourcetag=s_pctim_aiomsg) ，由于后台是java服务，需要将node服务器转为java服务器 



涉及的相关技术及版本如下。

* Netty 4.1.48.Final
 

 ###建立netty服务，绑定8081端口！
 

 ###HttpServerCodec解码
 
 需简单了解netty对http协议解析原理解析:
 
 将请求分成request、content、LastContent三部分
 
    Request = header 
 
    Content = body
  
    LastContent = 结束语
 
 ###业务逻辑

 Request中区分http和websocket
 
 ws请求的channal升级为websocket协议，放入ChannelGroup中
 
 ffmpeg推流到http协议服务
 
 http请求的content直接用ChannelGroup广播
 
 
 
 ###操作
 idea导入此项目
 
 server类 -> run  
 
 ffmpeg安装后执行ffmpeg指令：
 ffmpeg  -rtsp_transport tcp  -i  "rtsp://username:password@ip:port/" -q 0 -buffer_size 1024  -max_delay 2000 -f mpegts -vcodec copy -codec:v mpeg1video -s 800x600 http://127.0.0.1:8081/ws/live3

 打开viewDemo.html页面
 
###说明
项目进行一半发现硬件原因无法满足mpeg1video编码，而且硬件要求较高， 转向其他格式。代码只是简单实现功能，未做优化！
FFmpeg各版本支持的命令请参考[FFmpeg官方文档](http://ffmpeg.org/ffmpeg.html) 

