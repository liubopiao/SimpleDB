/*
 *@Type CommandDTO.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 12:57
 * @version
 */
package dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ActionDTO implements Serializable {
    private ActionTypeEnum type;
    private String key;
    private String value;
    private long seconds;

    public ActionDTO(ActionTypeEnum type, String key, String value, long seconds) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.seconds = seconds;
    }

    @Override
    public String toString() {
        return "ActionDTO{" +
                "type=" + type +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
