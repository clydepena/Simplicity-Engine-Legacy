package logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log
{
    public enum LogLevel
    {
        INFO,
        WARN,
        ERROR,
        FATAL
    }

    private String message;
    private String time;
    private String date;
    private LogLevel logLevel;
    private Object object;
    private transient String formatted = null;

    public Log(String message)
    {
        setLog(message, LogLevel.INFO, null);
    }

    public Log(String message, LogLevel logLevel)
    {
        setLog(message, logLevel, null);
    }

    public Log(String message, LogLevel logLevel, Object object)
    {
        setLog(message, logLevel, object);
    }

    public Log(String message, Object object)
    {
        setLog(message, LogLevel.INFO, object);
    }

    private void setLog(String message, LogLevel logLevel, Object object) 
    {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter hms = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String tempMsg = new String(message);

        for (int i = (message.length()-1); i > -1; i--)
        {
            if (message.charAt(i) == '\n') 
                tempMsg.substring(i, i);
            else
                break;
        }

        this.message = tempMsg;
        this.date = time.format(ymd);
        this.time = time.format(hms);
        this.logLevel = logLevel;
        this.object = object;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public Object getObject() {
        return object;
    }

    @Override
    public String toString()
    {
        if (formatted == null) 
        {
            StringBuilder builder = new StringBuilder();
            builder.append(date + " " + time + " [" + logLevel + "]\t");
            if (object != null) 
            {
                builder.append("'" + object.getClass().getSimpleName() + "'");
            }
            builder.append(" " + message);
            formatted = builder.toString();
        }
        return formatted;
    }
}
