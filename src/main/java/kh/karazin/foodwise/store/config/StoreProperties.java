package kh.karazin.foodwise.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

@Validated
@ConfigurationProperties(prefix = "foodwise.store")
public record StoreProperties(ZoneId timezone) {
}
