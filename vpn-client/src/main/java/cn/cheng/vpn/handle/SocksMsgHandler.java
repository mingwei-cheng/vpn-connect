package cn.cheng.vpn.handle;


import cn.cheng.vpn.common.Global;
import io.netty.buffer.ByteBuf;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Cheng Mingwei
 * @create 2020-08-03 10:40
 **/
public class SocksMsgHandler {

    private final Vertx vertx;
    public NetSocket netSocket;

    public SocksMsgHandler(Vertx vertx, NetSocket netSocket) {
        this.vertx = vertx;
        this.netSocket = netSocket;
    }

    private static final Logger logger = LoggerFactory.getLogger(SocksMsgHandler.class);

    private int count = 0;

    public void msgHandle(Buffer msg) {
        ByteBuf byteBuf = msg.getByteBuf();
        if (count == 0) {
            byte type = byteBuf.readByte();
            //头协议
            if (type == 0x05) {
                //客户端支持的认证方法数
                byte methods = byteBuf.readByte();
                //有哪些可支持的方法，详细列表 0x00 无需认证  0x01 GSSAPI 0x02 用户名密码认证 0xFF 无合适方法
                ByteBuf method = byteBuf.readBytes(methods);
                Buffer re = Buffer.buffer()
                        .appendByte((byte) 0x05)
                        //Server选择支持的认证方法返回
                        .appendByte((byte) 0x00);
                count++;
                if (Global.msgHandleMap.containsKey(netSocket.remoteAddress().port())) {
                    netSocket.write(re);
                }
            }
            //具体数据
        } else if (count == 1) {
            byte type = byteBuf.readByte();
            //头协议
            if (type == 0x05) {
                //0x01 连接上游服务器 0x02 BIND绑定,客户端会接收来自代理服务器的链接 0x03 UDP
                byte cmd = byteBuf.readByte();
                byte rsv = byteBuf.readByte();
                //0x01 ipv4 0x03域名 0x04 ipv6
                byte atyp = byteBuf.readByte();
                String addr;
                switch (atyp) {
                    case 0x01:
                        addr = String.valueOf(byteBuf.readInt());
                        break;
                    case 0x03:
                        //第一个字符为域名长度
                        byte b = byteBuf.readByte();
                        byte[] domain = new byte[b];
                        byteBuf.readBytes(domain);
                        addr = new String(domain);
                        break;
                    case 0x04:
                        byte[] ipv6 = new byte[16];
                        byteBuf.readBytes(ipv6);
                        addr = new String(ipv6);
                        break;
                    default:
                        logger.info("收到的数据有误");
                        return;
                }
                short port = byteBuf.readShort();

                logger.info("申请连接到{}:{}", addr, port);

                JsonObject data = new JsonObject();
                int sn = Global.getSn();
                data.put("atyp", atyp);
                data.put("sn", sn);
                data.put("cmd", 0x01);
                data.put("host", addr);
                data.put("port", port);
                Global.snMap.put(sn, netSocket);
                sendWS(data);
                count++;
            }
        } else {
            //具体数据，由服务端颁发的令牌来标识
            String token = Global.tokenMap.get(netSocket.remoteAddress().port());
            if (token != null) {
                JsonObject data = new JsonObject();
                int sn = Global.getSn();
                data.put("sn", sn);
                data.put("cmd", 0x02);
                data.put("token", token);
                data.put("data", msg.getBytes());
                Global.snMap.put(sn, netSocket);
                logger.info("netSocket:{}:{}", netSocket.remoteAddress(), sn);
                sendWS(data);
            }
        }
    }

    public void sendWS(JsonObject data) {
        vertx.eventBus().send(Global.WS_SEND_MSG, data);
    }

}
