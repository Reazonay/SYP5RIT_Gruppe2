package at.restaurant;

import jakarta.servlet.http.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
public class StaffPages {
  private final AuthService auth;
  private final AppSettings settings;
  private final ReservationService reservations;
  private final OperationsService operations;
  private final PaymentService payments;
  private final PageSupport pages;
  private final UiFormat format;

  public StaffPages(
      AuthService auth,
      AppSettings settings,
      ReservationService reservations,
      OperationsService operations,
      PaymentService payments,
      PageSupport pages,
      UiFormat format) {
    this.auth = auth;
    this.settings = settings;
    this.reservations = reservations;
    this.operations = operations;
    this.payments = payments;
    this.pages = pages;
    this.format = format;
  }

  @GetMapping("/login")
  String login() {
    return "login";
  }

  @PostMapping("/login")
  String login(
      @RequestParam String email,
      @RequestParam String password,
      HttpServletRequest request,
      HttpServletResponse response) {
    var result = auth.login(pages.valid(new Requests.Login(email, password)));
    SessionSecurity.rotate(request, response);
    response.addHeader(
        HttpHeaders.SET_COOKIE, cookie(result.get("session"), Duration.ofHours(8)).toString());
    return "redirect:/staff";
  }

  @PostMapping("/logout")
  String logout(
      @CookieValue(value = "session", defaultValue = "") String session,
      HttpServletRequest request,
      HttpServletResponse response) {
    auth.logout(session);
    SessionSecurity.rotate(request, response);
    response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    return "redirect:/staff/login";
  }

  private ResponseCookie cookie(String value, Duration duration) {
    return ResponseCookie.from("session", value)
        .path("/")
        .httpOnly(true)
        .secure(settings.production())
        .sameSite("Strict")
        .maxAge(duration)
        .build();
  }

  @GetMapping
  public String index(@AuthenticationPrincipal AuthService.Staff staff) {
    return "redirect:/staff/" + ("cook".equals(staff.role()) ? "kitchen" : "seats");
  }

  @GetMapping("/seats")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String seats(@RequestParam(required = false) String at, Model model) {
    String local = at == null ? format.localNow() : at;
    model.addAttribute("refreshSeconds", 15);
    model.addAttribute("at", local);
    var seats = operations.seats(format.instant(local));
    model.addAttribute("seats", seats);
    var counts = new java.util.HashMap<String, Long>();
    for (String state : java.util.List.of("free", "reserved", "occupied"))
      counts.put(
          state,
          seats.stream()
              .filter(
                  seat ->
                      Boolean.TRUE.equals(seat.get("active")) && state.equals(seat.get("status")))
              .count());
    model.addAttribute("seatCounts", counts);
    model.addAttribute("visits", operations.visits());
    return "seats";
  }

  @PostMapping("/visits/{id}/finish")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String endById(@PathVariable UUID id, RedirectAttributes flash) {
    reservations.finish(null, true, id);
    return pages.saved(flash, "/staff/seats");
  }

  @GetMapping("/visit")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String visit(Model model, HttpSession session) {
    return pages.visit(model, session, true);
  }

  @PostMapping("/access")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String access(@RequestParam String access, HttpSession session, RedirectAttributes flash) {
    String token = GuestPages.extract(access);
    reservations.view(token);
    session.setAttribute(PageSupport.STAFF_GUEST, token);
    return pages.saved(flash, "/staff/visit");
  }

  @PostMapping("/visit/check-in")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String checkIn(HttpSession session, RedirectAttributes flash) {
    reservations.checkIn(pages.access(session, true));
    return pages.saved(flash, "/staff/visit");
  }

  @PostMapping("/visit/finish")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String finish(HttpSession session, RedirectAttributes flash) {
    reservations.finish(pages.access(session, true), true, null);
    return pages.saved(flash, "/staff/visit");
  }

  @PostMapping("/visit/cancel")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String cancel(HttpSession session, RedirectAttributes flash) {
    reservations.cancel(pages.access(session, true));
    return pages.saved(flash, "/staff/visit");
  }

