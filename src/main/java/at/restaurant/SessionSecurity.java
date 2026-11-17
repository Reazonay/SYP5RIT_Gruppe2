package at.restaurant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

final class SessionSecurity {
  private SessionSecurity() {}

  // Cookie authentication is restored per request. Rotate browser state only on an actual
  // login/logout.
  static void rotate(HttpServletRequest request, HttpServletResponse response) {
    if (request.getSession(false) != null) request.changeSessionId();
    new HttpSessionCsrfTokenRepository().saveToken(null, request, response);
  }
}
