package cn.cheng.vpn.secret;

import io.vertx.core.json.JsonObject;

/**
 * @author Cheng Mingwei
 * @create 2020-08-04 17:56
 **/
public class SecretFactory {

    private static SecretInterface secretInterface;

    /**
     * 静态工厂,懒汉式
     *
     * @return 当前加密方式
     */
    public static synchronized SecretInterface getSecretImpl(JsonObject config) {
        if (secretInterface != null) {
            return secretInterface;
        }
        String name = config.getString("secret");
        if (name == null || "".equals(name)) {
            name = "DES";
        }
        switch (name) {
            case "PBE":
                System.out.println("-------- 使用PBE加密 --------");
                secretInterface = new PBESecretImpl();
                return secretInterface;
            case "DES":
            default:
                System.out.println("-------- 使用DES加密 --------");
                secretInterface = new DESSecretImpl();
                return secretInterface;
        }
    }
}
