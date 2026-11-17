package at.restaurant;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GuestController {
  private final Db db;
  private final ReservationService reservations;
  private final PaymentService payments;
  private final OperationsService operations;
  private final QrCodes qr;
  private final AppSettings settings;

  public GuestController(
      Db db,
      ReservationService reservations,
      PaymentService payments,
      OperationsService operations,
      QrCodes qr,
      AppSettings settings) {
    this.db = db;
    this.reservations = reservations;
    this.payments = payments;
    this.operations = operations;
    this.qr = qr;
    this.settings = settings;
  }

  static Map<String, Boolean> ok() {
    return Map.of("ok", true);
  }

  @GetMapping("/health")
  public Map<String, Boolean> health() {
    db.rows("SELECT 1");
    return ok();
  }

  @GetMapping("/menu")
  public Object menu() {
    return operations.menu();
  }

  @PostMapping("/reservations")
  @ResponseStatus(HttpStatus.CREATED)
  public Object reserve(@Valid @RequestBody Requests.Reservation body) {
    return reservations.reserve(body);
  }

  @GetMapping("/reservation")
  public Object view(@RequestHeader("X-Reservation-Token") String access) {
    return reservations.view(access);
  }

  @GetMapping("/qr")
  public Object qr(@RequestHeader("X-Reservation-Token") String access) {
    if (!Boolean.TRUE.equals(reservations.view(access).get("usable")))
      throw ApiException.conflict("QR-Code gesperrt");
    return Map.of("image", qr.dataUrl(settings.publicUrl() + "/#guest=" + access));
  }

  @PostMapping("/orders")
  @ResponseStatus(HttpStatus.CREATED)
  public Object order(
      @RequestHeader("X-Reservation-Token") String access,
      @Valid @RequestBody Requests.Order body) {
    reservations.order(access, body);
    return ok();
  }

  @PostMapping("/cancel")
  @ResponseStatus(HttpStatus.CREATED)
  public Object cancel(@RequestHeader("X-Reservation-Token") String access) {
    return reservations.cancel(access);
  }

  @PostMapping("/finish")
  @ResponseStatus(HttpStatus.CREATED)
  public Object finish(@RequestHeader("X-Reservation-Token") String access) {
    reservations.finish(access, false, null);
    return ok();
  }

  @GetMapping("/group")
  public Object group(@RequestHeader("X-Reservation-Token") String access) {
    return reservations.group(access);
  }

  @PostMapping("/group/cancel")
  @ResponseStatus(HttpStatus.CREATED)
  public Object cancelGroup(@RequestHeader("X-Reservation-Token") String access) {
    return reservations.cancelGroup(access);
  }

  @PostMapping("/group/shift")
  @ResponseStatus(HttpStatus.CREATED)
  public Object shift(
      @RequestHeader("X-Reservation-Token") String access,
      @Valid @RequestBody Requests.Shift body) {
    reservations.shift(access, body.startsAt());
    return ok();
  }

  @PostMapping("/payments")
  @ResponseStatus(HttpStatus.CREATED)
  public Object pay(
      @RequestHeader("X-Reservation-Token") String access,
      @Valid @RequestBody Requests.Payment body) {
    return payments.pay(access, body, false);
  }

  @PostMapping("/payments/{id}/check")
  @ResponseStatus(HttpStatus.CREATED)
  public Object check(@RequestHeader("X-Reservation-Token") String access, @PathVariable UUID id) {
    return payments.reconcile(access, id);
  }

  @PostMapping("/payments/{id}/refund")
  @ResponseStatus(HttpStatus.CREATED)
  public Object refund(@RequestHeader("X-Reservation-Token") String access, @PathVariable UUID id) {
    return payments.refund(access, id, false);
  }

  @PostMapping("/feedback")
  @ResponseStatus(HttpStatus.CREATED)
  public Object feedback(
      @RequestHeader("X-Reservation-Token") String access,
      @Valid @RequestBody Requests.Feedback body) {
    reservations.feedback(access, body);
    return ok();
  }
}
