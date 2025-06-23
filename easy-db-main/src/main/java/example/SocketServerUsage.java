/*
 *@Type SocketServerUsage.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 14:08
 * @version
 */
package example;

import controller.SocketServerController;
import service.NormalStore;
import service.Store;

import java.io.File;
import java.io.IOException;

/**
 * K-V 数据库虽然没有“表”的概念，但它以极简的方式实现了极致的读写速度，
 * 非常适合缓存、Session、计数器等高频读写场景。你现在实现的这个 K-V
 * 引擎已经具备基本的写入、读取、删除、持久化能力，完全可以作为轻量缓
 * 存服务使用。未来还可以支持更多命令（如 INCR、EXPIRE），甚至做成类 Redis 的服务端。
 */
public class SocketServerUsage {
    public static NormalStore store;
    public static void main(String[] args){
        String host = "localhost";
        int port = 12345;
        String dataDir = "data";
        store = new NormalStore(dataDir);
        SocketServerController controller = new SocketServerController(host, port, store);
        controller.startServer();

    }
}
