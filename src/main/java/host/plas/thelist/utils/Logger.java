package host.plas.thelist.utils;

import host.plas.thelist.TheList;

public class Logger {
    public static void logInfo(String message) {
        TheList.getInstance().getLogger().info(message);
    }

    public static void logWarning(String message) {
        TheList.getInstance().getLogger().warning(message);
    }

    public static void logSevere(String message) {
        TheList.getInstance().getLogger().severe(message);
    }

    public static void logInfo(StackTraceElement element) {
        logInfo(element.toString());
    }

    public static void logWarning(StackTraceElement element) {
        logWarning(element.toString());
    }

    public static void logSevere(StackTraceElement element) {
        logSevere(element.toString());
    }

    public static void logInfo(Throwable throwable) {
        logInfo(throwable.getMessage());

        for (StackTraceElement element : throwable.getStackTrace()) {
            logInfo(element);
        }
    }

    public static void logWarning(Throwable throwable) {
        logWarning(throwable.getMessage());

        for (StackTraceElement element : throwable.getStackTrace()) {
            logWarning(element);
        }
    }

    public static void logSevere(Throwable throwable) {
        logSevere(throwable.getMessage());

        for (StackTraceElement element : throwable.getStackTrace()) {
            logSevere(element);
        }
    }

    public static void logInfo(String message, Throwable throwable) {
        logInfo(message);
        logInfo(throwable);
    }

    public static void logWarning(String message, Throwable throwable) {
        logWarning(message);
        logWarning(throwable);
    }

    public static void logSevere(String message, Throwable throwable) {
        logSevere(message);
        logSevere(throwable);
    }
}
