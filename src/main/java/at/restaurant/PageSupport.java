package at.restaurant;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
public class PageSupport {
  static final String GUEST = "guestAccess",
      GROUP = "groupAccess",
      STAFF_GUEST = "staffGuestAccess";
  private final Validator validator;
  private final ReservationService reservations;
  private final OperationsService operations;
  private final QrCodes qr;
  private final AppSettings settings;

  public PageSupport(
      Validator validator,
      ReservationService reservations,
      OperationsService operations,
      QrCodes qr,
      AppSettings settings) {
    this.validator = validator;
    this.reservations = reservations;
    this.operations = operations;
    this.qr = qr;
    this.settings = settings;
  }

  public <T> T valid(T value) {
    var errors = validator.validate(value);
    if (!errors.isEmpty())
      throw new ApiException(
          400,
          "Bitte alle Felder korrekt ausfüllen: " + errors.iterator().next().getPropertyPath());
    return value;
  }

  public String access(HttpSession session, boolean staff) {
    String value = (String) session.getAttribute(staff ? STAFF_GUEST : GUEST);
    if (value == null) throw new ApiException(400, "Bitte zuerst den Reservierungszugang öffnen");
    return value;
  }

  public String group(HttpSession session) {
    String value = (String) session.getAttribute(GROUP);
    if (value == null) throw new ApiException(400, "Bitte zuerst den Gruppenzugang öffnen");
    return value;
  }

  public String visit(Model model, HttpSession session, boolean staff) {
    String token = (String) session.getAttribute(staff ? STAFF_GUEST : GUEST);
    model.addAttribute("staffView", staff);
    model.addAttribute("base", staff ? "/staff/visit" : "/visit");
    if (token != null) {
      var visit = reservations.view(token);
      model.addAttribute("visit", visit);
      if (Boolean.TRUE.equals(visit.get("usable")))
        model.addAttribute("qr", qr.dataUrl(settings.publicUrl() + "/#guest=" + token));
    }
    model.addAttribute("menu", operations.menu());
    model.addAttribute("requestKey", java.util.UUID.randomUUID());
    return "visit";
  }

  public String saved(RedirectAttributes flash, String target) {
    flash.addFlashAttribute("message", "Gespeichert.");
    return "redirect:" + target;
  }
}
