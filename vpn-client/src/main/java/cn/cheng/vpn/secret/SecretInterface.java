package cn.cheng.vpn.secret;


import io.vertx.core.buffer.Buffer;

/**
 * @author Cheng Mingwei
 * @create 2020-08-04 17:37
 **/
public interface SecretInterface {
    /**
     * 解码
     *
     * @param buffer
     * @return
     */
    Buffer encode(Buffer buffer, String password);

    /**
     * 编码
     *
     * @param buffer
     * @return
     */
    Buffer decode(Buffer buffer, String password);
}
