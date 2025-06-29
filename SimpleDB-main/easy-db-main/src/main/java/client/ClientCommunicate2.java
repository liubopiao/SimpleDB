package client;

import java.util.Scanner;

public class ClientCommunicate2 {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        try (SocketClientController client = new SocketClientController(host, port)) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("欢迎使用 easy-db 客户端。输入 help 查看帮助。");

            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;

                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] tokens = line.split("\\s+");
                String command = tokens[0].toLowerCase();

                switch (command) {
                    case "set":
                        if (tokens.length != 3) {
                            System.out.println("用法: set <key> <value>");
                            break;
                        }
                        client.set(tokens[1], tokens[2]);
                        System.out.println("OK");
                        break;

                    case "get":
                        if (tokens.length != 2) {
                            System.out.println("用法: get <key>");
                            break;
                        }
                        String value = client.get(tokens[1]);
                        System.out.println(value == null ? "(nil)" : value);
                        break;

                    case "rm":
                        if (tokens.length != 2) {
                            System.out.println("用法: rm <key>");
                            break;
                        }
                        client.rm(tokens[1]);
                        System.out.println("OK");
                        break;

                    case "exit":
                    case "quit":
                        System.out.println("正在关闭连接...");
                        client.close();
                        return;

                    case "help":
                        printHelp();
                        break;

                    default:
                        System.out.println("未知命令: " + command);
                        printHelp();
                        break;
                }
            }

        } catch (Exception e) {
            System.err.println("连接异常中断: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("可用命令:");
        System.out.println("  set <key> <value>   设置键值对");
        System.out.println("  get <key>           获取键的值");
        System.out.println("  rm <key>            删除键");
        System.out.println("  exit/quit           退出客户端");
        System.out.println("  help                显示帮助信息");
    }
}
