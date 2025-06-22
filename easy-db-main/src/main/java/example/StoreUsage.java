/*
 *@Type Usage.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 03:59
 * @version
 */
package example;

import service.NormalStore;

import java.io.File;
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

            //关于建表
            //设置主键
            //设置统一前缀
            /**
             * {"type":"SET","key":"student:id:1001", "value":"student:1"} // 索引
             * {"type":"SET","key":"student:1:id", "value":"1001"}
             * {"type":"SET","key":"student:1:name", "value":"张三"}
             */
            //where操作

            //关于内存溢出
            //通过优化reloadIndex实现

            //data表不会清理旧数据
            //关于缓冲池
        }
    }
}
