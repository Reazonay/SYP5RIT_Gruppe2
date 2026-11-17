package at.restaurant;

import java.time.Clock;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
class BackgroundJobs {
  private final ReservationService reservations;
  private final PaymentService payments;
  private final OutboxService outbox;
  private final Db db;
  private final Clock clock;

  BackgroundJobs(
      ReservationService reservations,
      PaymentService payments,
      OutboxService outbox,
      Db db,
      Clock clock) {
    this.reservations = reservations;
    this.payments = payments;
    this.outbox = outbox;
    this.db = db;
    this.clock = clock;
  }

  @Scheduled(fixedDelay = 15000, initialDelay = 15000)
  public void tick() {
    try {
      reservations.expireNoShows();
      payments.reconcilePending();
      outbox.deliver();
      db.update("DELETE FROM sessions WHERE expires_at<?", clock.instant());
    } catch (RuntimeException e) {
      LoggerFactory.getLogger(getClass())
          .error("Background job failed: {}", e.getClass().getSimpleName());
    }
  }
}
