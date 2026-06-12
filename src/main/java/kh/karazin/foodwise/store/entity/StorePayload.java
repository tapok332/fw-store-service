package kh.karazin.foodwise.store.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * POJO for JSONB store_payload column.
 * Contains working hours, tags, payment methods, and additional data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorePayload implements Serializable {

    private Map<DayOfWeek, DayHours> workingHours;
    private List<String> tags;
    private List<String> paymentMethods;
    private Map<String, Object> additionalData;

    /**
     * Check if the store is currently open based on working hours.
     */
    public boolean isCurrentlyOpen() {
        if (workingHours == null || workingHours.isEmpty()) {
            return false;
        }
        DayOfWeek today = DayOfWeek.from(java.time.LocalDate.now().getDayOfWeek());
        DayHours hours = workingHours.get(today);
        if (hours == null || hours.isClosed()) {
            return false;
        }
        LocalTime now = LocalTime.now();
        return !now.isBefore(hours.getOpens()) && !now.isAfter(hours.getCloses());
    }

    /**
     * Represents working hours for a single day.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayHours implements Serializable {
        private LocalTime opens;
        private LocalTime closes;
        private boolean closed;

        public boolean isClosed() {
            return closed;
        }
    }
}
