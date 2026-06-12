package kh.karazin.foodwise.store.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import kh.karazin.foodwise.common.dto.internal.InternalStoreDto;
import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.config.CacheConfig;
import kh.karazin.foodwise.store.config.StoreProperties;
import kh.karazin.foodwise.store.dto.MenuSearchResponse;
import kh.karazin.foodwise.store.dto.StoreCreateRequest;
import kh.karazin.foodwise.store.dto.StoreDto;
import kh.karazin.foodwise.store.dto.StoreMenuItemDto;
import kh.karazin.foodwise.store.dto.StoreSearchParams;
import kh.karazin.foodwise.store.entity.CategoryEntity;
import kh.karazin.foodwise.store.entity.ComboEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.entity.StoreMenuItemEntity;
import kh.karazin.foodwise.store.entity.StoreType;
import kh.karazin.foodwise.store.mapper.StoreMapper;
import kh.karazin.foodwise.store.repository.CategoryRepository;
import kh.karazin.foodwise.store.repository.ComboRepository;
import kh.karazin.foodwise.store.repository.StoreMenuItemRepository;
import kh.karazin.foodwise.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for store operations including search, retrieval, and menu queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreMenuItemRepository menuItemRepository;
    private final ComboRepository comboRepository;
    private final CategoryRepository categoryRepository;
    private final StoreMapper storeMapper;
    private final GeometryFactory geometryFactory;
    private final StoreProperties storeProperties;

    /**
     * Search stores with dynamic filters and sort. Honors the full query-string contract
     * declared in {@link StoreSearchParams}.
     */
    public Page<StoreDto> searchStores(StoreSearchParams params) {
        Specification<StoreEntity> spec = buildSpecification(params);
        Sort jpaSort = jpaSortFor(params);
        PageRequest pageable = PageRequest.of(params.page(), params.size(), jpaSort);
        return storeRepository.findAll(spec, pageable).map(storeMapper::toDto);
    }

    /**
     * Get store by ID.
     */
    public StoreDto getStoreById(UUID storeId) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId));

        List<StoreMenuItemEntity> menuItems = menuItemRepository.findByStoreIdAndAvailableTrue(storeId);
        List<ComboEntity> combos = comboRepository.findByStoreId(storeId);

        return storeMapper.toDtoWithMenu(store, menuItems, combos, null);
    }

    /**
     * Internal-lane lookup for {@code GET /internal/stores/{id}} per ADR 0010.
     *
     * <p>Single {@code findById} query — no menu-items / combos joins. The
     * downstream consumer (order-service) reads {@code name} for order
     * denormalization and the rest of the fields are part of the stable
     * contract for future-proofing.
     */
    public InternalStoreDto getStoreForInternal(UUID storeId) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId));
        return storeMapper.toInternalDto(store);
    }

    /**
     * Get store by ID with distance from a given point.
     */
    public StoreDto getStoreByIdWithDistance(UUID storeId, double lat, double lon) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId));

        List<StoreMenuItemEntity> menuItems = menuItemRepository.findByStoreIdAndAvailableTrue(storeId);
        List<ComboEntity> combos = comboRepository.findByStoreId(storeId);

        Double distance = storeRepository.findDistanceMeters(storeId, lat, lon).orElse(null);
        return storeMapper.toDtoWithMenu(store, menuItems, combos, distance);
    }

    /**
     * Get featured stores for homepage (first page, no filters).
     */
    public List<StoreDto> getFeaturedStores() {
        return storeRepository.findAll(PageRequest.of(0, 20))
                .map(storeMapper::toDto)
                .getContent();
    }

    /**
     * Find nearby stores within a radius (meters).
     */
    public List<StoreDto> findNearbyStores(double lat, double lon, double radius) {
        List<StoreEntity> stores = storeRepository.findNearbyStores(lat, lon, radius);
        return stores.stream().map(storeMapper::toDto).toList();
    }

    /**
     * Search menu items within a store.
     */
    public MenuSearchResponse searchMenu(UUID storeId, String query) {
        if (!storeRepository.existsById(storeId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId);
        }

        List<StoreMenuItemEntity> items = menuItemRepository.searchByStoreIdAndQuery(storeId, query);
        List<StoreMenuItemDto> dtos = items.stream().map(storeMapper::toStoreMenuItemDto).toList();

        return new MenuSearchResponse(query, dtos.size(), dtos);
    }

    /**
     * Create a new store from the given request.
     */
    @Transactional
    @CacheEvict(value = CacheConfig.STORES_CACHE, allEntries = true)
    public StoreDto createStore(StoreCreateRequest request) {
        CategoryEntity category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> FoodWiseException.errorWithDescription(
                            FoodWiseErrorCode.ENTITY_NOT_FOUND,
                            "Category not found: " + request.categoryId()));
        }

        Point location = geometryFactory.createPoint(
                new Coordinate(request.lng().doubleValue(), request.lat().doubleValue()));
        location.setSRID(4326);

        StoreEntity entity = StoreEntity.builder()
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .heroImageUrl(request.heroImageUrl())
                .type(request.type() != null ? request.type() : StoreType.RESTAURANT)
                .category(category)
                .address(request.address())
                .location(location)
                .rating(request.rating())
                .opensAt(request.opensAt())
                .closesAt(request.closesAt())
                .phone(request.phone())
                .website(request.website())
                .deliveryFee(request.deliveryFee())
                .minOrderAmount(request.minOrderAmount())
                .priceLevel(request.priceLevel())
                .build();

        StoreEntity saved = storeRepository.save(entity);
        log.info("Created store {} ({})", saved.getId(), saved.getName());
        return storeMapper.toDto(saved);
    }

    // ---------- private: search building blocks ----------

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

            // free-text search across name + description
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

    /**
     * Build a predicate matching stores whose opens_at..closes_at window contains the
     * current wall-clock time in the configured store timezone. Handles overnight ranges
     * (e.g. 22:00–02:00).
     */
    private Predicate openNowPredicate(jakarta.persistence.criteria.Root<StoreEntity> root,
                                       jakarta.persistence.criteria.CriteriaBuilder cb) {
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
