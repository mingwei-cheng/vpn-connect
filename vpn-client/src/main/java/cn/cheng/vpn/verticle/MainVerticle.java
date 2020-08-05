package cn.cheng.vpn.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;

/**
 * @author Cheng Mingwei
 * @create 2020-08-03 14:20
 **/
public class MainVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        vertx.deployVerticle(new WebSocketVerticle(), new DeploymentOptions().setConfig(config()), re -> {
            if (re.succeeded()) {
                vertx.deployVerticle(new SocketVerticle(), new DeploymentOptions().setConfig(config()));
            }
        });
    }
}
