package io.beanvortex.bitkip.exceptions;

import org.controlsfx.control.Notifications;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.beanvortex.bitkip.config.AppConfigs.log;
import static io.beanvortex.bitkip.config.AppConfigs.showErrorNotifications;

public class GlobalExceptionHandler {

    private static final String RELEVANT_PACKAGE = "io.beanvortex";
    private static final String[] EXCLUDE_PACKAGES = {"java.", "javafx.", "com.sun.", "jdk.", "sun."};

    public static void setup() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                logException("Uncaught exception in thread: " + thread.getName(), throwable)
        );
    }

    /**
     * A comprehensive method to log the entire exception chain with filtered stack traces.
     * This can also be used statically elsewhere in your code.
     */
    public static void logException(String message, Throwable throwable) {
        if (throwable == null) {
            log.error(message);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n");

        Throwable rootCause = getRootCause(throwable);
        sb.append("Root Cause: ").append(rootCause.toString()).append("\n");

        Throwable current = throwable;
        int causeLevel = 0;
        while (current != null) {
            if (causeLevel > 0) {
                sb.append("Caused by (").append(causeLevel).append("): ");
            } else {
                sb.append("Exception: ");
            }
            sb.append(current).append("\n");

            String filteredTrace = getFilteredStackTrace(current);
            if (!filteredTrace.isEmpty()) {
                sb.append(filteredTrace).append("\n");
            }

            current = current.getCause();
            causeLevel++;
        }

        if (showErrorNotifications)
            Notifications.create()
                    .title("An error occurred")
                    .text(rootCause + "\nsee Logs")
                    .showError();
        log.error(sb.toString());
    }

    /**
     * Drills down through the exception chain to find the root cause.
     */
    private static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static String getFilteredStackTrace(Throwable throwable) {
        return Arrays.stream(throwable.getStackTrace())
                .filter(GlobalExceptionHandler::isRelevantFrame)
                .map(element -> "\tat " + element)
                .collect(Collectors.joining("\n"));
    }

    private static boolean isRelevantFrame(StackTraceElement frame) {
        String className = frame.getClassName();
        if (className.startsWith(RELEVANT_PACKAGE)) {
            return true;
        }
        for (String excludePkg : EXCLUDE_PACKAGES) {
            if (className.startsWith(excludePkg)) {
                return false;
            }
        }
        return true;
    }
}