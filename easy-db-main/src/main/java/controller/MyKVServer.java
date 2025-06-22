package controller;

import client.MyKeyValueDB;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MyKVServer {
    private final int port;
    private final MyKeyValueDB db = new MyKeyValueDB();

    public MyKVServer(int port) {
        this.port = port;
        db.startCleanupTask(); // 启动清理线程
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("K/V DB Server started on port " + port);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 0) continue;

                switch (parts[0].toUpperCase()) {
                    case "SET":
                        if (parts.length >= 3) {
                            db.set(parts[1], parts[2]);
                            out.write("+OK\r\n");
                        }
                        break;
                    case "SETEX":
                        if (parts.length >= 4) {
                            db.setex(parts[1], parts[2], Long.parseLong(parts[3]));
                            out.write("+OK\r\n");
                        }
                        break;
                    case "GET":
                        if (parts.length >= 2) {
                            String value = db.get(parts[1]);
                            out.write((value != null ? ("$" + value.length() + "\r\n" + value + "\r\n") : "$-1\r\n"));
                        }
                        break;
                    case "DEL":
                        if (parts.length >= 2) {
                            db.del(parts[1]);
                            out.write(":1\r\n");
                        }
                        break;
                    default:
                        out.write("-ERR unknown command\r\n");
                }
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new MyKVServer(12345).start();
    }
}
