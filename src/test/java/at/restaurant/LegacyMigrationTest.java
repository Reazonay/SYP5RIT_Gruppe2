package at.restaurant;

import static org.junit.jupiter.api.Assertions.*;

import com.zaxxer.hikari.HikariDataSource;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@EnabledIfEnvironmentVariable(named = "RUN_DATABASE_TESTS", matches = "1")
class LegacyMigrationTest {
  @Test
  void adoptsTheExistingNestDatabaseWithoutRecreatingTables() throws Exception {
    String base = System.getenv("DATABASE_URL");
    assertNotNull(base);
    assertTrue(base.contains("restaurant_test"));
    String name = "restaurant_test_legacy_" + UUID.randomUUID().toString().replace("-", "");
    var config = new DatabaseConfig();
    try (var admin = (HikariDataSource) config.dataSource(base)) {
      var root = new JdbcTemplate(admin);
      root.execute("CREATE DATABASE " + name);
      try (var legacy =
          (HikariDataSource)
              config.dataSource(base.substring(0, base.lastIndexOf('/') + 1) + name)) {
        try (var connection = legacy.getConnection()) {
          ScriptUtils.executeSqlScript(
              connection, new ClassPathResource("db/migration/V1__initial.sql"));
        }
        var jdbc = new JdbcTemplate(legacy);
        jdbc.update("INSERT INTO migrations VALUES('001_initial')");
        jdbc.update("INSERT INTO seats(label,table_name) VALUES('Keep me','Existing table')");
        var flyway = Flyway.configure().dataSource(legacy).baselineVersion("1").load();
        config.legacyMigration(legacy).migrate(flyway);
        config.legacyMigration(legacy).migrate(flyway);
        assertEquals(
            1,
            jdbc.queryForObject("SELECT count(*) FROM seats WHERE label='Keep me'", Integer.class));
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE type='BASELINE'", Integer.class));
      } finally {
        root.execute("DROP DATABASE " + name + " WITH (FORCE)");
      }
    }
  }
}
