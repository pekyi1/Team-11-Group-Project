package utils;

import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.TestResult;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AllureLogListener implements TestLifecycleListener {
    private static final Logger logger = Logger.getLogger("AllureTestLogger");
    static {
        try {
            FileHandler fileHandler = new FileHandler("test-execution.log", true);
            // Custom Formatter Override
            fileHandler.setFormatter(new Formatter() {
                private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                @Override
                public String format(LogRecord record) {
                    return String.format("[%s] [%s] %s%n",
                            dateFormat.format(new Date(record.getMillis())),
                            record.getLevel(),
                            record.getMessage());
                }
            });
            // Stop duplicating output in the parent console logger
            logger.setUseParentHandlers(false);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Failed to initialize file logger for Allure: " + e.getMessage());
        }
    }

    @Override
    public void beforeTestSchedule(TestResult result) {
        logger.info("---------------------------------------------------");
        logger.info("TEST SCHEDULED: " + result.getName());
    }

    @Override
    public void beforeTestStart(TestResult result) {
        logger.info("TEST STARTED: " + result.getName());
    }

    @Override
    public void afterTestStop(TestResult result) {
        String status = result.getStatus() != null ? result.getStatus().name() : "UNKNOWN";
        logger.info("TEST FINISHED: " + result.getName() + " -> Status: " + status);
        logger.info("---------------------------------------------------\n");
    }
}
