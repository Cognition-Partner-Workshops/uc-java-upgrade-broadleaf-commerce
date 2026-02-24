package com.broadleafcommerce.search;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic smoke test to verify the Spring Boot application context loads successfully.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "solr.mode=standalone",
        "solr.url=http://localhost:8983/solr",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SearchServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context can be loaded
    }
}
