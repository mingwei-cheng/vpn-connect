package cn.cheng.vpn;

import io.vertx.core.Launcher;

/**
 * @author Cheng Mingwei
 * @create 2020-08-03 10:26
 **/
public class AppLauncher extends Launcher {

    public static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        System.setProperty("vertx.disableDnsResolver", "true");
        new AppLauncher().dispatch(args);
    }
}