  @PostMapping("/visit/order")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String order(
      @RequestParam int menuId,
      @RequestParam int quantity,
      HttpSession session,
      RedirectAttributes flash) {
    reservations.order(
        pages.access(session, true), pages.valid(new Requests.Order(menuId, quantity)));
    return pages.saved(flash, "/staff/visit");
  }

  @PostMapping("/visit/pay")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String pay(
      @RequestParam BigDecimal amount,
      @RequestParam UUID requestKey,
      HttpSession session,
      RedirectAttributes flash) {
    payments.pay(
        pages.access(session, true),
        pages.valid(new Requests.Payment("cash", requestKey, format.cents(amount))),
        true);
    return pages.saved(flash, "/staff/visit");
  }

  @PostMapping("/visit/payments/{id}/check")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String check(@PathVariable UUID id, HttpSession session, RedirectAttributes flash) {
    payments.reconcile(pages.access(session, true), id);
    return pages.saved(flash, "/staff/visit");
  }

  @PostMapping("/visit/payments/{id}/refund")
  @PreAuthorize("hasAnyAuthority('manager','waiter')")
  String refund(@PathVariable UUID id, HttpSession session, RedirectAttributes flash) {
    payments.refund(pages.access(session, true), id, true);
    return pages.saved(flash, "/staff/visit");
  }

  @GetMapping("/kitchen")
  @PreAuthorize("hasAnyAuthority('manager','waiter','cook')")
  String kitchen(Model model) {
    model.addAttribute("refreshSeconds", 15);
    model.addAttribute("orders", operations.kitchen());
    return "kitchen";
  }

  @PostMapping("/kitchen/orders/{id}")
  @PreAuthorize("hasAnyAuthority('manager','waiter','cook')")
  String orderStatus(
      @PathVariable UUID id,
      @RequestParam String status,
      @AuthenticationPrincipal AuthService.Staff staff,
      RedirectAttributes flash) {
    pages.valid(new Requests.OrderStatus(status));
    operations.orderStatus(id, status, staff.role());
    return pages.saved(flash, "/staff/kitchen");
  }

  @GetMapping("/manage")
  @PreAuthorize("hasAuthority('manager')")
  String manage(Model model) {
    model.addAttribute("menu", operations.menu());
    model.addAttribute("seats", operations.seats(java.time.Instant.now()));
    return "manage";
  }

  @PostMapping("/manage/menu")
  @PreAuthorize("hasAuthority('manager')")
  String addMenu(
      @RequestParam String name,
      @RequestParam String category,
      @RequestParam BigDecimal price,
      RedirectAttributes flash) {
    operations.addMenu(pages.valid(new Requests.Menu(name, category, format.cents(price), true)));
    return pages.saved(flash, "/staff/manage");
  }

  @PostMapping("/manage/menu/{id}")
  @PreAuthorize("hasAuthority('manager')")
  String editMenu(
      @PathVariable int id,
      @RequestParam String name,
      @RequestParam String category,
      @RequestParam BigDecimal price,
      @RequestParam(defaultValue = "false") boolean available,
      RedirectAttributes flash) {
    operations.editMenu(
        id, pages.valid(new Requests.Menu(name, category, format.cents(price), available)));
    return pages.saved(flash, "/staff/manage");
  }

  @PostMapping("/manage/seats")
  @PreAuthorize("hasAuthority('manager')")
  String addSeat(
      @RequestParam String label, @RequestParam String tableName, RedirectAttributes flash) {
    operations.addSeat(pages.valid(new Requests.Seat(label, tableName)));
    return pages.saved(flash, "/staff/manage");
  }

  @PostMapping("/manage/seats/{id}")
  @PreAuthorize("hasAuthority('manager')")
  String seatActive(@PathVariable int id, @RequestParam boolean active, RedirectAttributes flash) {
    operations.seatActive(id, active);
    return pages.saved(flash, "/staff/manage");
  }
}
