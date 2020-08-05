package cn.cheng.vpn.verticle;

import cn.cheng.vpn.common.Global;
import cn.cheng.vpn.handle.WebSocketHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Cheng Mingwei
 * @create 2020-08-03 13:55
 **/
public class WebSocketVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketVerticle.class);

    @Override
    public void start() throws Exception {

        vertx.eventBus().<JsonObject>consumer(Global.WS_MSG, json -> {
            String id = json.body().getString("id");
            byte[] data = json.body().getBinary("data");
            new WebSocketHandler(vertx, id, config()).handleMsg(data);
        });

        vertx.createHttpServer().websocketHandler(websocket -> {
            logger.info("WS收到新连接,{}", websocket.remoteAddress());
            String wsId = websocket.binaryHandlerID();
            Global.wsMap.put(wsId, websocket);

            websocket.handler(re -> {
//                logger.info("WS收到新消息：{}", ByteBufUtil.hexDump(re.getBytes()));
                JsonObject object = new JsonObject();
                object.put("data", re.getBytes());
                object.put("id", wsId);
                vertx.eventBus().send(Global.WS_MSG, object);
            });

            websocket.closeHandler(close -> {
                Global.wsMap.remove(wsId);
            });


        }).listen(config().getInteger("ws.port"), "0.0.0.0", re -> logger.info("WS在{}开启成功", re.result().actualPort()));

    }
}
