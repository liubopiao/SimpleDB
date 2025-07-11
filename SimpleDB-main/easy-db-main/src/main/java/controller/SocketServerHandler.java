/*
 *@Type SocketServerHandler.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 12:50
 * @version
 */
package controller;

import dto.ActionDTO;
import dto.ActionTypeEnum;
import dto.RespDTO;
import dto.RespStatusTypeEnum;
import service.NormalStore;
import service.Store;
import utils.LoggerUtil;

import java.io.*;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SocketServerHandler implements Runnable {
    private final Logger LOGGER = LoggerFactory.getLogger(SocketServerHandler.class);
    private Socket socket;
    private Store store;

    public SocketServerHandler(Socket socket, NormalStore store) {
        this.socket = socket;
        this.store = store;
    }

    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            while (!socket.isClosed()){
                try{
                    // 接收序列化对象
                    ActionDTO dto = (ActionDTO) ois.readObject();
                    LoggerUtil.info("SocketServerHandler", "收到命令: %s", dto.toString());

                    System.out.println("" + dto.toString());

                    // 处理命令逻辑(TODO://改成可动态适配的模式)
                    if (dto.getType() == ActionTypeEnum.GET) {
                        String value = this.store.get(dto.getKey());
                        LoggerUtil.info("SocketServerHandler", "收到命令: %s", dto.toString());
                        RespDTO resp = new RespDTO(RespStatusTypeEnum.SUCCESS, value);
                        oos.writeObject(resp);
                        oos.flush();
                    }
                    if (dto.getType() == ActionTypeEnum.SET) {
                        this.store.set(dto.getKey(), dto.getValue());
                        LoggerUtil.info("SocketServerHandler", "收到命令: %s", dto.toString());
                        RespDTO resp = new RespDTO(RespStatusTypeEnum.SUCCESS, null);
                        oos.writeObject(resp);
                        oos.flush();
                    }
                    if(dto.getType() == ActionTypeEnum.SETEX){
                        this.store.setex(dto.getKey(), dto.getValue(), dto.getSeconds());
                        LoggerUtil.info("SocketServerHandler", "收到命令: %s", dto.toString());
                        RespDTO resp = new RespDTO(RespStatusTypeEnum.SUCCESS, null);
                        oos.writeObject(resp);
                        oos.flush();
                    }
                    if (dto.getType() == ActionTypeEnum.RM) {
                        this.store.rm(dto.getKey());
                        RespDTO resp = new RespDTO(RespStatusTypeEnum.SUCCESS, null);
                        oos.writeObject(resp);
                        oos.flush();
                    }
                    if(dto.getType() == ActionTypeEnum.SHUTDOWN){
                        this.store.close();
                        RespDTO resp = new RespDTO(RespStatusTypeEnum.SUCCESS, null);
                        oos.writeObject(resp);
                        oos.flush();
                    }
                }catch (IOException | ClassNotFoundException e) {
                    // 客户端断开连接，正常退出
                    System.out.println("客户端断开连接: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
