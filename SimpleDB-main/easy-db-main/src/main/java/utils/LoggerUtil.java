package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerUtil {
    private static final String LOG_FILE = "logs/append.log";
    private static PrintWriter writer;

    static {
        try {
            File file = new File(LOG_FILE);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileWriter fileWriter = new FileWriter(file, true);
            writer = new PrintWriter(fileWriter);
        } catch (Exception e) {
            System.err.println("日志初始化失败");
            e.printStackTrace();
        }
    }

    public static void info(String tag, String format, Object... args) {
        String message = String.format("[%s] [%s] %s", now(), tag, String.format(format, args));
        System.out.println(message);
        if (writer != null) {
            writer.println(message);
            writer.flush();
        }
    }

    public static void warn(String tag, String format, Object... args) {
        info(tag, format, args);
    }

    public static void error(String tag, String message, Throwable t) {
        String log = String.format("[%s] [%s] ERROR: %s", now(), tag, message);
        System.err.println(log);
        if (writer != null) {
            writer.println(log);
            t.printStackTrace(writer);
            writer.flush();
        }
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
