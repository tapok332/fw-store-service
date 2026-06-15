package kh.karazin.foodwise.store.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kh.karazin.foodwise.store.domain.StoreType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cuisine-style or department-style classification for stores.
 *
 * <p>{@code slug} is the URL-friendly stable identifier. Display names come from
 * {@link #translations} resolved by request locale. {@link #applicableTypes}
 * drives the page partition between store groups.
 */
@Entity
@Table(name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_categories_slug", columnNames = "slug")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "slug", nullable = false, length = 64)
    private String slug;

    @Column(name = "icon_name", length = 64)
    private String iconName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "category_translations",
            joinColumns = @JoinColumn(name = "category_id"))
    @MapKeyColumn(name = "locale", length = 16)
    @Column(name = "name", nullable = false, length = 128)
    @Builder.Default
    private Map<String, String> translations = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER, targetClass = StoreType.class)
    @CollectionTable(name = "category_store_types",
            joinColumns = @JoinColumn(name = "category_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "store_type", length = 32, nullable = false)
    @Builder.Default
    private Set<StoreType> applicableTypes = EnumSet.noneOf(StoreType.class);

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
