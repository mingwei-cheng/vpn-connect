package cn.cheng.vpn.handle;

import cn.cheng.vpn.common.Global;
import cn.cheng.vpn.secret.SecretFactory;
import cn.cheng.vpn.secret.SecretInterface;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author Cheng Mingwei
 * @create 2020-08-03 15:42
 **/
public class WebSocketHandler {
    private final Vertx vertx;
    private final String id;
    private final String password;
    private final SecretInterface secretImpl;

    public WebSocketHandler(Vertx vertx, String id, JsonObject config) {
        this.vertx = vertx;
        this.id = id;
        String secret = config.getString("secret");
        this.password = config.getString("password");
        secretImpl = SecretFactory.getSecretImpl(secret);
    }

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);


    public void handleMsg(byte[] data) {
        Buffer decode = secretImpl.decode(Buffer.buffer(data), password);
        JsonObject json = new JsonObject(decode);
        Integer cmd = json.getInteger("cmd");
        switch (cmd) {
            case 0x00:
                login(json);
                break;
            case 0x01:
                connect(json);
                break;
            case 0x02:
                sendMsg(json);
                break;
            default:
                logger.info("错误的请求：{}", json);
                break;
        }
    }

    private void login(JsonObject json) {
//        logger.info("收到WS登录请求：{}", json);
        JsonObject re = new JsonObject()
                .put("sn", json.getInteger("sn"))
                .put("result", 200)
                .put("cmd", 0x00);
        sendWS(re);
    }

    private void connect(JsonObject json) {
        String host = json.getString("host");
        Integer port = json.getInteger("port");
        vertx.createNetClient().connect(port, host, result -> {
            NetSocket netSocket = result.result();
            if (result.succeeded()) {
                json.put("result", 200);
                String token = UUID.randomUUID().toString();
                json.put("token", token);
                Global.clientMap.put(token, netSocket);
                logger.info("{}:{}连接成功", host, port);
                SocketAddress socketAddress = netSocket.localAddress();
                json.put("host", socketAddress.host());
                json.put("port", socketAddress.port());
                netSocket.closeHandler(close -> {
                    logger.info("远程连接已关闭：{}", netSocket.remoteAddress());
                    JsonObject closeData = new JsonObject();
                    closeData.put("cmd", 0x03);
                    closeData.put("token", token);
                    sendWS(json);
                    Global.clientMap.remove(token);
                });
            } else {
                if (netSocket != null) {
                    netSocket.close();
                }
                logger.info("{}:{}连接失败{}", host, port, result.cause().getMessage());
                json.put("result", 500);
            }
            sendWS(json);
        });

    }

    private void sendMsg(JsonObject json) {
        String token = json.getString("token");
        byte[] data = json.getBinary("data");
//        logger.info("消息体：{}", ByteBufUtil.hexDump(data));
        NetSocket netSocket = Global.clientMap.get(token);
        if (netSocket != null) {
            if (data[0] == 16 && data[1] == 3 && data[2] == 1 && data[3] == 2) {
                netSocket.upgradeToSsl(v -> {
                    netSocket.handler(re -> {
                        json.put("data", re.getBytes());
                        sendWS(json);
                    });
                    netSocket.write(Buffer.buffer().appendBytes(data));
                });
            } else {
                netSocket.handler(re -> {
                    json.put("data", re.getBytes());
                    sendWS(json);
                });
                netSocket.write(Buffer.buffer().appendBytes(data));
            }
        }
    }

    private void sendWS(JsonObject json) {
        ServerWebSocket serverWebSocket = Global.wsMap.get(id);
        if (serverWebSocket != null) {
            Buffer encode = secretImpl.encode(json.toBuffer(), password);
            serverWebSocket.write(encode);
        }
    }
}
