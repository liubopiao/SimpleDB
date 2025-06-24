/*
 *@Type Client.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 13:15
 * @version
 */
package client;

import model.command.Command;

public interface Client {
    void set(String key, String value);

    String get(String key);

    void rm(String key);

    void setex(String key, String value, long seconds);

}
