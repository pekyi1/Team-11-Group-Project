package config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import utils.TestLoggingListener;

/**
 * Base test class that all API test classes should extend.
 * Handles common setup such as initialising the REST Assured configuration
 * and tracking test statuses through logic listeners.
 */
@ExtendWith(TestLoggingListener.class)
public abstract class BaseTest {
    @BeforeAll
    public static void setup() {
        BaseConfig.init();
    }
}
