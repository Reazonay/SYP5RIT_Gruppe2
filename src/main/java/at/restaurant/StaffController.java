package at.restaurant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
@Validated
public class StaffController {
  private final ReservationService reservations;
  private final PaymentService payments;
  private final OperationsService operations;

  public StaffController(
      ReservationService reservations, PaymentService payments, OperationsService operations) {
    this.reservations = reservations;
    this.payments = payments;
    this.operations = operations;
  }

  @GetMapping("/me")
  @PreAuthorize("hasAnyAuthority('manager','waiter','cook')")
  public Object me(@AuthenticationPrincipal AuthService.Staff staff) {
    return staff;
  }

  @GetMapping("/seats")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  public Object seats(@RequestParam Instant at) {
    return operations.seats(at);
  }

  @GetMapping("/visits")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  public Object visits() {
    return operations.visits();
  }

  @GetMapping("/reservation")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  public Object view(@RequestHeader("X-Reservation-Token") String access) {
    return reservations.view(access);
  }

  @PostMapping("/check-in")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  public Object checkIn(@RequestHeader("X-Reservation-Token") String access) {
    return reservations.checkIn(access);
  }

  @PostMapping("/finish")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  public Object finish(@RequestHeader("X-Reservation-Token") String access) {
    reservations.finish(access, true, null);
    return GuestController.ok();
  }

  @PostMapping("/visits/{id}/finish")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  public Object finishById(@PathVariable UUID id) {
    reservations.finish(null, true, id);
    return GuestController.ok();
  }

  @PostMapping("/payments")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  public Object cash(
      @RequestHeader("X-Reservation-Token") String access,
      @Valid @RequestBody Requests.Payment body) {
    if (!"cash".equals(body.method()))
      throw new ApiException(400, "Hier sind nur Barzahlungen möglich");
    return payments.pay(access, body, true);
  }

  @PostMapping("/payments/{id}/refund")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  public Object refund(@RequestHeader("X-Reservation-Token") String access, @PathVariable UUID id) {
    return payments.refund(access, id, true);
  }

  @GetMapping("/kitchen")
  @PreAuthorize("hasAnyAuthority('manager','waiter','cook')")
  public Object kitchen() {
    return operations.kitchen();
  }

  @PatchMapping("/orders/{id}")
  @PreAuthorize("hasAnyAuthority('manager','waiter','cook')")
  public Object status(
      @PathVariable UUID id,
      @Valid @RequestBody Requests.OrderStatus body,
      @AuthenticationPrincipal AuthService.Staff staff) {
    operations.orderStatus(id, body.status(), staff.role());
    return GuestController.ok();
  }

  @PostMapping("/menu")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('manager')")
  public Object addMenu(@Valid @RequestBody Requests.Menu body) {
    return operations.addMenu(body);
  }

  @PatchMapping("/menu/{id}")
  @PreAuthorize("hasAuthority('manager')")
  public Object editMenu(@PathVariable @Positive int id, @Valid @RequestBody Requests.Menu body) {
    return operations.editMenu(id, body);
  }

  @PostMapping("/seats")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('manager')")
  public Object addSeat(@Valid @RequestBody Requests.Seat body) {
    return operations.addSeat(body);
  }

  @PatchMapping("/seats/{id}")
  @PreAuthorize("hasAuthority('manager')")
  public Object seatActive(
      @PathVariable @Positive int id, @Valid @RequestBody Requests.Active body) {
    return operations.seatActive(id, body.active());
  }
}
