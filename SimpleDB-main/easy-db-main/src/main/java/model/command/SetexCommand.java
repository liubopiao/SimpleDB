package model.command;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SetexCommand extends AbstractCommand {
    private String key;
    private String value;
    private long seconds;

    public SetexCommand(String key, String value, long seconds) {
        super(CommandTypeEnum.SETEX);
        this.key = key;
        this.value = value;
        this.seconds = seconds;
    }
}
