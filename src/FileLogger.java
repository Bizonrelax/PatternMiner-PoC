import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger {
    private static PrintWriter logFile;
    private static boolean enabled = false;
    
    public static void start(String filename) {
        try {
            logFile = new PrintWriter(new FileWriter(filename, true));
            enabled = true;
            log("=== ЛОГ НАЧАТ " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " ===");
        } catch (IOException e) {
            System.err.println("Не удалось открыть файл лога: " + e.getMessage());
            enabled = false;
        }
    }
    
    public static void log(String message) {
        System.out.println(message);
        if (enabled && logFile != null) {
            logFile.println(message);
            logFile.flush();
        }
    }
    
    public static void close() {
        if (logFile != null) {
            log("=== ЛОГ ЗАВЕРШЕН " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " ===");
            logFile.close();
            enabled = false;
        }
    }
}