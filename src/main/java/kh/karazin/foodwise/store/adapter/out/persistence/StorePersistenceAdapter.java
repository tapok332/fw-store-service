package kh.karazin.foodwise.store.adapter.out.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import kh.karazin.foodwise.store.application.port.in.StoreSearchParams;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.config.StoreProperties;
import kh.karazin.foodwise.store.domain.Store;
import kh.karazin.foodwise.store.domain.StoreId;
import kh.karazin.foodwise.store.domain.StoreType;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Persistence adapter implementing the {@link StoreRepository} outbound port.
 * Owns the PostGIS specifics — the geography {@code Point} round-trip and the
 * dynamic {@code ST_Distance} criteria — so they never leak into the domain.
 */
@Component
@RequiredArgsConstructor
class StorePersistenceAdapter implements StoreRepository {

    private final StoreJpaRepository storeJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;
    private final GeometryFactory geometryFactory;
    private final StoreProperties storeProperties;

    @Override
    public Optional<Store> findById(StoreId storeId) {
        return storeJpaRepository.findById(storeId.value()).map(PersistenceMappers::toStore);
    }

    @Override
    public Page<Store> search(StoreSearchParams params) {
        Specification<StoreEntity> spec = buildSpecification(params);
        Sort jpaSort = jpaSortFor(params);
        PageRequest pageable = PageRequest.of(params.page(), params.size(), jpaSort);
        return storeJpaRepository.findAll(spec, pageable).map(PersistenceMappers::toStore);
    }

    @Override
    public List<Store> findFeatured() {
        return storeJpaRepository.findAll(PageRequest.of(0, 20))
                .map(PersistenceMappers::toStore)
                .getContent();
    }

    @Override
    public List<Store> findNearby(double lat, double lon, double radius) {
        return storeJpaRepository.findNearbyStores(lat, lon, radius).stream()
                .map(PersistenceMappers::toStore)
                .toList();
    }

    @Override
    public Optional<Double> findDistanceMeters(StoreId storeId, double lat, double lon) {
        return storeJpaRepository.findDistanceMeters(storeId.value(), lat, lon);
    }

    @Override
    public boolean existsById(StoreId storeId) {
        return storeJpaRepository.existsById(storeId.value());
    }

    @Override
    public Store save(Store store) {
        StoreEntity entity = store.id() == null ? newEntity(store) : reconcile(store);
        return PersistenceMappers.toStore(storeJpaRepository.save(entity));
    }

    private StoreEntity newEntity(Store store) {
        return StoreEntity.builder()
                .name(store.name())
                .description(store.description())
                .imageUrl(store.imageUrl())
                .heroImageUrl(store.heroImageUrl())
                .type(store.type())
                .category(store.category() != null
                        ? categoryJpaRepository.getReferenceById(store.category().id().value())
                        : null)
                .address(store.address())
                .location(toPoint(store))
                .rating(store.rating())
                .opensAt(store.opensAt())
                .closesAt(store.closesAt())
                .phone(store.phone())
                .website(store.website())
                .deliveryFee(store.deliveryFee())
                .minOrderAmount(store.minOrderAmount())
                .priceLevel(store.priceLevel())
                .build();
    }

    /**
     * The only post-create store mutation in the codebase is a hero-image swap
     * (media upload), so the reconcile path updates that field alone and leaves
     * the rest — including the geography point and timestamps — untouched.
     */
    private StoreEntity reconcile(Store store) {
        StoreEntity entity = storeJpaRepository.findById(store.id().value())
                .orElseThrow(() -> new IllegalStateException(
                        "Store disappeared during transaction: " + store.id().value()));
        entity.setHeroImageUrl(store.heroImageUrl());
        return entity;
    }

    private Point toPoint(Store store) {
        if (store.latitude() == null || store.longitude() == null) {
            return null;
        }
        Point point = geometryFactory.createPoint(
                new Coordinate(store.longitude(), store.latitude()));
        point.setSRID(4326);
        return point;
    }

    // ---------- search building blocks (ported verbatim from the prior service) ----------

    private Sort jpaSortFor(StoreSearchParams params) {
        return switch (params.sort()) {
            // DISTANCE is applied inside the Specification (we need ST_Distance).
            case DISTANCE -> Sort.unsorted();
            case RATING -> Sort.by(Sort.Direction.DESC, "rating");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "priceLevel");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "priceLevel");
            case RELEVANCE -> Sort.unsorted();
        };
    }

    private Specification<StoreEntity> buildSpecification(StoreSearchParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (params.search() != null && !params.search().isBlank()) {
                String pattern = "%" + params.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.<String>get("description")), pattern)
                ));
            }

            if (params.type() != null) {
                predicates.add(cb.equal(root.get("type"), params.type()));
            } else if (params.group() != null) {
                Set<StoreType> typesInGroup = StoreType.typesIn(params.group());
                predicates.add(root.get("type").in(typesInGroup));
            }

            if (params.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), params.categoryId()));
            } else if (params.categorySlug() != null && !params.categorySlug().isBlank()) {
                predicates.add(cb.equal(
                        root.get("category").<String>get("slug"),
                        params.categorySlug().toLowerCase()));
            }

            if (params.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<java.math.BigDecimal>get("rating"), params.minRating()));
            }

            if (!params.priceLevels().isEmpty()) {
                predicates.add(root.get("priceLevel").in(params.priceLevels()));
            }

            if (params.hasLocation()) {
                Expression<Double> distanceMeters = cb.function(
                        "ST_Distance", Double.class,
                        root.get("location"),
                        cb.function("ST_GeographyFromText", Object.class,
                                cb.literal("SRID=4326;POINT("
                                        + params.longitude() + " " + params.latitude() + ")")));

                if (params.maxDistanceKm() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            distanceMeters, params.maxDistanceKm() * 1000.0));
                }

                if (params.sort() == StoreSearchParams.SortField.DISTANCE && query != null) {
                    query.orderBy(cb.asc(distanceMeters));
                }
            }

            if (Boolean.TRUE.equals(params.openNow())) {
                predicates.add(openNowPredicate(root, cb));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate openNowPredicate(Root<StoreEntity> root, CriteriaBuilder cb) {
        LocalTime now = LocalTime.now(storeProperties.timezone());
        Path<LocalTime> opens = root.get("opensAt");
        Path<LocalTime> closes = root.get("closesAt");

        Predicate hasHours = cb.and(cb.isNotNull(opens), cb.isNotNull(closes));

        Predicate sameDay = cb.and(
                cb.lessThanOrEqualTo(opens, closes),
                cb.lessThanOrEqualTo(opens, cb.literal(now)),
                cb.greaterThanOrEqualTo(closes, cb.literal(now)));

        Predicate overnight = cb.and(
                cb.greaterThan(opens, closes),
                cb.or(
                        cb.lessThanOrEqualTo(opens, cb.literal(now)),
                        cb.greaterThanOrEqualTo(closes, cb.literal(now))));

        return cb.and(hasHours, cb.or(sameDay, overnight));
    }
}
