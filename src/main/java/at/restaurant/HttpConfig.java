package at.restaurant;

import java.time.Duration;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class HttpConfig {
  @Bean
  RestClient.Builder paymentHttp() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(15));
    factory.setReadTimeout(Duration.ofSeconds(15));
    return RestClient.builder().requestFactory(factory);
  }
}
