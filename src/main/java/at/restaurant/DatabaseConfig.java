package at.restaurant;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class DatabaseConfig {
  @Bean
  DataSource dataSource(@Value("${app.database-url}") String value) {
    var source = new HikariDataSource();
    if (value.startsWith("jdbc:")) {
      source.setJdbcUrl(value);
    } else {
      URI uri = URI.create(value);
      if (!java.util.Set.of("postgres", "postgresql").contains(uri.getScheme()))
        throw new IllegalArgumentException("PostgreSQL DATABASE_URL required");
      String[] credentials =
          uri.getRawUserInfo() == null
              ? new String[] {"restaurant", "restaurant"}
              : uri.getRawUserInfo().split(":", 2);
      source.setUsername(URLDecoder.decode(credentials[0], StandardCharsets.UTF_8));
      source.setPassword(
          credentials.length > 1 ? URLDecoder.decode(credentials[1], StandardCharsets.UTF_8) : "");
      source.setJdbcUrl(
          "jdbc:postgresql://"
              + uri.getHost()
              + ":"
              + (uri.getPort() < 0 ? 5432 : uri.getPort())
              + uri.getRawPath()
              + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()));
    }
    source.setMaximumPoolSize(10);
    source.setConnectionTimeout(10000);
    return source;
  }

  @Bean
  FlywayMigrationStrategy legacyMigration(DataSource dataSource) {
    return flyway -> {
      var jdbc = new JdbcTemplate(dataSource);
      boolean history =
          Boolean.TRUE.equals(
              jdbc.queryForObject(
                  "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL", Boolean.class));
      boolean legacy =
          Boolean.TRUE.equals(
              jdbc.queryForObject(
                  "SELECT to_regclass('public.migrations') IS NOT NULL", Boolean.class));
      if (!history && legacy) {
        Integer known =
            jdbc.queryForObject(
                "SELECT count(*) FROM migrations WHERE name='001_initial'", Integer.class);
        if (known == null || known != 1)
          throw new IllegalStateException("Unknown legacy database; migration requires inspection");
        flyway.baseline();
      }
      flyway.migrate();
    };
  }
}
