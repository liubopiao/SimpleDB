/*
 *@Type NormalStore.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 02:07
 * @version
 */
package service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import controller.SocketServerHandler;
import model.command.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CommandUtil;
import utils.LoggerUtil;
import utils.RandomAccessFileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class NormalStore implements Store, AutoCloseable {

    public static final String TABLE = ".table";
    public static final String RW_MODE = "rw";
    public static final String R_MODE = "r";
    public static final String NAME = "data";
    private final Logger LOGGER = LoggerFactory.getLogger(NormalStore.class);
    private final String logFormat = "[NormalStore][{}]: {}";


    /**
     * 内存表memTable，存放将要存进磁盘的记录
     * 缓存tempMemTable，存放磁盘的数据，用于删除冗余记录和查询优化
     */
    private TreeMap<String, Command> memTable;
    private TreeMap<String, Command> tempMemTable;


    //private static final int MEMTABLE_FLUSH_THRESHOLD = 1024; // 1KB
    // 或按记录数
    private static final int MEMTABLE_FLUSH_THRESHOLD_COUNT = 30;

    // 在 NormalStore 类中添加配置项
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * hash索引，存的是数据长度和偏移量
     */
    private HashMap<String, CommandPos> index;

    /**
     * 数据目录
     */
    private final String dataDir;

    /**
     * 读写锁，支持多线程，并发安全写入
     */
    private final ReadWriteLock indexLock;

    /**
     * 暂存数据的日志句柄
     */
    private RandomAccessFile writerReader;

    /**
     * 持久化阈值
     */
//    private final int storeThreshold;
    public NormalStore(String dataDir) {
        this.dataDir = dataDir;
        this.indexLock = new ReentrantReadWriteLock();
        this.memTable = new TreeMap<String, Command>();
        this.tempMemTable = new TreeMap<String, Command>();
        this.index = new HashMap<>();

        File file = new File(dataDir);
        if (!file.exists()) {
            LoggerUtil.info("NormalStore", logFormat, "NormalStore", "dataDir isn't exist,creating...");
            file.mkdirs();
        }
        this.reload();
        this.replayWal(); // 从 .wal 回放
    }

    public String genFilePath() {
        //File.separator貌似重复了
        return this.dataDir + File.separator + NAME + TABLE;
    }

    public void reload() {
        // 先加载主文件
        loadFile(new File(genFilePath()));

        // 加载所有备份文件
        for (int i = 1; i <= 5; i++) {
            File backup = new File(genFilePath() + "." + i + ".gz");
            if (backup.exists()) {
                try {
                    File decompressed = decompressToFile(backup);
                    loadFile(decompressed);
                    decompressed.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void loadFile(File fileto) {
        try (RandomAccessFile file = new RandomAccessFile(fileto, RW_MODE)) {
            System.out.println("当前路径:" + this.genFilePath());
            long len = file.length();
            long start = 0;
            file.seek(start);
            while (start < len) {
                int cmdLen = file.readInt();
                /**
                 * 这里下一次循环时不会掉上一次的new byte[cmdLen]吗?
                 *
                 * 不会。 Java 的垃圾回收机制（GC）会自动回收不再引用的 byte[] 数组，
                 * 所以即使你在循环中不断新建 new byte[cmdLen]，只要它不再被使用，
                 * 就会被 GC 回收，不会造成内存泄漏。
                 */
                byte[] bytes = new byte[cmdLen];
                if (cmdLen > 99999) {
                    System.out.println("读取到异常数据");
                    System.out.println("当前长度:" + cmdLen);
                }
                file.read(bytes);
                JSONObject value = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
                Command command = CommandUtil.jsonToCommand(value);
                start += 4 + cmdLen;
                if (command != null) {
                    CommandPos cmdPos = new CommandPos((int) start, cmdLen);
                    index.put(command.getKey(), cmdPos);
                    tempMemTable.put(command.getKey(), command);
                }
            }
            file.seek(file.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LoggerUtil.info("NormalStore", logFormat, "reload index: " + index.toString());

    }

    @Override
    public void set(String key, String value) {
        try {
            SetCommand command = new SetCommand(key, value);
            // 创建 SetCommand对象 并序列化为 JSON 字节数组；
            // 加锁
            indexLock.writeLock().lock();
            // TODO://先写内存表，内存表达到一定阀值再写进磁盘
            // 写table（wal）文件
            //Write-Ahead Logging--预写日志
            //先写入数据长度，再写入数据
            //这里直接写入磁盘了
            //RandomAccessFileUtil.writeInt(this.genFilePath(), commandBytes.length);
            //int pos = RandomAccessFileUtil.write(this.genFilePath(), commandBytes);
            // 保存到memTable
            //这里是自己实现的代码，先写入内存，如果内存表满了，再写入磁盘

            writeWalLog(command); // 先写 WAL
            //批量写入
            memTable.put(key, command);
            System.out.println("当前长度：" + memTable.size());
            // 判断是否需要刷盘
            if (memTable.size() >= MEMTABLE_FLUSH_THRESHOLD_COUNT) {
                this.flushMemTable();
            }
            // 添加索引
            //CommandPos cmdPos = new CommandPos(pos, commandBytes.length);
            //index.put(key, cmdPos);
            // TODO://判断是否需要将内存表中的值写回table
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    @Override
    public void setex(String key, String value, long seconds) {
        try {
            SetexCommand command = new SetexCommand(key, value, seconds);
            byte[] commandBytes = JSONObject.toJSONBytes(command);
            indexLock.writeLock().lock();

            writeWalLog(command); // 先写 WAL
            memTable.put(key, command);
            System.out.println("当前长度：" + memTable.size());
            if (memTable.size() >= MEMTABLE_FLUSH_THRESHOLD_COUNT) {
                this.flushMemTable();
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            indexLock.writeLock().unlock();
        }

    }

    @Override
    public String get(String key) {
        try {
            indexLock.readLock().lock();

            // 1. 先查 memTable（尚未持久化的写入）
            Command cmd = memTable.get(key);
            if (cmd != null) {
                System.out.println("从尚未持久化的写入记录查找");
                if (cmd instanceof SetCommand) {
                    return ((SetCommand) cmd).getValue();
                } else if (cmd instanceof RmCommand) {
                    return null; // 被删除
                } else if (cmd instanceof SetexCommand && !isExpired(cmd)) {
                    return ((SetexCommand) cmd).getValue();
                }
            }

            // 2. 再查 tempMemTable（已持久化的有效数据）
            cmd = tempMemTable.get(key);
            if (cmd != null) {
                System.out.println("从缓存（已持久化的有效数据）查找");
                if (cmd instanceof SetCommand) {
                    return ((SetCommand) cmd).getValue();
                } else if (cmd instanceof RmCommand) {
                    return null;
                } else if (cmd instanceof SetexCommand && !isExpired(cmd)) {
                    return ((SetexCommand) cmd).getValue();
                }
            }
            System.out.println("从磁盘中查找");
            // 从索引中获取信息
            CommandPos cmdPos = index.get(key);
            if (cmdPos == null) {
                return null;
            }
            //提供数据缓存，查询时先从缓存中查找，缓存没有再从磁盘中查找数据。

            //readByIndex是从磁盘中查找
            byte[] commandBytes = RandomAccessFileUtil.readByIndex(this.genFilePath(), cmdPos.getPos(), cmdPos.getLen());

            //将 JSON 格式的字符串解析为 JSONObject 对象
            JSONObject value = JSONObject.parseObject(new String(commandBytes));
            System.out.println("JSON序列为:" + value);
            cmd = CommandUtil.jsonToCommand(value);
            if (cmd instanceof SetCommand) {
                return ((SetCommand) cmd).getValue();
            }
            if (cmd instanceof RmCommand) {
                return null;
            }
            if(cmd instanceof SetexCommand && this.isExpired(cmd)){
                return ((SetexCommand) cmd).getValue();
            }

        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            indexLock.readLock().unlock();
        }
        return null;
    }

    @Override
    public void rm(String key) {
        try {
            RmCommand command = new RmCommand(key);
            byte[] commandBytes = JSONObject.toJSONBytes(command);
            // 加锁
            indexLock.writeLock().lock();
            // TODO://先写内存表，内存表达到一定阀值再写进磁盘

            // 写table（wal）文件
//            RandomAccessFileUtil.writeInt(this.genFilePath(), commandBytes.length);
//            int pos = RandomAccessFileUtil.write(this.genFilePath(), commandBytes);

            writeWalLog(command); // 先写 WAL
            // 保存到memTable
            memTable.put(key, command);
            // 判断是否需要刷盘
            if (memTable.size() >= MEMTABLE_FLUSH_THRESHOLD_COUNT) {
                this.flushMemTable();
            }
            // 添加索引
//            CommandPos cmdPos = new CommandPos(pos, commandBytes.length);
//            index.put(key, cmdPos);

            // TODO://判断是否需要将内存表中的值写回table

        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /**
     * 遍历 .table 文件，打印所有条目
     */
    @Override
    public void scan() {
        String filePath = genFilePath();
        try (RandomAccessFile file = new RandomAccessFile(filePath, RW_MODE)) {
            long len = file.length();
            long start = 0;
            file.seek(start);

            while (start < len) {
                int cmdLen = file.readInt(); // 读取长度前缀
                byte[] bytes = new byte[cmdLen];
                file.read(bytes); // 读取命令内容
                start += 4 + cmdLen;//4是偏移量

                JSONObject value = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
                Command command = CommandUtil.jsonToCommand(value);

                if (command != null) {
                    if (command instanceof SetCommand) {
                        SetCommand setCmd = (SetCommand) command;
                        System.out.println("Key: " + setCmd.getKey() + ", Value: " + setCmd.getValue());
                    } else if (command instanceof RmCommand) {
                        RmCommand rmCmd = (RmCommand) command;
                        System.out.println("Key removed: " + rmCmd.getKey());
                    } else if (command instanceof SetexCommand) {
                        SetexCommand setexCommand = (SetexCommand) command;
                        System.out.println("Key: " + setexCommand.getKey() + ", Value: " + setexCommand.getValue() + ", seconds:" + setexCommand.getSeconds());
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void close() throws IOException {
        //重写close
        if (!memTable.isEmpty()) {
            System.out.println("内存表未满,自动写入磁盘");
            this.flushMemTable();
        }
    }

    /**
     * 将内存表中的数据批量刷入磁盘文件，并更新索引
     */
    private void flushMemTable() throws IOException {
        if (memTable.isEmpty()) {
            return;
        }

        String filePath = genFilePath();

        File filesp = new File(filePath);
        // 检查当前文件大小
        if (filesp.exists() && filesp.length() > MAX_FILE_SIZE) {
            rotateFile(filesp); // 触发 Rotate
        }
        RandomAccessFile file = new RandomAccessFile(filePath, RW_MODE);
        try {
            // 定位到文件末尾
            file.seek(file.length());

            // 遍历内存表，逐条写入磁盘
            for (Map.Entry<String, Command> entry : memTable.entrySet()) {
                String key = entry.getKey();
                Command command = entry.getValue();

                // 序列化命令为字节数组
                byte[] bytes = JSONObject.toJSONBytes(command);

                // 写入长度前缀（4 字节）
                file.writeInt(bytes.length);

                // 写入实际内容
                file.write(bytes);

                // 更新索引：记录 key 对应的位置和长度
                int pos = (int) (file.getFilePointer() - bytes.length);
                index.put(key, new CommandPos(pos, bytes.length));
            }

            // 清空内存表
            memTable.clear();

        } finally {
            file.close();
        }
    }
    // 将内存中的有效数据重新写入文件，覆盖原有文件

    /**
     * 重写持久化文件，清理冗余数据
     */
    @Override
    public void rewritePersistenceFile() {
        String filePath = genFilePath();
        String tempFilePath = filePath + ".rewrite.tmp";

        try {
            // 1. 清空当前内存表和索引
            tempMemTable.clear();
            index.clear();

            // 2. 从原始文件中加载所有有效数据到内存和索引表
            reload();  //重建 index 和 TempMemTable

            //定期清理过期键
            cleanupExpiredKeys();


            // 3. 创建临时文件用于写入新数据
            RandomAccessFile tempFile = new RandomAccessFile(tempFilePath, RW_MODE);
            tempFile.setLength(0); // 清空文件内容

            // 4. 将缓存中有效的数据写入临时文件
            for (Map.Entry<String, Command> entry : tempMemTable.entrySet()) {
                String key = entry.getKey();
                Command command = entry.getValue();
                byte[] bytes = JSONObject.toJSONBytes(command);

                // 写入长度前缀和命令数据
                tempFile.writeInt(bytes.length);
                tempFile.write(bytes);

                int pos = (int) (tempFile.getFilePointer() - bytes.length);
                index.put(key, new CommandPos(pos, bytes.length));
            }

            // 5. 关闭临时文件
            tempFile.close();

            // 6. 替换原文件
            File oldFile = new File(filePath);
            File newFile = new File(tempFilePath);

            if (oldFile.exists()) {
                oldFile.delete();  // 删除旧文件
            }

            if (newFile.renameTo(oldFile)) {
                System.out.println("持久化文件重写完成，冗余数据已清理");
            } else {
                throw new IOException("文件替换失败: " + tempFilePath + " -> " + filePath);
            }

        } catch (IOException e) {
            System.err.println("持久化文件重写失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void startRewriteTask(long intervalMillis) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(intervalMillis); // 按时间间隔执行
                    rewritePersistenceFile();    // 执行重写
                } catch (InterruptedException e) {
                    System.out.println("持久化重写任务已停止");
                    break;
                }
            }
        }, "Persistence-Rewrite-Thread").start();
    }

    @Override
    public boolean isExpired(Command command) {
        if(command instanceof SetexCommand){
            return ((SetexCommand) command).getSeconds()!= null && ((SetexCommand) command).getSeconds() < System.currentTimeMillis();
        }
        return false;
    }

    @Override
    public void startCleanupTask() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // 每5秒检查一次过期键
                    cleanupExpiredKeys();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 重新设置中断标志
                    System.out.println("清理任务被中断");
                    break;
                }
            }
        }, "Cleanup-Thread").start();
    }
    public void cleanupExpiredKeys() {
        indexLock.writeLock().lock();
        try {
            // 遍历 tempMemTable 并移除过期键
            tempMemTable.entrySet().removeIf(entry -> isExpired(entry.getValue()));

//            // 遍历 index 并移除对应的过期键
//            index.entrySet().removeIf(entry -> {
////                Command command = tempMemTable.get(entry.getKey());
//                String key = entry.getKey();
//                // 从 tempMemTable 中获取命令
//                Command command = tempMemTable.get(key);
//                return command != null && isExpired(command);
//            });

            // 遍历 index 并移除对应的过期键
            index.entrySet().removeIf(entry -> {
                try {
                    String key = entry.getKey();
                    // 从 tempMemTable 中获取命令
                    Command command = tempMemTable.get(key);
                    return command != null && isExpired(command);
                } catch (Exception e) {
                    return false; // 出错时不删除
                }
            });

            System.out.println("已完成过期键清理");
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    private byte[] compress(byte[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
             DeflaterOutputStream dos = new DeflaterOutputStream(bos)) {
            dos.write(data);
            dos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("压缩失败", e);
        }
    }

    private byte[] decompress(byte[] data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             InflaterInputStream iis = new InflaterInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = iis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("解压失败", e);
        }
    }

    // 示例：WAL 写入逻辑
    private void writeWalLog(Command command) throws IOException {
        String walPath = dataDir + File.separator + "db.wal";
        try (RandomAccessFile walFile = new RandomAccessFile(walPath, "rw")) {
            walFile.seek(walFile.length());
            byte[] bytes = JSONObject.toJSONBytes(command);
            walFile.writeInt(bytes.length);
            walFile.write(bytes);
        }
    }

    private void replayWal() {
        String walPath = dataDir + File.separator + "db.wal";
        File file = new File(walPath);
        if (!file.exists())
        {
            System.out.println("wal文件不存在......");
            return;
        }

        try (RandomAccessFile walFile = new RandomAccessFile(walPath, R_MODE)) {
            long len = walFile.length();
            long pos = 0;
            while (pos < len) {
                int cmdLen = walFile.readInt();
                byte[] bytes = new byte[cmdLen];
                walFile.read(bytes);
                JSONObject json = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
                Command command = CommandUtil.jsonToCommand(json);
                if (command != null) {
                    memTable.put(command.getKey(), command); // 重建 memTable
                }
                pos += 4 + cmdLen;
            }
            // 清空 WAL 文件
            try (RandomAccessFile empty = new RandomAccessFile(walPath, RW_MODE)) {
                empty.setLength(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //避免 WAL 文件无限增长，定期合并已提交操作。
    @Override
    public void compactWal() {
        String walPath = dataDir + File.separator + "db.wal";
        String tmpPath = walPath + ".tmp";

        try (RandomAccessFile wal = new RandomAccessFile(walPath, R_MODE);
             RandomAccessFile tmp = new RandomAccessFile(tmpPath, RW_MODE)) {

            TreeMap<String, Command> dedupMap = new TreeMap<>();

            long len = wal.length();
            long pos = 0;
            while (pos < len) {
                int cmdLen = wal.readInt();
                byte[] bytes = new byte[cmdLen];
                wal.read(bytes);
                JSONObject json = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
                Command command = CommandUtil.jsonToCommand(json);
                if (command != null) {
                    dedupMap.put(command.getKey(), command); // 去重
                }
                pos += 4 + cmdLen;
            }

            for (Command cmd : dedupMap.values()) {
                byte[] bytes = JSONObject.toJSONBytes(cmd);
                tmp.writeInt(bytes.length);
                tmp.write(bytes);
            }

            // 替换原文件
            new File(walPath).delete();
            new File(tmpPath).renameTo(new File(walPath));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rotateFile(File currentFile) throws IOException {
        int maxBackupIndex = 5; // 最多保留 5 个备份文件
        String baseName = currentFile.getAbsolutePath();

        // 删除最老的备份文件（如果存在）
        File oldestBackup = new File(baseName + "." + maxBackupIndex + ".gz");
        if (oldestBackup.exists()) {
            oldestBackup.delete();
        }

        // 将旧文件依次后移，并压缩
        for (int i = maxBackupIndex - 1; i >= 1; i--) {
            File src = new File(baseName + "." + i + ".gz");
            if (src.exists()) {
                File dest = new File(baseName + "." + (i + 1) + ".gz");
                if (dest.exists()) {
                    dest.delete();
                }
                src.renameTo(dest);
            }
        }

        // 将当前文件重命名为 .1 并压缩
        File backupFile = new File(baseName + ".1");
        if (currentFile.exists()) {
            if (backupFile.exists()) {
                backupFile.delete();
            }
            currentFile.renameTo(backupFile);
            compressFile(backupFile, new File(baseName + ".1.gz"));
            backupFile.delete(); // 删除原始备份文件
        }

        // 创建新的空文件
        currentFile.createNewFile();
    }

    private void compressFile(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(target))) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
            }
        }
    }

    private File decompressToFile(File gzipFile) throws IOException {
        File tempFile = File.createTempFile("decompress", ".tmp");

        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzipFile));
             FileOutputStream fos = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }

        return tempFile;
    }

}
