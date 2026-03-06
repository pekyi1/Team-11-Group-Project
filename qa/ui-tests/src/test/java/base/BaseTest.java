package base;

import driver.DriverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.junit.jupiter.api.extension.ExtendWith;
import utils.ScreenshotListener;

@ExtendWith(ScreenshotListener.class)
public abstract class BaseTest {
    protected WebDriver driver;
    private String url;

    // used by the Screenshot Listener
    public WebDriver getDriver() {
        return driver;
    }

    protected String getUrl() {
        url = System.getProperty("baseUrl",
                System.getenv().getOrDefault("APP_BASE_URL",
                        "https://example.com/")); // Placeholder for the target base URL
        Assertions.assertNotNull(url);
        return url;
    }

    @BeforeEach
    void setUp() {
        driver = DriverFactory.createDriver();
        driver.get(getUrl());
        // Page Objects should be initialized here in the inheriting/future
        // implementation
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
