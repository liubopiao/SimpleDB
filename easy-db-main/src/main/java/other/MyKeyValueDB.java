package other;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyKeyValueDB {
    // 内存存储
    private final Map<String, Entry> store = new HashMap<>();

    // 写入日志或持久化（可选）
    private final String persistenceFile = "data.table";

    static class Entry {
        String value;
        Long expireAt; // 过期时间（毫秒）

        public Entry(String value, Long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    // 判断是否过期
    private boolean isExpired(Entry entry) {
        return entry.expireAt != null && entry.expireAt < System.currentTimeMillis();
    }

    // 设置键值
    public void set(String key, String value) {
        store.put(key, new Entry(value, null));
        persist(key);
    }

    // 设置带过期时间的键值
    public void setex(String key, String value, long seconds) {
        long expireAt = System.currentTimeMillis() + seconds * 1000;
        store.put(key, new Entry(value, expireAt));
        persist(key);
    }

    // 获取值
    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null || isExpired(entry)) {
            return null;
        }
        return entry.value;
    }

    // 删除键
    public void del(String key) {
        store.remove(key);
        // 可以写入删除日志

    }

    // 持久化到文件（简单追加）
    private void persist(String key) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(persistenceFile, true))) {
            Entry entry = store.get(key);
            writer.write(String.format("{\"key\":\"%s\",\"type\":\"SET\",\"value\":\"%s\"}\n", key, entry.value));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 定期清理过期键（后台线程）
    public void startCleanupTask() {
        new Thread(() -> {
            while (true) {
                List<String> expiredKeys = new ArrayList<>();
                for (Map.Entry<String, Entry> entry : store.entrySet()) {
                    if (isExpired(entry.getValue())) {
                        expiredKeys.add(entry.getKey());
                    }
                }
                for (String key : expiredKeys) {
                    store.remove(key);
                }
                try {
                    Thread.sleep(1000); // 每秒检查一次
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
}
