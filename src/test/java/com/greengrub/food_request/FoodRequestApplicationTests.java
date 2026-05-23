package com.greengrub.food_request;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test. Disabled by default — full context load requires a Postgres
 * docker instance and a running image-service for gRPC client init. Re-enable
 * once a test slice profile is added if integration coverage is needed here.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "grpc.server.port=0",
        "grpc.client.imageService.address=static://localhost:0"
})
@Disabled("Requires running Postgres + image-service; covered by integration tests instead.")
class FoodRequestApplicationTests {

    @Test
    void contextLoads() {
    }
}
