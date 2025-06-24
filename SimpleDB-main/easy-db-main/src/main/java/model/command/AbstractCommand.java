/*
 *@Type AbstractCommand.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 01:51
 * @version
 */
package model.command;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class AbstractCommand implements Command {
    /*
    * 命令类型
    * */
    private CommandTypeEnum type;

    public AbstractCommand(CommandTypeEnum type) {
        this.type = type;
    }

    @Override
    public String toString() {
        /**
         *使用了阿里巴巴的开源 JSON 库：FastJSON
         * 将整个对象转换为标准的 JSON 格式字符串，例如：
         *   {
         *     "type": "SET",
         *     "key": "k1",
         *     "value": "v1"
         *   }
         */
        return JSON.toJSONString(this);
    }
}
