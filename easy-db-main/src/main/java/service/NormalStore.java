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
import model.command.Command;
import model.command.CommandPos;
import model.command.RmCommand;
import model.command.SetCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CommandUtil;
import utils.LoggerUtil;
import utils.RandomAccessFileUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;

public class NormalStore implements Store,AutoCloseable {

    public static final String TABLE = ".table";
    public static final String RW_MODE = "rw";
    public static final String NAME = "data";
    private final Logger LOGGER = LoggerFactory.getLogger(NormalStore.class);
    private final String logFormat = "[NormalStore][{}]: {}";


    /**
     * 内存表，类似缓存
     */
    private TreeMap<String, Command> memTable;

    //private static final int MEMTABLE_FLUSH_THRESHOLD = 1024; // 1KB
    // 或按记录数
     private static final int MEMTABLE_FLUSH_THRESHOLD_COUNT = 30;

    /**
     * hash索引，存的是数据长度和偏移量
     * */
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
        this.index = new HashMap<>();

        File file = new File(dataDir);
        if (!file.exists()) {
            LoggerUtil.info(LOGGER,logFormat, "NormalStore","dataDir isn't exist,creating...");
            file.mkdirs();
        }
        this.reloadIndex();
    }

    public String genFilePath() {
        //File.separator貌似重复了
        return this.dataDir + File.separator + NAME + TABLE;
    }


    public void reloadIndex() {
        try {
            RandomAccessFile file = new RandomAccessFile(this.genFilePath(), RW_MODE);
            System.out.println("当前路径:"+this.genFilePath());
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
                if(cmdLen>99999){
                    System.out.println("读取到异常数据");
                    System.out.println("当前长度:"+cmdLen);
                }
                file.read(bytes);
                JSONObject value = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
                Command command = CommandUtil.jsonToCommand(value);
                start += 4;
                if (command != null) {
                    CommandPos cmdPos = new CommandPos((int) start, cmdLen);
                    index.put(command.getKey(), cmdPos);
                }
                start += cmdLen;
            }
            file.seek(file.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LoggerUtil.debug(LOGGER, logFormat, "reload index: "+index.toString());
    }

    @Override
    public void set(String key, String value) {
        try {
            SetCommand command = new SetCommand(key, value);
            // 创建 SetCommand对象 并序列化为 JSON 字节数组；
            byte[] commandBytes = JSONObject.toJSONBytes(command);
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

            //批量写入
            memTable.put(key, command);
            System.out.println("当前长度："+memTable.size());
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
    public String get(String key) {
        try {
            indexLock.readLock().lock();
            // 从索引中获取信息
            CommandPos cmdPos = index.get(key);
            if (cmdPos == null) {
                return null;
            }
            byte[] commandBytes = RandomAccessFileUtil.readByIndex(this.genFilePath(), cmdPos.getPos(), cmdPos.getLen());

            //将 JSON 格式的字符串解析为 JSONObject 对象
            JSONObject value = JSONObject.parseObject(new String(commandBytes));
            System.out.println("JSON序列为:"+value);
            Command cmd = CommandUtil.jsonToCommand(value);
            if (cmd instanceof SetCommand) {
                return ((SetCommand) cmd).getValue();
            }
            if (cmd instanceof RmCommand) {
                return null;
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

}
