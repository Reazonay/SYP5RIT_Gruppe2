package at.restaurant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Requests {
  private Requests() {}

  public record Person(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 3, max = 40) String phone,
      @NotNull @Pattern(regexp = "cash|card|paypal") String method) {}

  public record Reservation(
      @NotNull Instant startsAt,
      @NotNull @Min(30) @Max(240) Integer duration,
      @NotEmpty @Size(max = 10) List<@Valid Person> people) {}

  public record Order(
      @NotNull @Positive Integer menuId, @NotNull @Min(1) @Max(99) Integer quantity) {}

  public record Payment(
      @NotNull @Pattern(regexp = "cash|card|paypal") String method,
      @NotNull UUID requestKey,
      @NotNull @Min(1) @Max(10000000) Integer amount) {}

  public record Shift(@NotNull Instant startsAt) {}

  public record Feedback(
      @NotNull @Min(1) @Max(5) Integer rating, @NotNull @Size(max = 2000) String comment) {}

  public record Login(@NotBlank @Email String email, @NotNull @Size(max = 256) String password) {}

  public record Menu(
      @NotBlank @Size(max = 120) String name,
      @NotNull @Pattern(regexp = "food|drink") String category,
      @NotNull @Min(0) @Max(100000) Integer price,
      @NotNull Boolean available) {}

  public record Seat(
      @NotBlank @Size(max = 80) String label, @NotBlank @Size(max = 80) String tableName) {}

  public record Active(@NotNull Boolean active) {}

  public record OrderStatus(@NotNull @Pattern(regexp = "preparing|ready|served") String status) {}
}
