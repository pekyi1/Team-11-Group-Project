package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Map;

public class TestDataUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> getTestData(String fileName) {
        try (InputStream inputStream = TestDataUtils.class.getClassLoader()
                .getResourceAsStream("testdata/" + fileName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: testdata/" + fileName);
            }
            return objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to read test data from " + fileName, e);
        }
    }
}
