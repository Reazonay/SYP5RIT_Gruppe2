package at.restaurant;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GuestPages {
  private final ReservationService reservations;
  private final PaymentService payments;
  private final OperationsService operations;
  private final PageSupport pages;
  private final UiFormat format;

  public GuestPages(
      ReservationService reservations,
      PaymentService payments,
      OperationsService operations,
      PageSupport pages,
      UiFormat format) {
    this.reservations = reservations;
    this.payments = payments;
    this.operations = operations;
    this.pages = pages;
    this.format = format;
  }

  @InitBinder("reservationForm")
  void fields(WebDataBinder binder) {
    binder.initDirectFieldAccess();
    binder.setAutoGrowCollectionLimit(10);
  }

  @GetMapping("/")
  String home(@RequestParam(defaultValue = "1") int people, Model model) {
    model.addAttribute(
        "reservationForm", new UiForms.Reservation(Math.max(1, Math.min(10, people))));
    model.addAttribute("menu", operations.menu());
    return "reserve";
  }

  @PostMapping("/reserve")
  String reserve(
      @Valid @ModelAttribute("reservationForm") UiForms.Reservation form,
      BindingResult errors,
      Model model,
      HttpSession session,
      RedirectAttributes flash) {
    if (errors.hasErrors()) {
      model.addAttribute("menu", operations.menu());
      return "reserve";
    }
    var request =
        pages.valid(
            new Requests.Reservation(
                format.instant(form.startsAt),
                form.duration,
                form.people.stream().map(UiForms.Person::request).toList()));
    var booking = reservations.reserve(request);
    session.setAttribute(PageSupport.GUEST, booking.get("guestToken"));
    session.setAttribute(PageSupport.GROUP, booking.get("organizerToken"));
    return pages.saved(flash, "/visit");
  }

  @GetMapping("/visit")
  String visit(Model model, HttpSession session) {
    return pages.visit(model, session, false);
  }

  @GetMapping("/menu")
  String menu(Model model, HttpSession session) {
    String token = (String) session.getAttribute(PageSupport.GUEST);
    boolean ordering = false;
    if (token != null) {
      var visit = reservations.view(token);
      model.addAttribute("visit", visit);
      ordering = Boolean.TRUE.equals(visit.get("usable"));
    }
    model.addAttribute("ordering", ordering);
    model.addAttribute("base", "/visit");
    model.addAttribute("menu", operations.menu());
    return "menu";
  }

  @PostMapping("/access")
  String access(
      @RequestParam String access,
      @RequestParam(defaultValue = "guest") String kind,
      HttpSession session,
      RedirectAttributes flash) {
    String token = extract(access);
    if ("group".equals(kind)) {
      reservations.group(token);
      session.setAttribute(PageSupport.GROUP, token);
      return pages.saved(flash, "/group");
    }
    reservations.view(token);
    session.setAttribute(PageSupport.GUEST, token);
    return pages.saved(flash, "/visit");
  }

  static String extract(String value) {
    var matcher = java.util.regex.Pattern.compile("(?:guest|group)=([a-f0-9]{64})").matcher(value);
    return Tokens.validate(matcher.find() ? matcher.group(1) : value.trim());
  }

  @PostMapping("/visit/order")
  String order(
      @RequestParam int menuId,
      @RequestParam int quantity,
      HttpSession session,
      RedirectAttributes flash) {
    reservations.order(
        pages.access(session, false), pages.valid(new Requests.Order(menuId, quantity)));
    return pages.saved(flash, "/visit");
  }

  @PostMapping("/visit/cancel")
  String cancel(HttpSession session, RedirectAttributes flash) {
    reservations.cancel(pages.access(session, false));
    return pages.saved(flash, "/visit");
  }

  @PostMapping("/visit/finish")
  String finish(HttpSession session, RedirectAttributes flash) {
    reservations.finish(pages.access(session, false), false, null);
    return pages.saved(flash, "/visit");
  }

  @PostMapping("/visit/pay")
  String pay(
      @RequestParam BigDecimal amount,
      @RequestParam String method,
      @RequestParam UUID requestKey,
      HttpSession session,
      RedirectAttributes flash) {
    var payment =
        payments.pay(
            pages.access(session, false),
            pages.valid(new Requests.Payment(method, requestKey, format.cents(amount))),
            false);
    return payment.get("checkout_url") == null
        ? pages.saved(flash, "/visit")
        : "redirect:" + payment.get("checkout_url");
  }

  @PostMapping("/visit/payments/{id}/check")
  String check(@PathVariable UUID id, HttpSession session, RedirectAttributes flash) {
    payments.reconcile(pages.access(session, false), id);
    return pages.saved(flash, "/visit");
  }

  @PostMapping("/visit/payments/{id}/refund")
  String refund(@PathVariable UUID id, HttpSession session, RedirectAttributes flash) {
    payments.refund(pages.access(session, false), id, false);
    return pages.saved(flash, "/visit");
  }

  @PostMapping("/visit/feedback")
  String feedback(
      @RequestParam int rating,
      @RequestParam(defaultValue = "") String comment,
      HttpSession session,
      RedirectAttributes flash) {
    reservations.feedback(
        pages.access(session, false), pages.valid(new Requests.Feedback(rating, comment)));
    return pages.saved(flash, "/visit");
  }

  @GetMapping("/group")
  String group(Model model, HttpSession session) {
    String access = (String) session.getAttribute(PageSupport.GROUP);
    if (access != null) model.addAttribute("group", reservations.group(access));
    return "group";
  }

  @PostMapping("/group/cancel")
  String cancelGroup(HttpSession session, RedirectAttributes flash) {
    reservations.cancelGroup(pages.group(session));
    return pages.saved(flash, "/group");
  }

  @PostMapping("/group/shift")
  String shift(@RequestParam String startsAt, HttpSession session, RedirectAttributes flash) {
    reservations.shift(pages.group(session), format.instant(startsAt));
    return pages.saved(flash, "/group");
  }
}
