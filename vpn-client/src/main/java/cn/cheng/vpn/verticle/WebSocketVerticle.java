package cn.cheng.vpn.verticle;

import cn.cheng.vpn.common.Global;
import cn.cheng.vpn.handle.WebSocketHandler;
import cn.cheng.vpn.secret.SecretFactory;
import cn.cheng.vpn.secret.SecretInterface;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Cheng Mingwei
 * @create 2020-08-03 13:55
 **/
public class WebSocketVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketVerticle.class);

    int retryCount = 0;

    @Override
    public void start(Future future) throws Exception {
        HttpClientOptions httpClientOptions = new HttpClientOptions();
        httpClientOptions.setSendBufferSize(1024 * 1024)
                .setReceiveBufferSize(1024 * 1024)
                .setMaxWebsocketFrameSize(1024 * 1024);

        HttpClient httpClient = vertx.createHttpClient(httpClientOptions);
        //创建WS连接
        httpClient.websocket(config().getInteger("ws.port"), config().getString("ws.host"), "/ws", websocket -> {
            logger.info("成功连接到：{}", websocket.remoteAddress());
            Global.webSocket = websocket;
            retryCount = 0;
            initListen();

            //WS登录
            vertx.eventBus().send(
                    Global.WS_SEND_MSG,
                    new JsonObject()
                            .put("cmd", 0x00)
                            .put("sn", Global.getSn())
            );

            //创建消息监听器
            vertx.eventBus().<JsonObject>consumer(Global.WS_MSG, json -> {
                JsonObject body = json.body();
                Integer cmd = body.getInteger("cmd");
                if (cmd == 0x00) {
                    login(body, future);
                } else {
                    new WebSocketHandler().msgHandle(body);
                }
            });

        }, re -> {
            logger.error(re.getMessage());
            future.fail(re.getMessage());
        });

    }

    /**
     * WS登录
     *
     * @param jsonObject
     * @param future
     */
    private void login(JsonObject jsonObject, Future future) {
        Integer result = jsonObject.getInteger("result");
        if (result == 200) {
            logger.info("WS登陆成功！");
            future.succeeded();
        } else {
            future.fail("登录失败");
            Global.webSocket = null;
        }
        future.complete();
    }

    public void initListen() {
        //接收要通过WS发送的数据
        vertx.eventBus().<JsonObject>consumer(Global.WS_SEND_MSG, json -> {
            logger.info("WS发送消息：{}", json.body());
            WebSocket websocket = Global.webSocket;
            if (websocket != null) {
                SecretInterface secretImpl = SecretFactory.getSecretImpl(config());
                Buffer encode = secretImpl.encode(json.body().toBuffer(), config().getString("password"));
                websocket.write(encode);
            }
        });
        WebSocket websocket = Global.webSocket;
        //当WS关闭时
        websocket.closeHandler(rs -> {
            logger.info("WS连接关闭：{}", websocket.remoteAddress());
            Global.webSocket = null;
            if (retryCount < 5) {
                try {
                    start(Future.future());
                } catch (Exception e) {
                    logger.error("连接WS出现异常：{}", e.getMessage());
                }
                retryCount++;
            }
        });

        //处理WS连接
        websocket.handler(rs -> {
            logger.info("WS收到消息：{}", ByteBufUtil.hexDump(rs.getBytes()));
            SecretInterface secretImpl = SecretFactory.getSecretImpl(config());
            Buffer decode = secretImpl.decode(rs, config().getString("password"));
            vertx.eventBus().send(Global.WS_MSG, new JsonObject(decode));
        });
    }
}
