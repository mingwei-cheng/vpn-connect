package cn.cheng.vpn.verticle;

import cn.cheng.vpn.common.Global;
import cn.cheng.vpn.handle.SocksMsgHandler;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


/**
 * @author Cheng Mingwei
 * @create 2020-08-03 10:05
 **/
public class SocketVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(SocketVerticle.class);
    //断联重试
    int count = 0;

    @Override
    public void start(final Future future) throws Exception {
        NetServerOptions netServerOptions = new NetServerOptions();
        //设置socks5连接的超时时间，支持最大数据包大小
        netServerOptions.setIdleTimeout(60)
                .setIdleTimeoutUnit(TimeUnit.SECONDS)
                .setSendBufferSize(1024 * 1024)
                .setReceiveBufferSize(1024 * 1024);
        vertx.eventBus().<JsonObject>consumer(Global.SOCKS_MSG, m -> {
            JsonObject body = m.body();
            byte[] data = body.getBinary("data");
            Integer from = body.getInteger("from");
            SocksMsgHandler msgHandler = Global.msgHandleMap.get(from);
            if (msgHandler != null) {
                msgHandler.msgHandle(Buffer.buffer(data));
            } else {
                logger.info("{}，已失效", from);
            }
        });

        vertx.createNetServer(netServerOptions).connectHandler(r -> {
            logger.info("新连接{}:{}", r.remoteAddress(), r.isSsl());
            //建立连接成功时
            SocksMsgHandler socksMsgHandler = new SocksMsgHandler(vertx, r);
            Global.msgHandleMap.put(r.remoteAddress().port(), socksMsgHandler);
            count++;
            logger.info("已有{}个连接", count);

            //收到消息时
            r.handler(msg -> {
                logger.info("{}收到消息：{}", r.remoteAddress().port(), ByteBufUtil.hexDump(msg.getBytes()));
                JsonObject jsonObject = new JsonObject()
                        .put("data", msg.getBytes())
                        .put("from", r.remoteAddress().port());
                vertx.eventBus().send(Global.SOCKS_MSG, jsonObject);
            });

            //关闭连接时
            r.closeHandler(close -> {
                count--;
                SocketAddress socketAddress = r.remoteAddress();
                int port = socketAddress.port();
                SocksMsgHandler socksHandler = Global.msgHandleMap.get(port);
                if (socksHandler != null) {
                    if (socksHandler.netSocket != null) {
                        socksHandler.netSocket.close();
                        socksHandler.netSocket = null;
                    }
                    Global.msgHandleMap.remove(port);
                }
                Global.tokenMap.remove(port);
                logger.info("连接关闭{}", socketAddress);
                logger.info("还剩{}个连接", count);
            });
        }).listen(config().getInteger("server.port", 10801), result -> {
            if (result.succeeded()) {
                logger.info("服务创建成功");
                future.complete();
            } else {
                logger.info("服务创建失败");
                future.fail(result.cause());
            }
        });
    }
}
