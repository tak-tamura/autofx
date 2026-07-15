package com.takuro_tamura.autofx.infrastructure.datasource;

import com.takuro_tamura.autofx.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesSchemaAndSeedMigrationsToAnEmptyDatabase() {
        final var successfulMigrations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
            Integer.class
        );
        final var configParameters = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM config_parameter",
            Integer.class
        );
        final var orderTable = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'order'",
            Integer.class
        );

        assertEquals(3, successfulMigrations);
        assertEquals(27, configParameters);
        assertEquals(1, orderTable);
    }
}
