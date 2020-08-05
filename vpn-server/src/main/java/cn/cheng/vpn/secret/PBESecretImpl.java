package cn.cheng.vpn.secret;


import io.vertx.core.buffer.Buffer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.Key;
import java.util.Random;

/**
 * @author Cheng Mingwei
 * @create 2020-08-04 17:38
 **/
public class PBESecretImpl implements SecretInterface {

    @Override
    public Buffer encode(Buffer buffer, String password) {
        try {
            if (password.length() > 8) {
                password = password.substring(0, 8);
            }
            byte[] bytes = encrypt(buffer.getBytes(), password, password.getBytes());
            return Buffer.buffer(bytes);
        } catch (Exception e) {
            throw new RuntimeException("加密时出现异常");
        }
    }

    @Override
    public Buffer decode(Buffer buffer, String password) {
        try {
            if (password.length() > 8) {
                password = password.substring(0, 8);
            }
            byte[] bytes = decrypt(buffer.getBytes(), password, password.getBytes());
            return Buffer.buffer(bytes);
        } catch (Exception e) {
            throw new RuntimeException("解密时出现异常");
        }
    }


    /**
     * 支持以下任意一种算法
     *
     * <pre>
     * PBEWithMD5AndDES
     * PBEWithMD5AndTripleDES
     * PBEWithSHA1AndDESede
     * PBEWithSHA1AndRC2_40
     * </pre>
     */
    public static final String ALGORITHM = "PBEWITHMD5andDES";

    /**
     * 盐初始化
     *
     * @return
     * @throws Exception
     */
    public static byte[] initSalt() throws Exception {
        byte[] salt = new byte[8];
        Random random = new Random();
        random.nextBytes(salt);
        return salt;
    }

    /**
     * 转换密钥<br>
     *
     * @param password
     * @return
     * @throws Exception
     */
    private static Key toKey(String password) throws Exception {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
        SecretKey secretKey = keyFactory.generateSecret(keySpec);

        return secretKey;
    }

    /**
     * 加密
     *
     * @param data     数据
     * @param password 密码
     * @param salt     盐
     * @return
     * @throws Exception
     */
    public static byte[] encrypt(byte[] data, String password, byte[] salt)
            throws Exception {

        Key key = toKey(password);

        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 100);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

        return cipher.doFinal(data);

    }

    /**
     * 解密
     *
     * @param data     数据
     * @param password 密码
     * @param salt     盐
     * @return
     * @throws Exception
     */
    public static byte[] decrypt(byte[] data, String password, byte[] salt)
            throws Exception {

        Key key = toKey(password);

        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 100);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

        return cipher.doFinal(data);

    }
}
