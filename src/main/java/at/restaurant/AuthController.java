package at.restaurant;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final AuthService auth;
  private final AppSettings settings;

  public AuthController(AuthService auth, AppSettings settings) {
    this.auth = auth;
    this.settings = settings;
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.CREATED)
  public Object login(@Valid @RequestBody Requests.Login body, HttpServletResponse response) {
    var login = auth.login(body);
    response.addHeader(
        HttpHeaders.SET_COOKIE, cookie(login.get("session"), Duration.ofHours(8)).toString());
    return Map.of("role", login.get("role"));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.CREATED)
  public Object logout(
      @CookieValue(value = "session", defaultValue = "") String session,
      HttpServletResponse response) {
    auth.logout(session);
    response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    return GuestController.ok();
  }

  private ResponseCookie cookie(String value, Duration lifetime) {
    return ResponseCookie.from("session", value)
        .httpOnly(true)
        .sameSite("Strict")
        .secure(settings.production())
        .path("/")
        .maxAge(lifetime)
        .build();
  }
}
