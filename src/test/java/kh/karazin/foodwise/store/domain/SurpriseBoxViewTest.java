package kh.karazin.foodwise.store.domain;

import kh.karazin.foodwise.common.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class SurpriseBoxViewTest {

    private static final Currency UAH = Currency.getInstance("UAH");

    private SurpriseBoxView box(Money price, Money retailPrice) {
        return new SurpriseBoxView(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "Test Box", "desc", "img", price, retailPrice, 5,
                null, null, false, "category", null,
                "Store", "Kyiv", null, null);
    }

    @Test
    void discount_is_20_when_price_is_80_percent_of_retail() {
        int discount = box(
                Money.ofMajor(new BigDecimal("4.00"), UAH),
                Money.ofMajor(new BigDecimal("5.00"), UAH)).discountPercentage();
        assertThat(discount).isEqualTo(20);
    }

    @Test
    void discount_is_zero_when_retailPrice_is_null() {
        int discount = box(Money.ofMajor(new BigDecimal("3.00"), UAH), null).discountPercentage();
        assertThat(discount).isZero();
    }

    @Test
    void discount_is_clamped_to_zero_when_price_exceeds_retail() {
        int discount = box(
                Money.ofMajor(new BigDecimal("6.00"), UAH),
                Money.ofMajor(new BigDecimal("5.00"), UAH)).discountPercentage();
        assertThat(discount)
                .as("discount must not be negative when price exceeds retail")
                .isZero();
    }
}
