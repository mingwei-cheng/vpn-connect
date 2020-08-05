package cn.cheng.vpn.secret;

/**
 * @author Cheng Mingwei
 * @create 2020-08-04 17:56
 **/
public class SecretFactory {


    /**
     * 静态工厂
     *
     * @return 当前加密方式
     */
    public static SecretInterface getSecretImpl(String name) {
        if (name == null || "".equals(name)) {
            name = "DES";
        }
        switch (name) {
            case "PBE":
                return new PBESecretImpl();
            case "DES":
            default:
                return new DESSecretImpl();
        }
    }
}
