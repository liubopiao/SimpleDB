/*
 *@Type Usage.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 03:59
 * @version
 */
package myTest;

import service.NormalStore;

import java.io.IOException;
//C:\Users\30475\Downloads\easy-db-main\easy-db-main\data\data.table
public class StoreUsage {
    public static void main(String[] args) throws IOException {
        String dataDir="data";
        try (NormalStore store = new NormalStore(dataDir)) {
//            store.set("zsy1","1");
//            store.set("zsy2","2");
//            store.set("zsy3","3");
//            store.set("zsy4","4");
//            System.out.println(store.get("zsy4"));

            //关于遍历data.table,打印所有条目
//            store.scan();

            //rm
//            store.rm("zsy4");
//            System.out.println(store.get("zsy4"));

            store.scan();

            //关于缓冲池

            //日志实现 ok

            //除了SET,SETEX,RM 之外的其他操作

            //批量操作 ok

            //交互式 ok

            //数据冗余 ok

            //k-v数据库的用途 ok

            //底层数据库 MyKeyValueDB 没有任何同步机制
        }
    }
}
