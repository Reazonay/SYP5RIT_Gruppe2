package at.restaurant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
class StartupCommand implements ApplicationRunner {
  private final SeedService seed;
  private final ConfigurableApplicationContext context;
  private final String command, password;
  private final boolean seedOnStart;

  StartupCommand(
      SeedService seed,
      ConfigurableApplicationContext context,
      @Value("${app.command:}") String command,
      @Value("${app.seed-password}") String password,
      @Value("${app.seed}") boolean seedOnStart) {
    this.seed = seed;
    this.context = context;
    this.command = command;
    this.password = password;
    this.seedOnStart = seedOnStart;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (seedOnStart || "seed".equals(command)) seed.seed(password);
    if (!command.isBlank()) {
      if (!java.util.Set.of("seed", "migrate").contains(command))
        throw new IllegalArgumentException("Unknown command: " + command);
      SpringApplication.exit(context);
    }
  }
}
