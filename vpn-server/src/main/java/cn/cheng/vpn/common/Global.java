package cn.cheng.vpn.common;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.NetSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Cheng Mingwei
 * @create 2020-08-03 15:41
 **/
public class Global {
    public static final String WS_MSG = "ws.msg";
    public static final String WS_SEND_MSG = "ws.send.msg";

    public static Map<String, NetSocket> clientMap = new ConcurrentHashMap<>();
    public static Map<String, ServerWebSocket> wsMap = new ConcurrentHashMap<>();
}
