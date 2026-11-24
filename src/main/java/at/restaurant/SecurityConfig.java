package at.restaurant;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
class SecurityConfig {
  @Bean
  SecurityFilterChain security(
      HttpSecurity http, AuthService auth, AppSettings settings, Clock clock) throws Exception {
    http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .securityContext(
            context ->
                context
                    .requireExplicitSave(true)
                    .securityContextRepository(
                        new org.springframework.security.web.context
                            .NullSecurityContextRepository()))
        .requestCache(
            cache ->
                cache.requestCache(
                    new org.springframework.security.web.savedrequest.NullRequestCache()))
        .sessionManagement(session -> session.requireExplicitAuthenticationStrategy(true));
    http.authorizeHttpRequests(
        r ->
            r.requestMatchers("/staff/login", "/staff/switch")
                .permitAll()
                .requestMatchers("/api/staff/**", "/staff", "/staff/**")
                .authenticated()
                .anyRequest()
                .permitAll());
    http.exceptionHandling(
        e ->
            e.authenticationEntryPoint(
                    (req, res, error) -> {
                      if (req.getRequestURI().startsWith("/api/"))
                        reject(res, 401, "Bitte anmelden");
                      else res.sendRedirect("/staff/login");
                    })
                .accessDeniedHandler((req, res, error) -> reject(res, 403, "Keine Berechtigung")));
    http.addFilterBefore(new ApiFilter(auth, settings, clock), AnonymousAuthenticationFilter.class);
    http.headers(
        h ->
            h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; img-src 'self' data:; style-src 'self'; object-src"
                                + " 'none'; frame-ancestors 'none'"))
                .referrerPolicy(
                    r ->
                        r.policy(
                            org.springframework.security.web.header.writers
                                .ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)));
    return http.build();
  }

  static void reject(HttpServletResponse response, int code, String message) throws IOException {
    response.setStatus(code);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"message\":\"" + message + "\"}");
  }

  static class ApiFilter extends OncePerRequestFilter {
    private final AuthService auth;
    private final AppSettings settings;
    private final Clock clock;

    private record Bucket(int count, long until) {}

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    ApiFilter(AuthService auth, AppSettings settings, Clock clock) {
      this.auth = auth;
      this.settings = settings;
      this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
      res.setHeader("Cache-Control", "no-store");
      if (!Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod())) {
        String origin = req.getHeader("Origin");
        boolean api = req.getRequestURI().startsWith("/api/");
        if ((api || (origin != null && !"null".equals(origin)))
            && !settings.origin().equals(origin)) {
          reject(res, 403, "Ungültiger Ursprung");
          return;
        }
        long now = clock.millis();
        boolean login =
            (req.getRequestURI().equals("/api/login")
                || req.getRequestURI().equals("/staff/login"));
        String key = req.getRemoteAddr() + ":" + (login ? "login" : "write");
        Bucket bucket =
            buckets.compute(
                key,
                (k, old) ->
                    old == null || old.until() <= now
                        ? new Bucket(1, now + 60000)
                        : new Bucket(old.count() + 1, old.until()));
        if (bucket.count() > (login ? 10 : 120)) {
          reject(res, 429, "Zu viele Anfragen. Bitte kurz warten.");
          return;
        }
        if (buckets.size() > 10000) buckets.entrySet().removeIf(e -> e.getValue().until() < now);
      }
      if (req.getCookies() != null)
        for (Cookie cookie : req.getCookies())
          if ("session".equals(cookie.getName())) {
            auth.authenticate(cookie.getValue())
                .ifPresent(
                    staff ->
                        SecurityContextHolder.getContext()
                            .setAuthentication(
                                UsernamePasswordAuthenticationToken.authenticated(
                                    staff,
                                    null,
                                    List.of(new SimpleGrantedAuthority(staff.role())))));
            break;
          }
      try {
        chain.doFilter(req, res);
      } finally {
        SecurityContextHolder.clearContext();
      }
    }
  }
}
