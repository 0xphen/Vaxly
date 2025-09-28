package com.vaxly.vaxlyshared;

import com.vaxly.vaxlyshared.config.AwsSqsConfig;
import com.vaxly.vaxlyshared.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {AwsSqsConfig.class, RedisConfig.class})
public class VaxlySharedApplicationTests {

    @Test
    void contextLoads() {
        // just verifies that the Spring context can start
    }
}
