/*
 *@Type CommandTypeEnum.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 12:56
 * @version
 */
package dto;

public enum ActionTypeEnum {
    /*
     * 增、改
     * */
    SET,
    /*
     * 删
     * */
    RM,
    /*
    * 查
    * */
    GET,
    /**
     * 将内存表数据写入磁盘
     */
    SHUTDOWN,
}