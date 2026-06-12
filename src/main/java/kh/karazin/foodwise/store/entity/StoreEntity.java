package kh.karazin.foodwise.store.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import kh.karazin.foodwise.common.money.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Store entity with PostGIS location support and JSONB payload.
 */
@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "hero_image_url")
    private String heroImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private StoreType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(name = "address")
    private String address;

    @Column(name = "location", columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "rating", precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "phone")
    private String phone;

    @Column(name = "website")
    private String website;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountMinor", column = @Column(name = "delivery_fee_amount_minor")),
            @AttributeOverride(name = "currency",    column = @Column(name = "delivery_fee_currency", length = 3))
    })
    private Money deliveryFee;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountMinor", column = @Column(name = "min_order_amount_minor")),
            @AttributeOverride(name = "currency",    column = @Column(name = "min_order_currency", length = 3))
    })
    private Money minOrderAmount;

    @Column(name = "price_level")
    private Integer priceLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "store_payload", columnDefinition = "jsonb")
    private StorePayload storePayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    @Builder.Default
    private List<StoreMenuItemEntity> menuItems = new ArrayList<>();

    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MenuSectionEntity> menuSections = new ArrayList<>();

    /**
     * Check if the store is currently open based on opens_at/closes_at.
     * Caller must pass the store's wall-clock zone (configured via foodwise.store.timezone /
     * STORE_TIMEZONE env var) — opensAt/closesAt are stored as wall-clock, not UTC, so the
     * container default zone (often UTC) would misalign the comparison.
     */
    public boolean isCurrentlyOpen(ZoneId zone) {
        if (opensAt == null || closesAt == null) {
            return false;
        }
        LocalTime now = LocalTime.now(zone);
        if (closesAt.isAfter(opensAt)) {
            return !now.isBefore(opensAt) && !now.isAfter(closesAt);
        }
        // Handles overnight hours (e.g., 22:00 - 02:00)
        return !now.isBefore(opensAt) || !now.isAfter(closesAt);
    }

    /**
     * Get working hours from the JSONB payload, if available.
     */
    public String getWorkingHoursJson() {
        if (storePayload != null && storePayload.getWorkingHours() != null) {
            return storePayload.getWorkingHours().toString();
        }
        return null;
    }
}
