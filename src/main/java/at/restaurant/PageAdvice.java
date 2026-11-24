package at.restaurant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(assignableTypes = {GuestPages.class, StaffPages.class})
public class PageAdvice {
  private final AppSettings settings;

  public PageAdvice(AppSettings settings) {
    this.settings = settings;
  }

  @ModelAttribute("pagePath")
  public String pagePath(HttpServletRequest request) {
    return request.getRequestURI();
  }

  @ModelAttribute("staff")
  public AuthService.Staff staff() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getPrincipal() instanceof AuthService.Staff staff ? staff : null;
  }

  @ModelAttribute("roleSwitchEnabled")
  public boolean roleSwitchEnabled() {
    return !settings.production();
  }

  @ModelAttribute("activeWorkspace")
  public String activeWorkspace(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null || !uri.startsWith("/staff")) return "widget";
    var current = staff();
    if (current != null) return current.role();
    return "staff";
  }

  @ExceptionHandler({
    ApiException.class,
    org.springframework.dao.DataAccessException.class,
    org.springframework.security.access.AccessDeniedException.class
  })
  public org.springframework.web.servlet.ModelAndView failure(
      Exception error, HttpServletRequest request) {
    int code =
        error instanceof ApiException domain
            ? domain.status()
            : error instanceof org.springframework.security.access.AccessDeniedException
                ? 403
                : 409;
    String text =
        error instanceof ApiException
            ? error.getMessage()
            : code == 403
                ? "Keine Berechtigung"
                : "Die Änderung konnte wegen eines Konflikts nicht gespeichert werden.";
    return page(code, text, request);
  }

  @ExceptionHandler({
    org.springframework.validation.BindException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class
  })
  public org.springframework.web.servlet.ModelAndView invalid(
      Exception error, HttpServletRequest request) {
    return page(400, "Bitte alle Felder korrekt ausfüllen.", request);
  }

  private org.springframework.web.servlet.ModelAndView page(
      int status, String message, HttpServletRequest request) {
    var model = new org.springframework.web.servlet.ModelAndView("page-error");
    model.setStatus(HttpStatus.valueOf(status));
    model.addObject("error", message);
    model.addObject("back", request.getRequestURI().startsWith("/staff") ? "/staff" : "/visit");
    model.addObject("staff", staff());
    return model;
  }
}
