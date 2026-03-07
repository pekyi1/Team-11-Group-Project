package utils;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class TestLoggingListener implements TestWatcher, BeforeAllCallback, AfterAllCallback,
        BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final String LOG_FILE_PATH = "target/local-test-run.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private void writeLog(String message) {
        try {
            Path path = Paths.get(LOG_FILE_PATH);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            String logEntry = String.format("[%s] %s%n", LocalDateTime.now().format(formatter), message);
            Files.writeString(path, logEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        writeLog("=== Starting Test Suite: " + context.getRequiredTestClass().getSimpleName() + " ===");
    }

    @Override
    public void afterAll(ExtensionContext context) {
        writeLog("=== Finished Test Suite: " + context.getRequiredTestClass().getSimpleName() + " ===");
        writeLog("--------------------------------------------------");
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        writeLog("  -> Starting Test: " + context.getRequiredTestMethod().getName());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        writeLog("     Result: PASSED");
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String causeMsg = cause != null ? cause.getMessage() : "Unknown";
        writeLog("     Result: FAILED - " + causeMsg);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        writeLog("     Result: ABORTED");
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        writeLog("     Result: DISABLED - " + reason.orElse("No reason provided"));
    }
}
