/*
 *@Type SocketClientUsage.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 14:07
 * @version
 */
package client;

public class SocketClientUsage {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        SocketClientController client = new SocketClientController(host, port);
//        client.get("zsy200");

//        client.set("zsy350","for zsy350");
//        client.close();

        client.get("zsy350");

//        client.set("zsy12","for test");
//        client.set("zsy15","for test15");

//        client.setex("seven","777",100L);
//        client.setex("eight","88888",1000L);

//        client.get("zsy12");
//        client.rm("zsy12");
//        client.get("zsy12");
//        client.close();
    }
}