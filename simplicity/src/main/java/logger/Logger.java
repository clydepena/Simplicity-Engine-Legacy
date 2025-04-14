package logger;

import java.io.*;
import java.util.*;

import observers.EventSystem;
import observers.events.*;

public class Logger {
    private static List<Log> logs = new ArrayList<>();

    public static class LogLevel {
        public static final int INFO = 0;
        public static final int WARN = 1;
        public static final int ERROR = 2;
        public static final int FATAL = 3;
    }

    public Logger() {

    }

    public static void info(String message) {
        log(message, 0, null);
    }

    public static void warn(String message) {
        log(message, 1, null);
    }

    public static void error(String message) {
        log(message, 2, null);
    }

    public static void fatal(String message) {
        log(message, 3, null);
    }

    public static void log(String message, Object object) {
        log(message, 0, object);
    }

    public static void log(String message, int logLevel, Object object) {
        Log.LogLevel level;
        switch (logLevel) {
            case 0:
                level = Log.LogLevel.INFO;
                break;
            case 1:
                level = Log.LogLevel.WARN;
                break;
            case 2:
                level = Log.LogLevel.ERROR;

                break;
            case 3:
                level = Log.LogLevel.FATAL;
                break;
            default:
                level = Log.LogLevel.INFO;
                break;
        }
        logs.add(new Log(message, level, object));
        notifyEventLogged(logs.getLast());
    }

    public static final String toStringAllLogs() {
        StringBuilder builder = new StringBuilder();
        builder.append("Java " + System.getProperty("java.version") + "\n");
        for (Log log : logs) {
            builder.append(log.toString() + '\n');
        }
        return builder.toString();
    }

    private static void notifyEventLogged(Log log) {
        EventSystem.notify(log, new Event(EventType.EventLogged));
    }
}
