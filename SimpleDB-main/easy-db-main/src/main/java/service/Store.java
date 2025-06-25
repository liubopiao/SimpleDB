/*
 *@Type Store.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 02:05
 * @version
 */
package service;

import model.command.Command;
import model.command.SetexCommand;

import java.io.Closeable;

public interface Store extends Closeable {
    void set(String key, String value);

    void setex(String key, String value, long seconds);

    String get(String key);

    void rm(String key);

    void scan();

    public void rewritePersistenceFile();

    public void startRewriteTask(long intervalMillis);

    boolean isExpired(Command command);

    public void startCleanupTask();

    public void cleanupExpiredKeys();
}
