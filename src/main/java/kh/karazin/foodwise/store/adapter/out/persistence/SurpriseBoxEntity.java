package kh.karazin.foodwise.store.adapter.out.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kh.karazin.foodwise.common.money.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Surprise box entity — discounted food boxes available for pickup.
 */
@Entity
@Table(name = "surprise_boxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseBoxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountMinor", column = @Column(name = "price_amount_minor", nullable = false)),
            @AttributeOverride(name = "currency",    column = @Column(name = "price_currency", length = 3, nullable = false))
    })
    private Money price;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountMinor", column = @Column(name = "retail_price_amount_minor")),
            @AttributeOverride(name = "currency",    column = @Column(name = "retail_price_currency", length = 3))
    })
    private Money retailPrice;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "pickup_from")
    private LocalTime pickupFrom;

    @Column(name = "pickup_to")
    private LocalTime pickupTo;

    @Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "delivery_available", nullable = false)
    @Builder.Default
    private Boolean deliveryAvailable = false;

    @Column(name = "rating")
    private BigDecimal rating;

    @Column(name = "recommended", nullable = false)
    @Builder.Default
    private Boolean recommended = false;

    @Column(name = "category")
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
