package kh.karazin.foodwise.store.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class StoreOpeningHoursTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private Store storeWithHours(LocalTime opensAt, LocalTime closesAt) {
        return Store.create("S", null, null, null, StoreType.RESTAURANT, null, "addr",
                50.45, 30.52, null, opensAt, closesAt, null, null, null, null, null);
    }

    @Test
    void isCurrentlyOpen_false_when_hours_missing() {
        assertThat(storeWithHours(null, null).isCurrentlyOpen(KYIV)).isFalse();
    }

    @Test
    void isCurrentlyOpen_true_for_all_day_window() {
        // 00:00–23:59 always contains the current wall-clock time.
        assertThat(storeWithHours(LocalTime.of(0, 0), LocalTime.of(23, 59)).isCurrentlyOpen(KYIV)).isTrue();
    }

    @Test
    void isCurrentlyOpen_handles_overnight_window() {
        // 22:00–02:00 overnight: open iff now >= 22:00 OR now <= 02:00.
        LocalTime now = LocalTime.now(KYIV);
        boolean expected = !now.isBefore(LocalTime.of(22, 0)) || !now.isAfter(LocalTime.of(2, 0));
        assertThat(storeWithHours(LocalTime.of(22, 0), LocalTime.of(2, 0)).isCurrentlyOpen(KYIV))
                .isEqualTo(expected);
    }
}
