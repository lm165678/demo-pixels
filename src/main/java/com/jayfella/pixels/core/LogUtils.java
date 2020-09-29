package com.jayfella.pixels.core;

import org.apache.log4j.*;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Arrays;

public class LogUtils {

    public static void initializeLogger(Level level) {
        initializeLogger(level, false);
    }

    public static void initializeLogger(Level level, boolean removeHandlers) {

        if (removeHandlers) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
        }

        SLF4JBridgeHandler.install();

        ConsoleAppender console = new ConsoleAppender(new PatternLayout("%d{dd MMM yyyy HH:mm:ss} [ %p | %c{1} ] %m%n"));
        console.setThreshold(level);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
    }

    public static void setLevel(String pkg, Level level) {
        LogManager.getLogger(pkg).setLevel(level);
    }

    public static void setLevel(String[] packages, Level level) {
        Arrays.stream(packages).forEach(p -> LogManager.getLogger(p).setLevel(level));
    }

}
