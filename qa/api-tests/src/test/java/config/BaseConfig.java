package config;

import io.restassured.RestAssured;
import java.util.Properties;

/**
 * Centralized configuration for REST Assured.
 * Reads runtime properties to configure REST Assured tests.
 */
public class BaseConfig {
    private static final Properties properties = new Properties();

    public static String getBaseUrl() {
        return System.getProperty("baseUrl",
                properties.getProperty("base.url", "http://localhost:8080")); // fallback generic URL
    }

    /**
     * Initialises RestAssured globally. Called from BaseTest.setup().
     */
    public static void init() {
        RestAssured.baseURI = getBaseUrl();
    }
}
