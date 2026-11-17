package at.restaurant;

import java.util.UUID;

public interface PaymentGateway {
  record Checkout(String id, String url) {}

  void validate(String method);

  Checkout checkout(String method, int amount, UUID requestKey, String guestUrl);

  String check(String method, String providerId, int amount, UUID paymentId);

  String refund(String method, String providerId, int amount, UUID refundId);
}
