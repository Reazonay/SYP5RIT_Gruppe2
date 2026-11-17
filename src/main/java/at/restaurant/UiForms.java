package at.restaurant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public final class UiForms {
  private UiForms() {}

  public static class Person {
    @NotBlank
    @Size(max = 120)
    public String name = "";

    @NotBlank
    @Email
    @Size(max = 254)
    public String email = "";

    @NotBlank
    @Size(min = 3, max = 40)
    public String phone = "";

    @Pattern(regexp = "cash|card|paypal")
    public String method = "cash";

    Requests.Person request() {
      return new Requests.Person(name.trim(), email, phone.trim(), method);
    }
  }

  public static class Reservation {
    @NotBlank public String startsAt = "";

    @Min(30)
    @Max(240)
    public int duration = 60;

    @NotEmpty
    @Size(max = 10)
    @Valid
    public List<Person> people = new ArrayList<>(List.of(new Person()));

    public Reservation() {}

    public Reservation(int count) {
      people = new ArrayList<>();
      for (int i = 0; i < count; i++) people.add(new Person());
    }
  }
}
