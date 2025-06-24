package other;

import client.Client;
import model.command.Command;

import java.io.*;
import java.net.*;

public class MySocketClient implements Client {
    private final String host;
    private final int port;

    public MySocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private void sendCommand(String command, BufferedWriter out) throws IOException {
        out.write(command + "\r\n");
        out.flush();
    }

    private String receiveResponse(BufferedReader in) throws IOException {
        return in.readLine();
    }

    @Override
    public void set(String key, String value) {
        try (Socket socket = new Socket(host, port);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            sendCommand("SET " + key + " " + value, out);
            System.out.println(receiveResponse(in));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setex(String key, String value, long seconds) {
        try (Socket socket = new Socket(host, port);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            sendCommand("SETEX " + key + " " + value + " " + seconds, out);
            System.out.println(receiveResponse(in));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String get(String key) {
        try (Socket socket = new Socket(host, port);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            sendCommand("GET " + key, out);
            return receiveResponse(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void rm(String key) {
        try (Socket socket = new Socket(host, port);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            sendCommand("DEL " + key, out);
            System.out.println(receiveResponse(in));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
