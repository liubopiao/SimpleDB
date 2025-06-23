/*
 *@Type ServerController.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 12:20
 * @version
 */
package controller;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.NormalStore;
import service.Store;
import utils.LoggerUtil;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Setter
@Getter
public class SocketServerController implements Controller {

    private final Logger LOGGER = LoggerFactory.getLogger(SocketServerController.class);

    private final String logFormat = "[SocketServerController][{}]: {}";
    private String host;
    private int port;
    private NormalStore store;

    public SocketServerController(String host, int port, NormalStore store) {
        this.host = host;
        this.port = port;
        this.store = store;
    }

    @Override
    public void set(String key, String value) {

    }

    @Override
    public String get(String key) {
        return null;
    }

    @Override
    public void rm(String key) {

    }

    @Override
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LoggerUtil.info("SocketServerController", logFormat,"startServer","Server started, waiting for connections...");

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    LoggerUtil.info("SocketServerController", logFormat,"startServer","New client connected");
                    // 为每个客户端连接创建一个新的线程
                    new Thread(new SocketServerHandler(socket, store)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
