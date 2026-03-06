package utils;

import base.BaseTest;
import io.qameta.allure.Attachment;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class ScreenshotListener implements AfterTestExecutionCallback {
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        boolean testFailed = context.getExecutionException().isPresent();
        if (testFailed) {
            Object testInstance = context.getRequiredTestInstance();
            if (testInstance instanceof BaseTest) {
                WebDriver driver = ((BaseTest) testInstance).getDriver();
                if (driver != null) {
                    try {
                        captureScreenshotOnFailure(driver);
                    } catch (Exception e) {
                        System.err.println("Failed to capture screenshot: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Attachment(value = "Screenshot on Failure", type = "image/png")
    public byte[] captureScreenshotOnFailure(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }
}
