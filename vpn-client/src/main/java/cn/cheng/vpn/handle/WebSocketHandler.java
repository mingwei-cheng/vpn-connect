package cn.cheng.vpn.handle;


import cn.cheng.vpn.common.Global;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * @author Cheng Mingwei
 * @create 2020-08-03 10:40
 **/
public class WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    public void msgHandle(JsonObject jsonObject) {
        Integer cmd = jsonObject.getInteger("cmd");
        switch (cmd) {
            case 0x01:
                connect(jsonObject);
                break;
            case 0x02:
                sendMsg(jsonObject);
                break;
            case 0x03:
                close(jsonObject);
                break;
            default:
                logger.info("WS收到未知消息：{}", jsonObject);
                break;
        }
    }

    private void close(JsonObject jsonObject) {
        String token = jsonObject.getString("token");
        for (Integer next : Global.tokenMap.keySet()) {
            if (Global.tokenMap.get(next).equals(token)) {
                SocksMsgHandler socksMsgHandler = Global.msgHandleMap.get(next);
                if (socksMsgHandler != null) {
                    socksMsgHandler.netSocket.close();
                    Global.msgHandleMap.remove(next);
                    Global.tokenMap.remove(next);
                    logger.info("连接被远程关闭：{}", socksMsgHandler.netSocket.remoteAddress());
                }
                return;
            }
        }
    }

    private void sendMsg(JsonObject jsonObject) {
        NetSocket netSocket = Global.snMap.get(jsonObject.getInteger("sn"));
        if (Global.msgHandleMap.containsKey(netSocket.remoteAddress().port())) {
            byte[] data = jsonObject.getBinary("data");
            netSocket.write(Buffer.buffer(data));
//            logger.info("{}:0x03回复：{}", netSocket.remoteAddress(), new String(data));
        }
    }

    private void connect(JsonObject jsonObject) {
        NetSocket netSocket = Global.snMap.get(jsonObject.getInteger("sn"));
        Integer result = jsonObject.getInteger("result");
        String token = jsonObject.getString("token");
        String host = jsonObject.getString("host");
        int atyp = jsonObject.getInteger("atyp");
        int port = jsonObject.getInteger("port");
        Buffer buffer = Buffer.buffer();
        if (result == 200) {
            Global.tokenMap.put(netSocket.remoteAddress().port(), token);
            buffer.appendByte((byte) 0x05)
                    .appendByte((byte) 0x00)
                    .appendByte((byte) 0x00)
                    .appendByte((byte) atyp);
            if (atyp == 0x03) {
                buffer.appendByte((byte) host.getBytes().length);
            }
            buffer.appendBytes(host.getBytes())
                    .appendShort((short) port);
            logger.info("{}:{}连接成功", host, port);
            if (Global.msgHandleMap.containsKey(netSocket.remoteAddress().port())) {
//                logger.info("0x02回复：{}", ByteBufUtil.hexDump(buffer.getBytes()));
                netSocket.write(buffer);
            }
        } else {
            buffer.appendByte((byte) 0x05)
                    .appendByte((byte) 0x03)
                    .appendByte((byte) 0x00)
                    .appendByte((byte) atyp);
            if (atyp == 0x03) {
                buffer.appendByte((byte) host.getBytes().length);
            }
            buffer.appendBytes(host.getBytes())
                    .appendShort((short) port);
            logger.info("{}:{}连接失败", host, port);
            if (Global.msgHandleMap.containsKey(netSocket.remoteAddress().port())) {
//                logger.info("0x02回复：{}", new String(buffer.getBytes()));
                netSocket.write(buffer);
            }
            netSocket.close();
        }
    }

}
