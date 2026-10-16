package com.oskarro.oskarito;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.oskarro.oskarito.config.DataSeeder;

@SpringBootTest
@ActiveProfiles("test")
class OskaritoApplicationTests {

    @MockitoBean
    DataSeeder dataSeeder;

    @Test
    void contextLoads() {
    }

}
