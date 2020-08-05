# VPN服务

- 本程序分为客户端和服务端，都基于java（vert.x）实现。
- 客户端和服务端是基于WebSocket实现通讯的。
- 服务端不关心客户端是怎么实现代理的，具体代理方式可以由客户端自由实现。
- 但是需要分为两步访问。第一步连接到目标服务器，第二步发送具体数据。
- 本程序的客户端仅仅实现了Socks5的无认证访问。你可以自由编写客户端，通过指定的通讯协议实现WebSocket来连接到服务端，具体通讯协议在后面有写。
- 加密部分写在secret包内，内置实现了DES和PBE两种，你也可以自由添加，只需要实现SecretInterface接口，并在工厂方法SecretFactory中，添加你新增的方式即可。


---
### 启动说明
将服务端运行在被代理的服务器上，客户端运行在需代理访问的主机上，通过指定连接端口即可实现代理访问。
需配置配置文件中的相关信息如下(配置文件都是json格式)：
- 服务端需配置：
	```yaml
	{
	  "ws.port": 18888,
	  "secret": "DES",
	  "password": "helloSecret"
	}
	```
1. ws.port：为服务端对外暴露供客户端连接的端口
2. secret：加密方式（服务端客户端需配置一致）
3. password：密码（服务端客户端需配置一致）

- 客户端需配置：
	```yaml
	{
	  "server.port": 1080, 
	  "ws.port": 16666,
	  "ws.host": "xxxxxx",
	  "secret": "DES",
	  "password": "helloSecret"
	}
	```
1. server.port：为主机提供代理的端口
2. ws.host：为服务端的地址

- **内置加密方式可选的有DES和PBE**
---
### 程序内部协议说明
程序间通信 客户端/服务端 对json包装后加密传输，再在 服务端/客户端 进行解密。
协议如下：
- WS登录
-- 客户端发送

	 name	| 	value	| type | remarks
	----------|------------|---------|--
	cmd|0x00|int
	sn|序列号|int|自增
- WS登录 
-- 服务端返回

	 name	| 	value	| type | remarks
	----------|------------|---------|--
	 cmd|0x00|int
	 sn|序列号|int|发送的序列号
	 result|登录结果|int|200:标识登录成功

---

- 连接目标服务器
-- 客户端发送


	 name	| 	value	| type | remarks
	----------|------------|---------|--
	 cmd|0x01|int
	 sn|序列号|int|自增
	 atyp|访问地址类型|int|0x01（ipv4）0x03（域名）0x04（ipv6）
	 host|访问地址|String
	 port|访问端口|int
	 
- 连接目标服务器
-- 服务端返回

	 name	| 	value	| type | remarks
	----------|------------|---------|--
	 cmd|0x00|int
	 sn|序列号|int|发送的序列号
	 result|登录结果|int|200:标识连接目标服务器成功
	 token|token|String|标识当前连接
	 atyp|访问地址类型|int|发送的atyp
	 
---

- 发送具体数据
-- 客户端发送

	 name	| 	value	| type | remarks
	 ----------|------------|---------|--
	 cmd|0x02|int
	 sn|序列号|int|自增
	 token|token|String|由服务端连接目标服务器后返回的token
	 data|data|bytes|具体数据

- 发送具体数据
-- 服务端返回

	 name	| 	value	| type | remarks
	----------|------------|---------|--
	 cmd|0x00|int
	 sn|序列号|int|发送的序列号
	 data|数据|bytes|访问目标服务器返回的数据
	 
---

