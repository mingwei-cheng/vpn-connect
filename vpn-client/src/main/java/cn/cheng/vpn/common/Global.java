package cn.cheng.vpn.common;

import cn.cheng.vpn.handle.SocksMsgHandler;
import com.sun.applet2.AppletParameters;
import io.vertx.core.http.WebSocket;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Cheng Mingwei
 * @create 2020-08-03 10:31
 **/
public class Global {
    public static final String SOCKS_MSG = "socks.msg";
    public static final String WS_MSG = "ws.msg";
    public static final String WS_SEND_MSG = "ws.send.msg";


    public static Map<Integer, NetSocket> snMap = new ConcurrentHashMap<>();
    public static Map<Integer, SocksMsgHandler> msgHandleMap = new ConcurrentHashMap<>();
    public static Map<Integer, String> tokenMap = new ConcurrentHashMap<>();

    public static WebSocket webSocket;

    private static int sn;

    public static int getSn() {
        if (sn >= Integer.MAX_VALUE - 1) {
            sn = 0;
        } else {
            sn++;
        }
        return sn;
    }
}
