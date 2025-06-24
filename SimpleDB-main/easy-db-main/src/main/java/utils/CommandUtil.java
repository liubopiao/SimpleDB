/*
 *@Type ConvertUtil.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 02:09
 * @version
 */
package utils;

import com.alibaba.fastjson.JSONObject;
import model.command.*;

public class CommandUtil {
    public static final String TYPE = "type";

    public static Command jsonToCommand(JSONObject value){
        if (value.getString(TYPE).equals(CommandTypeEnum.SET.name())) {
            return value.toJavaObject(SetCommand.class);
        } else if (value.getString(TYPE).equals(CommandTypeEnum.RM.name())) {
            return value.toJavaObject(RmCommand.class);
        }else if(value.getString(TYPE).equals(CommandTypeEnum.SETEX.name())){
            return value.toJavaObject(SetexCommand.class);
        }
        return null;
    }
}
