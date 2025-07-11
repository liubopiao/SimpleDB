/*
 *@Type SocketClient.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 13:15
 * @version
 */
package client;

import dto.ActionDTO;
import dto.ActionTypeEnum;
import dto.RespDTO;

import java.io.*;
import java.net.Socket;

public class SocketClientController implements Client, AutoCloseable {
    private String host;
    private int port;
    private static Socket socket;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;

    public SocketClientController(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            socket = new Socket(host, port);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(String key, String value) {
        try {
            // 传输序列化对象
            ActionDTO dto = new ActionDTO(ActionTypeEnum.SET, key, value, 0L);
            oos.writeObject(dto);
            oos.flush();
            // 接收响应数据
            RespDTO resp = (RespDTO) ois.readObject();
            System.out.println("resp data: set:" + resp.toString());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String get(String key) {
        try {
            // 传输序列化对象
            ActionDTO dto = new ActionDTO(ActionTypeEnum.GET, key, null, 0L);
            oos.writeObject(dto);
            oos.flush();
            RespDTO resp = (RespDTO) ois.readObject();
            System.out.println("resp data: get:" + resp.toString());
            // 接收响应数据
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void rm(String key) {
        try {
            ActionDTO dto = new ActionDTO(ActionTypeEnum.RM, key, null, 0L);
            oos.writeObject(dto);
            oos.flush();
            RespDTO resp = (RespDTO) ois.readObject();
            System.out.println("resp data: rm:" + resp.toString());

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setex(String key, String value, long seconds) {
        try {
            long expireAt = System.currentTimeMillis() + seconds * 1000;//时间戳
            ActionDTO dto = new ActionDTO(ActionTypeEnum.SETEX, key, value, expireAt);
            oos.writeObject(dto);
            oos.flush();
            RespDTO resp = (RespDTO) ois.readObject();
            System.out.println("resp data: setex:" + resp.toString());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void close() {
        try {
            ActionDTO dto = new ActionDTO(ActionTypeEnum.SHUTDOWN, null, null, 0L);
            oos.writeObject(dto);
            oos.flush();
            RespDTO resp = (RespDTO) ois.readObject();
            System.out.println("resp data: close:" + resp.toString());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
